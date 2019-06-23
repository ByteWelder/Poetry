package poetry

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import poetry.annotations.ForeignCollectionFieldSingleTarget
import poetry.internal.Entry
import poetry.internal.JsonKeyAndField
import poetry.internal.database.Database
import poetry.internal.database.QueryUtils
import poetry.internal.database.inClauseOf
import poetry.internal.database.native.NativeDatabase
import poetry.internal.database.putOrThrow
import poetry.internal.database.transaction
import poetry.internal.getValue
import poetry.internal.reflection.ClassAnnotationRetriever
import poetry.internal.reflection.FieldAnnotationRetriever
import poetry.internal.reflection.FieldRetriever
import poetry.internal.reflection.findForeignFieldOrThrow
import poetry.internal.reflection.findIdFieldOrThrow
import poetry.internal.reflection.getColumnNameForField
import poetry.internal.reflection.getForeignCollectionParameterType
import poetry.internal.reflection.getTableName
import poetry.internal.reflection.getTableNameOrThrow
import poetry.internal.reflection.isForeign
import poetry.internal.reflection.isId
import poetry.internal.toEntry
import poetry.internal.toIterable
import poetry.internal.toIterableJsonObject
import poetry.internal.warnIfOnMainThread
import kotlin.reflect.KClass

private const val logTag = "Poetry"

/**
 * Persist a JSONObject or JSONArray to an SQLite database by parsing annotations (both from OrmLite and custom ones).
 */
class Poetry
/**
 * @param database the database used for persistence
 * @param options 0 or a combination of 1 or more options as defined by [Poetry].OPTION_*
 */
private constructor(
	private val database: Database,
	private val options: Int = PoetryOptions.DEFAULT
) {
	private val fieldRetriever = FieldRetriever()
	private val fieldAnnotationRetriever = FieldAnnotationRetriever()
	private val classAnnotationRetriever = ClassAnnotationRetriever()

	@JvmOverloads constructor(database: SQLiteDatabase, options: Int = PoetryOptions.DEFAULT)
			: this(NativeDatabase(database), options)

	/**
	 * Store an object to the database.
	 *
	 * @param modelClass the type to persist
	 * @param jsonObject the json to process
	 * @param <IdType>   the ID type to return
	 * @return the ID of the persisted object
	 * @throws JSONException when something went wrong through parsing, this also fails the database transaction and results in no data changes
	</IdType> */
	@Throws(JSONException::class)
	fun writeObject(modelClass: Class<*>, jsonObject: JSONObject): Long {
		warnIfOnMainThread("Poetry.writeObject()")
		return database.transaction {
			writeObjectInternal(modelClass, jsonObject)
		}
	}

	@Throws(JSONException::class)
	fun writeObject(modelClass: KClass<*>, jsonObject: JSONObject) = writeObject(modelClass.java, jsonObject)

	/**
	 * Store an array of objects to the database.
	 *
	 * @param modelClass the type to persist
	 * @param jsonArray  the json to process
	 * @return the list of id values of the persisted objects
	 * @throws JSONException when something went wrong through parsing (this cancels the db transaction)
	 */
	@Throws(JSONException::class)
	fun writeArray(modelClass: Class<*>, jsonArray: JSONArray): List<Long> {
		warnIfOnMainThread("Poetry.writeArray()")
		return database.transaction {
			writeArrayOfObjects(modelClass, jsonArray)
		}
	}

	/**
	 * Store an array of objects to the database.
	 *
	 * @param modelClass the type to persist
	 * @param jsonArray  the json to process
	 * @return the list of id values of the persisted objects
	 * @throws JSONException when something went wrong through parsing (this cancels the db transaction)
	 */
	@Throws(JSONException::class)
	fun writeArray(modelClass: KClass<*>, jsonArray: JSONArray) = writeArray(modelClass.java, jsonArray)

	@Throws(JSONException::class)
	private fun writeObjectInternal(modelClass: Class<*>, jsonObject: JSONObject): Long {
		val tableAnnotation = classAnnotationRetriever.findAnnotationOrThrow(modelClass, DatabaseTable::class.java)
		val tableName = getTableName(modelClass, tableAnnotation)

		// Process all JSON keys and map them to the database
		val entries = jsonObject.keys().asSequence()
				.mapNotNull { jsonKeyAndFieldOf(it, modelClass) }
				.map { it.toEntry(jsonObject, fieldAnnotationRetriever) }
				.toList() // TODO: remove in future

		val contentValues = entries.mapNotNull { it as? Entry.FieldEntry }
				.fold(ContentValues(entries.size)) { contentValues, entry ->
					// Object IDs are a special case because we need to insert a new object if the object doesn't exist yet
					// and we also want to retrieve the value to return it in this method and to resolve one-to-many relationships for child objects
					val dbFieldName = getColumnNameForField(entry.field, entry.databaseField)
					val dbFieldValue = processDatabaseField(entry, jsonObject)
					contentValues.putOrThrow(dbFieldName, dbFieldValue)
					contentValues
				}

		// TODO: search for id in previous stream
		val id = entries.mapNotNull { it as? Entry.FieldEntry }
				.filter{ it.databaseField.isId() }
				.map { entry ->
					val idColumnName = getColumnNameForField(entry.field, entry.databaseField)
					val idValue = jsonObject.getValue(entry.jsonKey, entry.field.type)
					database.queryFirst(tableName, idColumnName, idValue.toString())
				}
				.firstOrNull()

		val resultingId = if (id != null) {
			database.update(tableName, contentValues, "ROWID = ?", arrayOf(id.toString()))
			id
		} else {
			database.insertOrThrow(tableName, contentValues)
		}

		Log.i(logTag, "Imported ${modelClass.simpleName} with id $resultingId")

		entries.forEach {
			when (it) {
				is Entry.ManyToManyEntry -> processManyToMany(it, resultingId)
				is Entry.ManyToOneEntry -> processManyToOne(it, resultingId)
			}
		}

		return resultingId
	}

	@Throws(JSONException::class)
	private fun writeArrayOfObjects(modelClass: Class<*>, jsonArray: JSONArray): List<Long> {
		return jsonArray.toIterableJsonObject()
				.map { writeObjectInternal(modelClass, it) }
				.toList()
	}

	@Throws(JSONException::class)
	private fun writeArrayOfBaseTypes(modelClass: Class<*>, jsonArray: JSONArray, singleTargetField: ForeignCollectionFieldSingleTarget): List<Any> {
		val tableAnnotation = classAnnotationRetriever.findAnnotationOrThrow(modelClass, DatabaseTable::class.java)
		val tableName = getTableName(modelClass, tableAnnotation)
		return jsonArray.toIterable()
				.map { it.toString() }
				.map { dbColumnValue -> ContentValues().apply { put(singleTargetField.targetField, dbColumnValue) } }
				.map { contentValues -> database.insertOrThrow(tableName, contentValues, singleTargetField.targetField) }
				.toList()
	}

	@Throws(JSONException::class)
	private fun processDatabaseField(entry: Entry.FieldEntry, jsonParentObject: JSONObject): Any? {
		return if (jsonParentObject.isNull(entry.jsonKey)) {
			null
		} else if (entry.databaseField.isForeign()) {
			val foreignObject = jsonParentObject.optJSONObject(entry.jsonKey)
			if (foreignObject != null) {
				// If the JSON includes the foreign object, try to persist it
				writeObjectInternal(entry.field.type, foreignObject)
			} else {
				// The JSON does not include the foreign object, see if it is a valid key for the foreign object
				val foreignObjectIdField = fieldAnnotationRetriever.findIdFieldOrThrow(entry.field.type)
				jsonParentObject.getValue(entry.jsonKey, foreignObjectIdField.type)
			}
		} else { // non-null, non-foreign, so assume regular value
			jsonParentObject.get(entry.jsonKey)
		}
	}

	@Throws(JSONException::class)
	private fun processManyToMany(entry: Entry.ManyToManyEntry, parentId: Any) {
		val foreignCollectionField = entry.field
		val targetClass = foreignCollectionField.getForeignCollectionParameterType()
		val targetForeignField = fieldAnnotationRetriever.findForeignFieldOrThrow(targetClass, entry.modelClass)
		val targetTargetField = fieldRetriever.findFirstFieldOfTypeOrThrow(targetClass, entry.manyToManyField.targetType.javaObjectType)
		val targetTargetIds = writeArrayOfObjects(targetTargetField.type, entry.jsonArray)
		val targetTableName = classAnnotationRetriever.getTableNameOrThrow(targetClass)
		val targetForeignDbField = fieldAnnotationRetriever.findAnnotation(targetForeignField, DatabaseField::class.java)
		val targetForeignDbFieldSafe = checkNotNull(targetForeignDbField)
		val targetForeignFieldName = getColumnNameForField(targetForeignField, targetForeignDbFieldSafe)
		val targetForeignFieldValue = QueryUtils.parseAttribute(parentId)

		database.delete(targetTableName, "$targetForeignFieldName = $targetForeignFieldValue", emptyArray())

		val targetTargetDatabaseField = fieldAnnotationRetriever.findAnnotation(targetTargetField, DatabaseField::class.java)
		val targetTargetDatabaseFieldSafe = checkNotNull(targetTargetDatabaseField)
		val targetTargetFieldName = getColumnNameForField(targetTargetField, targetTargetDatabaseFieldSafe)

		// Insert new references
		targetTargetIds.indices.forEach { index ->
			val values = ContentValues(2).apply {
				putOrThrow(targetForeignFieldName, parentId)
				putOrThrow(targetTargetFieldName, targetTargetIds[index])
			}
			database.insertOrThrow(targetTableName, values)
		}
	}

	@Throws(JSONException::class)
	private fun processManyToOne(entry: Entry.ManyToOneEntry, parentId: Any) {
		val foreignCollectionField = entry.field
		val targetClass = foreignCollectionField.getForeignCollectionParameterType()
		val targetIdField = fieldAnnotationRetriever.findIdFieldOrThrow(targetClass)
		val targetForeignField = fieldAnnotationRetriever.findForeignFieldOrThrow(targetClass, entry.modelClass)
		val singleTargetField = fieldAnnotationRetriever.findAnnotation(entry.field, ForeignCollectionFieldSingleTarget::class.java)

		val targetIds: List<Any> = if (singleTargetField == null) {
			writeArrayOfObjects(targetClass, entry.jsonArray)
		} else {
			writeArrayOfBaseTypes(targetClass, entry.jsonArray, singleTargetField)
		}

		val targetForeignFieldDbAnnotation = fieldAnnotationRetriever.findAnnotationOrThrow(targetForeignField, DatabaseField::class.java)
		val targetForeignFieldName = getColumnNameForField(targetForeignField, targetForeignFieldDbAnnotation)
		val values = ContentValues(1).apply {
			putOrThrow(targetForeignFieldName, parentId)
		}
		val inClause = inClauseOf(targetIds)
		val targetTableName = classAnnotationRetriever.getTableNameOrThrow(targetClass)
		val targetIdFieldName = fieldAnnotationRetriever.getColumnNameForField(targetIdField)

		// Update references to all target objects
		database.update(targetTableName, values, "$targetIdFieldName ${inClause.selector}", inClause.values)

		if (!PoetryOptions.isEnabled(options, PoetryOptions.DISABLE_FOREIGN_COLLECTION_CLEANUP)) {
			// Remove all objects that are not referenced to the parent anymore
			val idToCleanUp = QueryUtils.parseAttribute(parentId)
			val deleteSelectClause = "$targetIdFieldName NOT ${inClause.selector} AND $targetForeignFieldName = $idToCleanUp"
			// TODO: add idToCleanUp to delete whereArgs array
			database.delete(targetTableName, deleteSelectClause, inClause.values)
		}
	}

	private fun jsonKeyAndFieldOf(jsonKey: String, modelClass: Class<*>): JsonKeyAndField? {
		val field = fieldRetriever.getField(modelClass, jsonKey)
		// If Field is not found, we might want to log that error
		return if (field == null) {
			if (!PoetryOptions.isEnabled(options, PoetryOptions.DISABLE_IGNORED_ATTRIBUTES_WARNING)) {
				Log.w(logTag, "Ignored attribute $jsonKey because it wasn't found in ${modelClass.simpleName} as a DatabaseField")
			}
			null
		} else {
			JsonKeyAndField(jsonKey, field)
		}
	}
}
