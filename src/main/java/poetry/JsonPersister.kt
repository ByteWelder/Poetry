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
import poetry.internal.database.enableWriteAheadLoggingSafely
import poetry.internal.database.transactionNonExclusive
import poetry.internal.reflection.ClassAnnotationRetriever
import poetry.internal.reflection.FieldAnnotationRetriever
import poetry.internal.reflection.FieldRetriever
import poetry.internal.reflection.OrmliteReflection
import java.lang.reflect.Field

/**
 * Persist a JSONObject or JSONArray to an SQLite database by parsing annotations (both from OrmLite and custom ones).
 */
class JsonPersister
/**
 * Constructor.
 *
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
				?: throw RuntimeException("DatabaseTable annotation not found for " + modelClass.name)

		val values = ContentValues()
		val jsonKeys = jsonObject.keys()
		val foreignCollectionMappings = ArrayList<ForeignCollectionMapping>()

		val tableName = OrmliteReflection.getTableName(modelClass, tableAnnotation)

		// We want to know the object ID because we need it to resolve one-to-many relationships (foreign collection fields)
		var idFieldName: String? = null
		var objectId: Any? = null

		// Process all JSON keys and map them to the database
		while (jsonKeys.hasNext()) {
			// Get the next key
			val jsonKey = jsonKeys.next() as String

			// Find a Field with the same name as the key
			// TODO: use JsonProperty annotation to get an optional name override

			val field = fieldRetriever.getField(modelClass, jsonKey)

			if (field == null) {
				if (!isOptionEnabled(options, OPTION_DISABLE_IGNORED_ATTRIBUTES_WARNING)) {
					Log.w(javaClass.name, String.format("ignored attribute %s because it wasn't found in %s as a DatabaseField", jsonKey, modelClass.simpleName))
				}

				continue
			}

			val databaseField = fieldAnnotationRetriever.getAnnotation(field, DatabaseField::class.java)

			// DatabaseField is used for: object IDs, simple key-values and one-to-one relationships
			if (databaseField != null) {
				// Object IDs are a special case because we need to insert a new object if the object doesn't exist yet
				// and we also want to retrieve the value to return it in this method and to resolve one-to-many relationships for child objects
				if (OrmliteReflection.isId(databaseField)) {
					objectId = processIdField(databaseField, field, jsonObject, jsonKey, tableName)
					idFieldName = OrmliteReflection.getFieldName(field, databaseField)
				} else
				// object exists, so process its value or reference
				{
					processDatabaseField(databaseField, field, jsonObject, jsonKey, modelClass, values)
				}
			} else { // check if we have a ForeignCollectionField (which is used for one-to-many relationships)
				val foreignCollectionField = fieldAnnotationRetriever.getAnnotation(field, ForeignCollectionField::class.java)

				if (foreignCollectionField != null) {
					val jsonArray = if (!jsonObject.isNull(jsonKey)) jsonObject.getJSONArray(jsonKey) else null

					val foreignCollectionMapping = ForeignCollectionMapping(field, jsonArray)
					foreignCollectionMappings.add(foreignCollectionMapping)
				}
			}
		}

		// Determine the object ID
		if (objectId == null || idFieldName == null) {
			val idField = OrmliteReflection.findIdField(fieldAnnotationRetriever, modelClass)
					?: throw SQLiteException("class ${modelClass.name} doesn't have a DatabaseField that is marked as being an ID")

			val idDatabaseField = fieldAnnotationRetriever.getAnnotation(idField, DatabaseField::class.java)

			// we don't have to check for id_database_field being null because OrmliteReflection.findIdField implied it is there
			val idDatabaseFieldSafe = checkNotNull(idDatabaseField) { "unexpected null in id_database_field" }
			idFieldName = OrmliteReflection.getFieldName(idField, idDatabaseFieldSafe)

			val insertedId = database.insert("'$tableName'", idFieldName, ContentValues())

			if (insertedId == -1L) {
				throw SQLiteException("failed to insert " + modelClass.name + " with id field " + idFieldName)
			}

			objectId = insertedId
		}

		// Process regular fields
		if (values.size() > 0) {
			database.update("'$tableName'", values, "$idFieldName = ?", arrayOf(objectId.toString()))
		}

		Log.i(javaClass.name, String.format("imported %s (%s=%s)", modelClass.simpleName, idFieldName, objectId.toString()))

		// Process foreign collection fields for inserted object
		for (foreignCollectionMapping in foreignCollectionMappings) {
			val manyToManyField = fieldAnnotationRetriever.getAnnotation(foreignCollectionMapping.field, ManyToManyField::class.java)

			if (manyToManyField != null) {
				processManyToMany(manyToManyField, foreignCollectionMapping, objectId, modelClass)
			} else {
				processManyToOne(foreignCollectionMapping, objectId, modelClass)
			}
		}

		return objectId as IdType
	}

	@Throws(JSONException::class)
	private fun <IdType> persistArrayOfObjects(modelClass: Class<*>, jsonArray: JSONArray): List<IdType> {
		val results = ArrayList<IdType>(jsonArray.length())

		for (i in 0 until jsonArray.length()) {
			val jsonObject = jsonArray.getJSONObject(i)
			val objectId = persistObjectInternal<IdType>(modelClass, jsonObject)

			results.add(objectId)
		}

		return results
	}

	@Throws(JSONException::class)
	private fun persistArrayOfBaseTypes(modelClass: Class<*>, jsonArray: JSONArray, singleTargetField: ForeignCollectionFieldSingleTarget): List<Any> {
		val tableAnnotation = modelClass.getAnnotation(DatabaseTable::class.java)
				?: throw RuntimeException("DatabaseTable annotation not found for " + modelClass.name)

		val tableName = OrmliteReflection.getTableName(modelClass, tableAnnotation)

		val results = ArrayList<Any>(jsonArray.length())

		for (i in 0 until jsonArray.length()) {
			val valueObject = jsonArray.get(i)

			val contentValues = ContentValues()
			contentValues.put(singleTargetField.targetField, valueObject.toString())

			val insertedId = database.insert("'$tableName'", singleTargetField.targetField, contentValues)

			if (insertedId == -1L) {
				throw SQLiteException("failed to insert " + modelClass.name)
			}

			results.add(insertedId)
		}

		return results
	}

	/**
	 * Process an ID field giving JSON input and serialization information.
	 * If no object is found in the database, a new one is inserted and its ID is returned.
	 *
	 * @param databaseField the Ormlite annotation
	 * @param field         the field that is annotated by databaseField
	 * @param jsonObject    the object that is being mapped
	 * @param jsonKey       the key where the value of the id field can be found within the jsonObject
	 * @param tableName     the table to insert a new row in case the ID is not found in the database
	 * @return the ID field value of this object (never null)
	 * @throws JSONException when the ID field value cannot be determined
	 */
	@Throws(JSONException::class)
	private fun processIdField(databaseField: DatabaseField, field: Field, jsonObject: JSONObject, jsonKey: String, tableName: String): Any {
		val dbFieldName = OrmliteReflection.getFieldName(field, databaseField)

		val objectId = JsonUtils.getValue(jsonObject, jsonKey, field.type)
				?: throw RuntimeException(String.format("failed to get a value from JSON with key %s and type %s", jsonKey, field.type.name))

		val sql = String.format("SELECT * FROM '%s' WHERE %s = ? LIMIT 1", tableName, dbFieldName)
		val selectionArgs = arrayOf(objectId.toString())
		val cursor = database.rawQuery(sql, selectionArgs)
		val objectExists = cursor.count > 0
		cursor.close()

		if (objectExists) {
			// return existing object id
			return objectId
		} else { // create object
			val values = ContentValues(1)

			if (!JsonUtils.copyValue(objectId, dbFieldName, values)) {
				throw JSONException(String.format("failed to process id field %s for table %s and jsonKey %s", field.name, tableName, jsonKey))
			}

			val insertedId = database.insert("'$tableName'", null, values)

			if (insertedId == -1L) {
				throw SQLiteException(String.format("failed to insert %s with id %s=%s", field.type.name, dbFieldName, objectId.toString()))
			}

			Log.i(javaClass.name, String.format("prepared %s row (id=%s/%s)", tableName, objectId.toString(), java.lang.Long.toString(insertedId)))

			return objectId // don't return inserted_id, because it's always long (while the target type might be int or another type)
		}
	}

	@Throws(JSONException::class)
	private fun processDatabaseField(databaseField: DatabaseField, field: Field, jsonParentObject: JSONObject, jsonKey: String, modelClass: Class<*>, values: ContentValues) {
		val dbFieldName = OrmliteReflection.getFieldName(field, databaseField)

		if (jsonParentObject.isNull(jsonKey)) {
			values.putNull(dbFieldName)
		} else if (OrmliteReflection.isForeign(databaseField)) {
			val foreignObject = jsonParentObject.optJSONObject(jsonKey)

			if (foreignObject != null) {
				//If the JSON includes the forein object, try to persist it

				val foreignObjectId = persistObjectInternal<Any>(field.type, foreignObject)

				if (!JsonUtils.copyValue(foreignObjectId, dbFieldName, values)) {
					throw RuntimeException("failed to copy values for key " + jsonKey + " in " + modelClass.name + ": key type " + foreignObjectId.javaClass + " is not supported")
				}
			} else {
				//The JSON does not include the foreign object, see if it is a valid key for the foreign object

				val foreignObjectIdField = OrmliteReflection.findIdField(fieldAnnotationRetriever, field.type)
						?: throw RuntimeException("failed to find id field for foreign object " + field.type.name + " in " + modelClass.name)

				val foreignObjectId = JsonUtils.getValue(jsonParentObject, jsonKey, foreignObjectIdField.type)
						?: throw RuntimeException("incompatible id type for foreign object " + field.type.name + " in " + modelClass.name + " (expected " + foreignObjectIdField.type.name + ")")

				if (!JsonUtils.copyValue(foreignObjectId, dbFieldName, values)) {
					throw RuntimeException("failed to copy values for key " + jsonKey + " in " + modelClass.name + ": key type " + foreignObjectId.javaClass + " is not supported")
				}
			}
		} else { // non-foreign
			if (!JsonUtils.copyContentValue(jsonParentObject, jsonKey, values, dbFieldName)) {
				Log.w(javaClass.name, String.format("attribute type %s has an unsupported type while parsing %s", jsonKey, modelClass.simpleName))
			}
		}
	}

	@Throws(JSONException::class)
	private fun processManyToMany(manyToManyField: ManyToManyField, foreignCollectionMapping: ForeignCollectionMapping, parentId: Any, parentClass: Class<*>) {
		if (foreignCollectionMapping.jsonArray == null) {
			// TODO: Delete mapping
			Log.w(javaClass.name, String.format("Mapping %s for type %s was null. Ignored it, but it should be deleted!", foreignCollectionMapping.field.name, foreignCollectionMapping.field.type.name))
			return
		}

		val foreignCollectionField = foreignCollectionMapping.field

		val targetClass = OrmliteReflection.getForeignCollectionParameterType(foreignCollectionField)
		val targetIdField = OrmliteReflection.findIdField(fieldAnnotationRetriever, targetClass)
				?: throw RuntimeException("no id field found while processing foreign collection relation for " + targetClass.name)

		val targetForeignField = OrmliteReflection.findForeignField(fieldAnnotationRetriever, targetClass, parentClass)
				?: throw RuntimeException("no foreign field found while processing foreign collection relation for " + targetClass.name)

		val targetTargetField = fieldRetriever.getFirstFieldOfType(targetClass, manyToManyField.targetType.javaObjectType)
				?: throw RuntimeException("ManyToMany problem: no ID field found for type " + manyToManyField.targetType.javaObjectType.name)

		val targetTargetIds = persistArrayOfObjects<Any>(targetTargetField.type, foreignCollectionMapping.jsonArray)

		// TODO: cache table name
		val targetTableName = OrmliteReflection.getTableName(classAnnotationRetriever, targetClass)
		val targetForeignDbField = fieldAnnotationRetriever.getAnnotation(targetForeignField, DatabaseField::class.java)
		val targetForeignDbFieldSafe = checkNotNull(targetForeignDbField)
		val targetForeignFieldName = OrmliteReflection.getFieldName(targetForeignField, targetForeignDbFieldSafe)
		val targetForeignFieldValue = QueryUtils.parseAttribute(parentId)
		val deleteSelectClause = "$targetForeignFieldName = $targetForeignFieldValue"
		database.delete("'$targetTableName'", deleteSelectClause, arrayOf())

		val targetTargetDatabaseField = fieldAnnotationRetriever.getAnnotation(targetTargetField, DatabaseField::class.java)
		val targetTargetDatabaseFieldSafe = checkNotNull(targetTargetDatabaseField)
		val targetTargetFieldName = OrmliteReflection.getFieldName(targetTargetField, targetTargetDatabaseFieldSafe)

		// Insert new references
		targetTargetIds.indices.forEach { index ->
			val values = ContentValues(2)

			if (!JsonUtils.copyValue(parentId, targetForeignFieldName, values)) {
				throw RuntimeException("parent id copy failed")
			}

			if (!JsonUtils.copyValue(targetTargetIds[index], targetTargetFieldName, values)) {
				throw RuntimeException("target id copy failed")
			}

			if (database.insert("'$targetTableName'", null, values) == -1L) {
				throw RuntimeException("failed to insert item in $targetTableName")
			}
		}
	}

	@Throws(JSONException::class)
	private fun processManyToOne(foreignCollectionMapping: ForeignCollectionMapping, parentId: Any, parentClass: Class<*>) {
		if (foreignCollectionMapping.jsonArray == null) {
			// TODO: Delete mapping
			Log.w(javaClass.name, String.format("Mapping %s for type %s was null. Ignored it, but it should be deleted!", foreignCollectionMapping.field.name, foreignCollectionMapping.field.type.name))
			return
		}

		val foreignCollectionField = foreignCollectionMapping.field

		val targetClass = OrmliteReflection.getForeignCollectionParameterType(foreignCollectionField)
		val targetIdField = OrmliteReflection.findIdField(fieldAnnotationRetriever, targetClass)
				?: throw RuntimeException("no id field found while processing foreign collection relation for " + targetClass.name)

		val targetForeignField = OrmliteReflection.findForeignField(fieldAnnotationRetriever, targetClass, parentClass)
				?: throw RuntimeException("no foreign field found while processing foreign collection relation for " + targetClass.name)

		val singleTargetField = fieldAnnotationRetriever.getAnnotation(foreignCollectionMapping.field, ForeignCollectionFieldSingleTarget::class.java)

		val targetIds: List<Any> = if (singleTargetField == null) {
			persistArrayOfObjects(targetClass, foreignCollectionMapping.jsonArray)
		} else {
			persistArrayOfBaseTypes(targetClass, foreignCollectionMapping.jsonArray, singleTargetField)
		}

		val targetForeignFieldDbAnnotation = fieldAnnotationRetriever.getAnnotation(targetForeignField, DatabaseField::class.java)
		val targetForeignFieldDbAnnotationSafe = checkNotNull(targetForeignFieldDbAnnotation) { "DatabaseField annotation not found" }
		val targetForeignFieldName = OrmliteReflection.getFieldName(targetForeignField, targetForeignFieldDbAnnotationSafe)

		val values = ContentValues(1)

		if (!JsonUtils.copyValue(parentId, targetForeignFieldName, values)) {
			throw RuntimeException("failed to copy foreign key " + targetForeignFieldName + " in " + parentClass.name + ": key type " + parentId.javaClass + " is not supported")
		}

		val targetIdArgs = arrayOfNulls<String>(targetIds.size)
		val inClause = QueryUtils.createInClause(targetIds, targetIdArgs)

		// update references to all target objects
		val targetTableName = OrmliteReflection.getTableName(classAnnotationRetriever, targetClass)
		val targetIdFieldName = OrmliteReflection.getFieldName(fieldAnnotationRetriever, targetIdField)

		val updateSelectClause = "$targetIdFieldName $inClause"
		database.update("'$targetTableName'", values, updateSelectClause, targetIdArgs)

		if (!isOptionEnabled(options, OPTION_DISABLE_FOREIGN_COLLECTION_CLEANUP)) {
			// remove all objects that are not referenced to the parent anymore
			val idToCleanUp = QueryUtils.parseAttribute(parentId)
			val deleteSelectClause = "$targetIdFieldName NOT $inClause AND $targetForeignFieldName = $idToCleanUp"
			database.delete("'$targetTableName'", deleteSelectClause, targetIdArgs)
		}
	}

	/**
	 * All necessary data to map an array of objects onto the provided parent field.
	 */
	private class ForeignCollectionMapping
	/**
	 * @param field
	 * @param jsonArray or null
	 */
	internal constructor(
		internal val field: Field,
		internal val jsonArray: JSONArray?
	)

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
