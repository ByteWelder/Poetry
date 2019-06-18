package poetry

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Looper
import android.util.Log
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.table.DatabaseTable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import poetry.annotations.ForeignCollectionFieldSingleTarget
import poetry.annotations.ManyToManyField
import poetry.internal.JsonUtils
import poetry.internal.database.QueryUtils
import poetry.internal.database.createRowIfNotExists
import poetry.internal.database.enableWriteAheadLoggingSafely
import poetry.internal.database.putOrThrow
import poetry.internal.database.transactionNonExclusive
import poetry.internal.getValue
import poetry.internal.reflection.ClassAnnotationRetriever
import poetry.internal.reflection.FieldAnnotationRetriever
import poetry.internal.reflection.FieldRetriever
import poetry.internal.reflection.findForeignField
import poetry.internal.reflection.findIdField
import poetry.internal.reflection.findIdFieldOrThrow
import poetry.internal.reflection.getColumnNameForField
import poetry.internal.reflection.getForeignCollectionParameterType
import poetry.internal.reflection.getTableName
import poetry.internal.reflection.isForeign
import poetry.internal.reflection.isId
import poetry.internal.toIterable
import poetry.internal.toIterableJsonObject
import java.lang.reflect.Field

private const val logTag = "JsonPersister"

private class IdDescriptor(val columnName: String, val id: Any) {
	companion object {
		val None: IdDescriptor = IdDescriptor("", 0)
	}
}

/**
 * All necessary data to map an array of objects onto the provided parent field.
 */
private class ForeignCollectionMapping(val field: Field, val jsonArray: JSONArray)


/**
 * Persist a JSONObject or JSONArray to an SQLite database by parsing annotations (both from OrmLite and custom ones).
 */
class JsonPersister
/**
 * @param database the database used for persistence
 * @param options 0 or a combination of 1 or more options as defined by [JsonPersister].OPTION_*
 */
@JvmOverloads constructor(private val database: SQLiteDatabase, private val options: Int = 0) {
	private val fieldRetriever = FieldRetriever()
	private val fieldAnnotationRetriever = FieldAnnotationRetriever()
	private val classAnnotationRetriever = ClassAnnotationRetriever()

	/**
	 * Recursively persist this object and all its children.
	 *
	 * @param modelClass the type to persist
	 * @param jsonObject the json to process
	 * @param <IdType>   the ID type to return
	 * @return the ID of the persisted object
	 * @throws JSONException when something went wrong through parsing, this also fails the database transaction and results in no data changes
	</IdType> */
	@Throws(JSONException::class)
	fun <IdType> persistObject(modelClass: Class<*>, jsonObject: JSONObject): IdType {
		warnIfOnMainThread("persistObject()")

		database.enableWriteAheadLoggingSafely()

		return database.transactionNonExclusive {
			persistObjectInternal(modelClass, jsonObject)
		}
	}

	/**
	 * Recursively persist the array and all its object's children.
	 *
	 * @param modelClass the type to persist
	 * @param jsonArray  the json to process
	 * @param <IdType>   the ID type to return
	 * @return the list of IDs of the persisted objects
	 * @throws JSONException when something went wrong through parsing, this also fails the database transaction and results in no data changes
	 */
	@Throws(JSONException::class)
	fun <IdType> persistArray(modelClass: Class<*>, jsonArray: JSONArray): List<IdType> {
		warnIfOnMainThread("persistArray()")

		database.enableWriteAheadLoggingSafely()

		return database.transactionNonExclusive {
			persistArrayOfObjects(modelClass, jsonArray)
		}
	}

	/**
	 * Main persistence method for persisting a single object
	 *
	 * @param modelClass the type to persist
	 * @param jsonObject the json data to persist
	 * @param <IdType>   the ID type to return
	 * @return the object ID (never null)
	 * @throws JSONException when json processing fails
	</IdType> */
	@Throws(JSONException::class)
	private fun <IdType> persistObjectInternal(modelClass: Class<*>, jsonObject: JSONObject): IdType {
		val tableAnnotation = modelClass.getAnnotation(DatabaseTable::class.java)
				?: throw RuntimeException("DatabaseTable annotation not found for ${modelClass.name}")

		val values = ContentValues()
		val foreignCollectionMappings = ArrayList<ForeignCollectionMapping>()
		val tableName = getTableName(modelClass, tableAnnotation)

		// We want the id so we can return it,
		// but we also it to resolve one-to-many relationships (foreign collection fields)
		var idDescriptor: IdDescriptor = IdDescriptor.None

		// Process all JSON keys and map them to the database
		jsonObject.keys().asSequence().forEach { jsonKey ->
			// Find a Field with the same name as the key
			// TODO: use JsonProperty annotation to get an optional name override
			val field = fieldRetriever.getField(modelClass, jsonKey)
			if (field != null) {
				val databaseField = fieldAnnotationRetriever.findAnnotation(field, DatabaseField::class.java)
				// DatabaseField is used for: object IDs, simple key-values and one-to-one relationships
				if (databaseField != null) {
					// Object IDs are a special case because we need to insert a new object if the object doesn't exist yet
					// and we also want to retrieve the value to return it in this method and to resolve one-to-many relationships for child objects
					if (databaseField.isId()) {
						if (idDescriptor != IdDescriptor.None) {
							throw JSONException("Trying to set id twice for ${modelClass.name} with id ${idDescriptor.id}")
						}
						idDescriptor = createRowIfNotExists(jsonObject, jsonKey, field, databaseField, tableName)
					} else { // object exists, so process its value or reference
						processDatabaseField(databaseField, field, jsonObject, jsonKey, modelClass, values)
					}
				} else { // check if we have a ForeignCollectionField (which is used for one-to-many relationships)
					val foreignCollectionField = fieldAnnotationRetriever.findAnnotation(field, ForeignCollectionField::class.java)
					if (foreignCollectionField != null) {
						val jsonArray = jsonObject.optJSONArray(jsonKey)
						if (jsonArray == null) {
							Log.w(logTag, "There was a recoverable JSON error: Mapping ${field.name} for type ${field.type.name} was null.")
						} else {
							val foreignCollectionMapping = ForeignCollectionMapping(field, jsonArray)
							foreignCollectionMappings.add(foreignCollectionMapping)
						}
					}
				}
			} else {
				if (!isOptionEnabled(options, OPTION_DISABLE_IGNORED_ATTRIBUTES_WARNING)) {
					Log.w(logTag, "ignored attribute $jsonKey because it wasn't found in ${modelClass.simpleName} as a DatabaseField")
				}
			}
		}

		// None of the JSON values represented an id, so try to find it in the class
		if (idDescriptor == IdDescriptor.None) {
			idDescriptor = insertRowFromModelClass(tableName, modelClass)
		}

		// Update id field
		// TODO: Is this really necessary? We should already have this in the database by now?!
		if (values.size() > 0) {
			database.update("'$tableName'", values, "${idDescriptor.columnName} = ?", arrayOf(idDescriptor.id.toString()))
		}

		Log.i(logTag, "imported ${modelClass.simpleName} (${idDescriptor.columnName}=${idDescriptor.id})")

		// Process foreign collection fields for inserted object
		foreignCollectionMappings.forEach { mapping ->
			val manyToManyField = fieldAnnotationRetriever.findAnnotation(mapping.field, ManyToManyField::class.java)
			if (manyToManyField != null) {
				processManyToMany(manyToManyField, mapping, idDescriptor.id, modelClass)
			} else {
				processManyToOne(mapping, idDescriptor.id, modelClass)
			}
		}

		return idDescriptor.id as IdType
	}

	@Throws(JSONException::class)
	private fun <IdType> persistArrayOfObjects(modelClass: Class<*>, jsonArray: JSONArray): List<IdType> {
		return jsonArray.toIterableJsonObject()
				.map { persistObjectInternal<IdType>(modelClass, it) }
				.toList()
	}

	/**
	 * It's important that the JSONArray that is passed, only contains base types.
	 * All its contents will be grabbed and converted with toString().
	 */
	@Throws(JSONException::class)
	private fun persistArrayOfBaseTypes(modelClass: Class<*>, jsonArray: JSONArray, singleTargetField: ForeignCollectionFieldSingleTarget): List<Any> {
		val tableAnnotation = modelClass.getAnnotation(DatabaseTable::class.java)
				?: throw RuntimeException("DatabaseTable annotation not found for ${modelClass.name}")
		val tableName = getTableName(modelClass, tableAnnotation)

		return jsonArray.toIterable()
				.map { it.toString() }
				.map { valueString ->
					ContentValues().apply {
						put(singleTargetField.targetField, valueString)
					}
				}
				.map { contentValues ->
					database.insertOrThrow("'$tableName'", singleTargetField.targetField, contentValues)
				}
				.toList()
	}

	/**
	 * Based on the JSON input and RTTI, create a database row if it doesn't exist yet.
	 */
	@Throws(JSONException::class)
	private fun createRowIfNotExists(jsonObject: JSONObject, jsonKey: String, field: Field, databaseField: DatabaseField, tableName: String): IdDescriptor {
		val id = jsonObject.getValue(jsonKey, field.type)
		val idColumnName = getColumnNameForField(field, databaseField)
		// TODO: make test with String id
		database.createRowIfNotExists(tableName, idColumnName, id)
		return IdDescriptor(idColumnName, id)
	}

	@Throws(JSONException::class)
	private fun processDatabaseField(databaseField: DatabaseField, field: Field, jsonParentObject: JSONObject, jsonKey: String, modelClass: Class<*>, values: ContentValues) {
		val dbFieldName = getColumnNameForField(field, databaseField)

		if (jsonParentObject.isNull(jsonKey)) {
			values.putNull(dbFieldName)
		} else if (databaseField.isForeign()) {
			val foreignObject = jsonParentObject.optJSONObject(jsonKey)
			if (foreignObject != null) {
				//If the JSON includes the foreign object, try to persist it
				val foreignObjectId = persistObjectInternal<Any>(field.type, foreignObject)
				values.putOrThrow(dbFieldName, foreignObjectId)
			} else {
				//The JSON does not include the foreign object, see if it is a valid key for the foreign object
				val foreignObjectIdField = fieldAnnotationRetriever.findIdFieldOrThrow(field.type)
				val foreignObjectId = jsonParentObject.getValue(jsonKey, foreignObjectIdField.type)
				values.putOrThrow(dbFieldName, foreignObjectId)
			}
		} else { // non-foreign
			if (!JsonUtils.copyContentValue(jsonParentObject, jsonKey, values, dbFieldName)) {
				Log.w(logTag, "attribute type $jsonKey has an unsupported type while parsing ${modelClass.simpleName}")
			}
		}
	}

	@Throws(JSONException::class)
	private fun processManyToMany(manyToManyField: ManyToManyField, foreignCollectionMapping: ForeignCollectionMapping, parentId: Any, parentClass: Class<*>) {
		val foreignCollectionField = foreignCollectionMapping.field
		val targetClass = foreignCollectionField.getForeignCollectionParameterType()
		val targetForeignField = fieldAnnotationRetriever.findForeignField(targetClass, parentClass)
				?: throw RuntimeException("no foreign field found while processing foreign collection relation for ${targetClass.name}")
		val targetTargetField = fieldRetriever.getFirstFieldOfType(targetClass, manyToManyField.targetType.javaObjectType)
				?: throw RuntimeException("ManyToMany problem: no ID field found for type ${manyToManyField.targetType.javaObjectType.name}")
		val targetTargetIds = persistArrayOfObjects<Any>(targetTargetField.type, foreignCollectionMapping.jsonArray)
		val targetTableName = classAnnotationRetriever.getTableName(targetClass)
		val targetForeignDbField = fieldAnnotationRetriever.findAnnotation(targetForeignField, DatabaseField::class.java)
		val targetForeignDbFieldSafe = checkNotNull(targetForeignDbField)
		val targetForeignFieldName = getColumnNameForField(targetForeignField, targetForeignDbFieldSafe)
		val targetForeignFieldValue = QueryUtils.parseAttribute(parentId)

		val deleteSelectClause = "$targetForeignFieldName = $targetForeignFieldValue"
		database.delete("'$targetTableName'", deleteSelectClause, emptyArray())

		val targetTargetDatabaseField = fieldAnnotationRetriever.findAnnotation(targetTargetField, DatabaseField::class.java)
		val targetTargetDatabaseFieldSafe = checkNotNull(targetTargetDatabaseField)
		val targetTargetFieldName = getColumnNameForField(targetTargetField, targetTargetDatabaseFieldSafe)

		// Insert new references
		targetTargetIds.indices.forEach { index ->
			val values = ContentValues(2).apply {
				putOrThrow(targetForeignFieldName, parentId)
				putOrThrow(targetTargetFieldName, targetTargetIds[index])
			}
			database.insertOrThrow("'$targetTableName'", null, values)
		}
	}

	@Throws(JSONException::class)
	private fun processManyToOne(foreignCollectionMapping: ForeignCollectionMapping, parentId: Any, parentClass: Class<*>) {
		val foreignCollectionField = foreignCollectionMapping.field
		val targetClass = foreignCollectionField.getForeignCollectionParameterType()
		val targetIdField = fieldAnnotationRetriever.findIdField(targetClass)
				?: throw RuntimeException("no id field found while processing foreign collection relation for ${targetClass.name}")
		val targetForeignField = fieldAnnotationRetriever.findForeignField(targetClass, parentClass)
				?: throw RuntimeException("no foreign field found while processing foreign collection relation for ${targetClass.name}")
		val singleTargetField = fieldAnnotationRetriever.findAnnotation(foreignCollectionMapping.field, ForeignCollectionFieldSingleTarget::class.java)

		val targetIds: List<Any> = if (singleTargetField == null) {
			persistArrayOfObjects(targetClass, foreignCollectionMapping.jsonArray)
		} else {
			persistArrayOfBaseTypes(targetClass, foreignCollectionMapping.jsonArray, singleTargetField)
		}

		val targetForeignFieldDbAnnotation = fieldAnnotationRetriever.findAnnotation(targetForeignField, DatabaseField::class.java)
		val targetForeignFieldDbAnnotationSafe = checkNotNull(targetForeignFieldDbAnnotation) { "DatabaseField annotation not found" }
		val targetForeignFieldName = getColumnNameForField(targetForeignField, targetForeignFieldDbAnnotationSafe)

		val values = ContentValues(1).apply {
			putOrThrow(targetForeignFieldName, parentId)
		}

		val targetIdArgs = arrayOfNulls<String>(targetIds.size)
		val inClause = QueryUtils.createInClause(targetIds, targetIdArgs)

		// update references to all target objects
		val targetTableName = classAnnotationRetriever.getTableName(targetClass)
		val targetIdFieldName = fieldAnnotationRetriever.getColumnNameForField(targetIdField)

		val updateSelectClause = "$targetIdFieldName $inClause"
		database.update("'$targetTableName'", values, updateSelectClause, targetIdArgs)

		if (!isOptionEnabled(options, OPTION_DISABLE_FOREIGN_COLLECTION_CLEANUP)) {
			// remove all objects that are not referenced to the parent anymore
			val idToCleanUp = QueryUtils.parseAttribute(parentId)
			val deleteSelectClause = "$targetIdFieldName NOT $inClause AND $targetForeignFieldName = $idToCleanUp"
			database.delete("'$targetTableName'", deleteSelectClause, targetIdArgs)
		}
	}

	private fun insertRowFromModelClass(tableName: String, modelClass: Class<*>): IdDescriptor {
		val idField = fieldAnnotationRetriever.findIdField(modelClass)
				?: throw SQLiteException("class ${modelClass.name} doesn't have a DatabaseField that is marked as being an ID")
		val idDatabaseField = fieldAnnotationRetriever.findAnnotation(idField, DatabaseField::class.java)
		// we don't have to check for id_database_field being null because OrmliteReflection.findIdField implied it is there
		val idDatabaseFieldSafe = checkNotNull(idDatabaseField) { "unexpected null in id_database_field" }
		val idColumnName = getColumnNameForField(idField, idDatabaseFieldSafe)
		val insertedId = database.insertOrThrow("'$tableName'", idColumnName, ContentValues())
		return IdDescriptor(idColumnName, insertedId)
	}

	companion object {
		/**
		 * When a foreign collection is imported (one-to-many relationship),
		 * the normal behavior is that the old children are deleted.
		 * This options allows you to disable that behavior.
		 */
		const val OPTION_DISABLE_FOREIGN_COLLECTION_CLEANUP = 0x0001
		/**
		 * Don't display warnings when JSON attributes are not annotated as a field in an object.
		 */
		const val OPTION_DISABLE_IGNORED_ATTRIBUTES_WARNING = 0x0002

		/**
		 * Check if an option is enabled
		 *
		 * @param optionsSet  the compound of option values (combined with logical OR operator)
		 * @param optionCheck one or more options to check (combined with logical OR operator)
		 * @return true when all the options from optionCheck are contained in optionsSet
		 */
		private fun isOptionEnabled(optionsSet: Int, optionCheck: Int): Boolean = optionsSet and optionCheck == optionCheck
	}
}

private fun warnIfOnMainThread(methodName: String) {
	if (Looper.myLooper() == Looper.getMainLooper()) {
		Log.w(JsonPersister::class.java.name, "Don't call $methodName from the main thread")
	}
}
