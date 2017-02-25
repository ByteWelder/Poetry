package poetry.json;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import poetry.json.annotations.ForeignCollectionFieldSingleTarget;
import poetry.json.annotations.ManyToManyField;
import poetry.reflection.AnnotationRetriever;
import poetry.reflection.FieldRetriever;
import poetry.reflection.OrmliteReflection;
import poetry.utils.QueryUtils;

/**
 * Persist a JSONObject or JSONArray to an SQLite database by parsing annotations (both from OrmLite and custom ones).
 */
public class JsonPersister {
	/**
	 * When a foreign collection is imported (one-to-many relationship),
	 * the normal behavior is that the old children are deleted.
	 * This options allows you to disable that behavior.
	 */
	public static final int OPTION_DISABLE_FOREIGN_COLLECTION_CLEANUP = 0x0001;
	/**
	 * Don't display warnings when JSON attributes are not annotated as a field in an object.
	 */
	public static final int OPTION_DISABLE_IGNORED_ATTRIBUTES_WARNING = 0x0002;

	private final SQLiteDatabase database;
	private final int options;
	private final FieldRetriever fieldRetriever = new FieldRetriever();
	private final AnnotationRetriever annotationRetriever = new AnnotationRetriever();

	public JsonPersister(SQLiteDatabase writableDatabase) {
		this(writableDatabase, 0);
	}

	/**
	 * Constructor.
	 *
	 * @param writableDatabase the database used for persistence
	 * @param options          0 or a combination of 1 or more options as defined by {@link JsonPersister}.OPTION_*
	 */
	public JsonPersister(SQLiteDatabase writableDatabase, int options) {
		database = writableDatabase;
		this.options = options;
	}

	/**
	 * Recursively persist this object and all its children.
	 *
	 * @param modelClass the type to persist
	 * @param jsonObject the json to process
	 * @param <IdType>   the ID type to return
	 * @return the ID of the persisted object
	 * @throws JSONException when something went wrong through parsing, this also fails the database transaction and results in no data changes
	 */
	public <IdType> IdType persistObject(Class<?> modelClass, JSONObject jsonObject) throws JSONException {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			Log.w(getClass().getName(), "please call persistObject() on a background thread");
		}

		try {
			enableWriteAheadLogging();

			database.beginTransactionNonExclusive();

			IdType id = persistObjectInternal(modelClass, jsonObject);

			database.setTransactionSuccessful();

			return id;
		} finally {
			endTransaction();
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
	public <IdType> List<IdType> persistArray(Class<?> modelClass, JSONArray jsonArray) throws JSONException {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			Log.w(getClass().getName(), "please call persistArray() on a background thread");
		}

		try {
			enableWriteAheadLogging();

			database.beginTransactionNonExclusive();

			List<IdType> id_list = persistArrayOfObjects(modelClass, jsonArray);

			database.setTransactionSuccessful();

			return id_list;
		} catch (JSONException e) {
			throw e;
		} finally {
			endTransaction();
		}
	}

	private void enableWriteAheadLogging() {
		try {
			// Write Ahead Logging (WAL) mode cannot be enabled or disabled while there are transactions in progress.
			if (!database.inTransaction()) {
				database.enableWriteAheadLogging();
			}
		} catch (IllegalStateException e) {
		    /*
             * To catch: java.lang.IllegalStateException: Write Ahead Logging (WAL) mode cannot be enabled or disabled while there are transactions in progress.  Finish all transactions and release all active database connections first.
             * This exception still gets triggered, possibly because the transaction is started right after inTransaction() is checked.
             */
			if (Build.VERSION.SDK_INT >= 16 && !database.isWriteAheadLoggingEnabled()) {
				Log.w(getClass().getName(), "Write Ahead Logging is not enabled because a transaction was active");
			}
		}
	}

	private void endTransaction() {
		if (database.inTransaction()) {
			try {
				database.endTransaction();
			} catch (IllegalStateException e) {
				Log.w(getClass().getName(), "endTransaction() failed - this does not mean there was a rollback, it just means that the transaction was closed earlier than expeced.");
			}
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
	 */
	private <IdType> IdType persistObjectInternal(Class<?> modelClass, JSONObject jsonObject) throws JSONException {
		DatabaseTable table_annotation = modelClass.getAnnotation(DatabaseTable.class);

		if (table_annotation == null) {
			throw new RuntimeException("DatabaseTable annotation not found for " + modelClass.getName());
		}

		ContentValues values = new ContentValues();
		Iterator<?> json_keys = jsonObject.keys();
		List<ForeignCollectionMapping> foreign_collection_mappings = new ArrayList<>();

		String table_name = OrmliteReflection.getTableName(modelClass, table_annotation);

		// We want to know the object ID because we need it to resolve one-to-many relationships (foreign collection fields)
		String id_field_name = null;
		Object object_id = null;

		// Process all JSON keys and map them to the database
		while (json_keys.hasNext()) {
			// Get the next key
			String json_key = (String) json_keys.next();

			// Find a Field with the same name as the key
			// TODO: use JsonProperty annotation to get an optional name override

			Field field = fieldRetriever.getField(modelClass, json_key);

			if (field == null) {
				if (!isOptionEnabled(options, OPTION_DISABLE_IGNORED_ATTRIBUTES_WARNING)) {
					Log.w(getClass().getName(), String.format("ignored attribute %s because it wasn't found in %s as a DatabaseField", json_key, modelClass.getSimpleName()));
				}

				continue;
			}

			DatabaseField database_field = annotationRetriever.getAnnotation(field, DatabaseField.class);

			// DatabaseField is used for: object IDs, simple key-values and one-to-one relationships
			if (database_field != null) {
				// Object IDs are a special case because we need to insert a new object if the object doesn't exist yet
				// and we also want to retrieve the value to return it in this method and to resolve one-to-many relationships for child objects
				if (OrmliteReflection.isId(database_field)) {
					object_id = processIdField(database_field, field, jsonObject, json_key, table_name);
					id_field_name = OrmliteReflection.getFieldName(field, database_field);
				} else // object exists, so process its value or reference
				{
					processDatabaseField(database_field, field, jsonObject, json_key, modelClass, values);
				}
			} else { // check if we have a ForeignCollectionField (which is used for one-to-many relationships)
				ForeignCollectionField foreign_collection_field = annotationRetriever.getAnnotation(field, ForeignCollectionField.class);

				if (foreign_collection_field != null) {
					JSONArray json_array = !jsonObject.isNull(json_key) ? jsonObject.getJSONArray(json_key) : null;

					ForeignCollectionMapping foreign_collection_mapping = new ForeignCollectionMapping(field, json_array);
					foreign_collection_mappings.add(foreign_collection_mapping);
				}
			}
		}

		// Determine the object ID
		if (object_id == null || id_field_name == null) {
			Field id_field = OrmliteReflection.findIdField(annotationRetriever, modelClass);

			if (id_field == null) {
				throw new SQLiteException("class " + modelClass.getName() + " doesn't have a DatabaseField that is marked as being an ID");
			}

			// we don't have to check for id_database_field being null because OrmliteReflection.findIdField implied it is there
			DatabaseField id_database_field = annotationRetriever.getAnnotation(id_field, DatabaseField.class);

			id_field_name = OrmliteReflection.getFieldName(id_field, id_database_field);

			long inserted_id = database.insert("'" + table_name + "'", id_field_name, new ContentValues());

			if (inserted_id == -1) {
				throw new SQLiteException("failed to insert " + modelClass.getName() + " with id field " + id_field_name);
			}

			object_id = inserted_id;
		}

		// Process regular fields
		if (values.size() > 0) {
			database.update("'" + table_name + "'", values, id_field_name + " = ?", new String[]{object_id.toString()});
		}

		Log.i(getClass().getName(), String.format("imported %s (%s=%s)", modelClass.getSimpleName(), id_field_name, object_id.toString()));

		// Process foreign collection fields for inserted object
		for (ForeignCollectionMapping foreign_collection_mapping : foreign_collection_mappings) {
			ManyToManyField many_to_many_field = annotationRetriever.getAnnotation(foreign_collection_mapping.getField(), ManyToManyField.class);

			if (many_to_many_field != null) {
				processManyToMany(many_to_many_field, foreign_collection_mapping, object_id, modelClass);
			} else {
				processManyToOne(foreign_collection_mapping, object_id, modelClass);
			}
		}

		return (IdType) object_id;
	}

	private <IdType> List<IdType> persistArrayOfObjects(Class<?> modelClass, JSONArray jsonArray) throws JSONException {
		List<IdType> results = new ArrayList<>(jsonArray.length());

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject json_object = jsonArray.getJSONObject(i);

			IdType object_id = persistObjectInternal(modelClass, json_object);

			results.add(object_id);
		}

		return results;
	}

	private List<Object> persistArrayOfBaseTypes(Class<?> modelClass, JSONArray jsonArray, ForeignCollectionFieldSingleTarget singleTargetField) throws JSONException {
		DatabaseTable table_annotation = modelClass.getAnnotation(DatabaseTable.class);

		if (table_annotation == null) {
			throw new RuntimeException("DatabaseTable annotation not found for " + modelClass.getName());
		}

		String table_name = OrmliteReflection.getTableName(modelClass, table_annotation);

		List<Object> results = new ArrayList<>(jsonArray.length());

		for (int i = 0; i < jsonArray.length(); i++) {
			Object value_object = jsonArray.get(i);

			ContentValues content_values = new ContentValues();
			content_values.put(singleTargetField.targetField(), value_object.toString());

			long inserted_id = database.insert("'" + table_name + "'", singleTargetField.targetField(), content_values);

			if (inserted_id == -1) {
				throw new SQLiteException("failed to insert " + modelClass.getName());
			}

			results.add(inserted_id);
		}

		return results;
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
	private Object processIdField(DatabaseField databaseField, Field field, JSONObject jsonObject, String jsonKey, String tableName) throws JSONException {
		String db_field_name = OrmliteReflection.getFieldName(field, databaseField);

		Object object_id = JsonUtils.getValue(jsonObject, jsonKey, field.getType());

		if (object_id == null) {
			throw new RuntimeException(String.format("failed to get a value from JSON with key %s and type %s", jsonKey, field.getType().getName()));
		}

		String sql = String.format("SELECT * FROM '%s' WHERE %s = ? LIMIT 1", tableName, db_field_name);
		String[] selection_args = new String[]{object_id.toString()};
		Cursor cursor = database.rawQuery(sql, selection_args);
		boolean object_exists = (cursor.getCount() > 0);
		cursor.close();

		if (object_exists) {
			// return existing object id
			return object_id;
		} else { // create object
			ContentValues values = new ContentValues(1);

			if (!JsonUtils.copyValue(object_id, db_field_name, values)) {
				throw new JSONException(String.format("failed to process id field %s for table %s and jsonKey %s", field.getName(), tableName, jsonKey));
			}

			long inserted_id = database.insert("'" + tableName + "'", null, values);

			if (inserted_id == -1) {
				throw new SQLiteException(String.format("failed to insert %s with id %s=%s", field.getType().getName(), db_field_name, object_id.toString()));
			}

			Log.i(getClass().getName(), String.format("prepared %s row (id=%s/%s)", tableName, object_id.toString(), Long.toString(inserted_id)));

			return object_id; // don't return inserted_id, because it's always long (while the target type might be int or another type)
		}
	}

	private void processDatabaseField(DatabaseField databaseField, Field field, JSONObject jsonParentObject, String jsonKey, Class<?> modelClass, ContentValues values) throws JSONException {
		String db_field_name = OrmliteReflection.getFieldName(field, databaseField);

		if (jsonParentObject.isNull(jsonKey)) {
			values.putNull(db_field_name);
		} else if (OrmliteReflection.isForeign(databaseField)) {
			JSONObject foreign_object = jsonParentObject.optJSONObject(jsonKey);

			if (foreign_object != null) {
				//If the JSON includes the forein object, try to persist it

				Object foreign_object_id = persistObjectInternal(field.getType(), foreign_object);

				if (!JsonUtils.copyValue(foreign_object_id, db_field_name, values)) {
					throw new RuntimeException("failed to copy values for key " + jsonKey + " in " + modelClass.getName() + ": key type " + foreign_object_id.getClass() + " is not supported");
				}
			} else {
				//The JSON does not include the foreign object, see if it is a valid key for the foreign object

				Field foreign_object_id_field = OrmliteReflection.findIdField(annotationRetriever, field.getType());

				if (foreign_object_id_field == null) {
					throw new RuntimeException("failed to find id field for foreign object " + field.getType().getName() + " in " + modelClass.getName());
				}

				Object foreign_object_id = JsonUtils.getValue(jsonParentObject, jsonKey, foreign_object_id_field.getType());

				if (foreign_object_id == null) {
					throw new RuntimeException("incompatible id type for foreign object " + field.getType().getName() + " in " + modelClass.getName() + " (expected " + foreign_object_id_field.getType().getName() + ")");
				}

				if (!JsonUtils.copyValue(foreign_object_id, db_field_name, values)) {
					throw new RuntimeException("failed to copy values for key " + jsonKey + " in " + modelClass.getName() + ": key type " + foreign_object_id.getClass() + " is not supported");
				}
			}
		} else // non-foreign
		{
			if (!JsonUtils.copyContentValue(jsonParentObject, jsonKey, values, db_field_name)) {
				Log.w(getClass().getName(), String.format("attribute type %s has an unsupported type while parsing %s", jsonKey, modelClass.getSimpleName()));
			}
		}
	}

	private void processManyToMany(ManyToManyField manyToManyField, ForeignCollectionMapping foreignCollectionMapping, Object parentId, Class<?> parentClass) throws JSONException {
		if (foreignCollectionMapping.getJsonArray() == null) {
			// TODO: Delete mapping
			Log.w(getClass().getName(), String.format("Mapping %s for type %s was null. Ignored it, but it should be deleted!", foreignCollectionMapping.getField().getName(), foreignCollectionMapping.getField().getType().getName()));
			return;
		}

		Field foreign_collection_field = foreignCollectionMapping.getField();

		Class<?> target_class = OrmliteReflection.getForeignCollectionParameterType(foreign_collection_field);
		Field target_id_field = OrmliteReflection.findIdField(annotationRetriever, target_class);

		if (target_id_field == null) {
			throw new RuntimeException("no id field found while processing foreign collection relation for " + target_class.getName());
		}

		Field target_foreign_field = OrmliteReflection.findForeignField(annotationRetriever, target_class, parentClass);

		if (target_foreign_field == null) {
			throw new RuntimeException("no foreign field found while processing foreign collection relation for " + target_class.getName());
		}

		Field target_target_field = fieldRetriever.getFirstFieldOfType(target_class, manyToManyField.targetType());

		if (target_target_field == null) {
			throw new RuntimeException("ManyToMany problem: no ID field found for type " + manyToManyField.targetType().getName());
		}

		List<Object> target_target_ids = persistArrayOfObjects(target_target_field.getType(), foreignCollectionMapping.getJsonArray());

		// TODO: cache table name
		String target_table_name = OrmliteReflection.getTableName(annotationRetriever, target_class);
		DatabaseField target_foreign_db_field = annotationRetriever.getAnnotation(target_foreign_field, DatabaseField.class);
		String target_foreign_field_name = OrmliteReflection.getFieldName(target_foreign_field, target_foreign_db_field);

		String delete_select_clause = target_foreign_field_name + " = " + QueryUtils.parseAttribute(parentId);
		database.delete("'" + target_table_name + "'", delete_select_clause, new String[]{});

		DatabaseField target_target_database_field = annotationRetriever.getAnnotation(target_target_field, DatabaseField.class);
		String target_target_field_name = OrmliteReflection.getFieldName(target_target_field, target_target_database_field);

		// Insert new references
		for (int i = 0; i < target_target_ids.size(); ++i) {
			ContentValues values = new ContentValues(2);

			if (!JsonUtils.copyValue(parentId, target_foreign_field_name, values)) {
				throw new RuntimeException("parent id copy failed");
			}

			if (!JsonUtils.copyValue(target_target_ids.get(i), target_target_field_name, values)) {
				throw new RuntimeException("target id copy failed");
			}

			if (database.insert("'" + target_table_name + "'", null, values) == -1) {
				throw new RuntimeException("failed to insert item in " + target_table_name);
			}
		}
	}

	private void processManyToOne(ForeignCollectionMapping foreignCollectionMapping, Object parentId, Class<?> parentClass) throws JSONException {
		if (foreignCollectionMapping.getJsonArray() == null) {
			// TODO: Delete mapping
			Log.w(getClass().getName(), String.format("Mapping %s for type %s was null. Ignored it, but it should be deleted!", foreignCollectionMapping.getField().getName(), foreignCollectionMapping.getField().getType().getName()));
			return;
		}

		Field foreign_collection_field = foreignCollectionMapping.getField();

		Class<?> target_class = OrmliteReflection.getForeignCollectionParameterType(foreign_collection_field);
		Field target_id_field = OrmliteReflection.findIdField(annotationRetriever, target_class);

		if (target_id_field == null) {
			throw new RuntimeException("no id field found while processing foreign collection relation for " + target_class.getName());
		}

		Field target_foreign_field = OrmliteReflection.findForeignField(annotationRetriever, target_class, parentClass);

		if (target_foreign_field == null) {
			throw new RuntimeException("no foreign field found while processing foreign collection relation for " + target_class.getName());
		}

		ForeignCollectionFieldSingleTarget single_target_field = annotationRetriever.getAnnotation(foreignCollectionMapping.getField(), ForeignCollectionFieldSingleTarget.class);

		List<Object> target_ids;

		if (single_target_field == null) {
			target_ids = persistArrayOfObjects(target_class, foreignCollectionMapping.getJsonArray());
		} else {
			target_ids = persistArrayOfBaseTypes(target_class, foreignCollectionMapping.getJsonArray(), single_target_field);
		}

		DatabaseField target_foreign_field_db_annotation = annotationRetriever.getAnnotation(target_foreign_field, DatabaseField.class);
		String target_foreign_field_name = OrmliteReflection.getFieldName(target_foreign_field, target_foreign_field_db_annotation);

		ContentValues values = new ContentValues(1);

		if (!JsonUtils.copyValue(parentId, target_foreign_field_name, values)) {
			throw new RuntimeException("failed to copy foreign key " + target_foreign_field_name + " in " + parentClass.getName() + ": key type " + parentId.getClass() + " is not supported");
		}

		String[] target_id_args = new String[target_ids.size()];
		String in_clause = QueryUtils.createInClause(target_ids, target_id_args);

		// update references to all target objects
		String target_table_name = OrmliteReflection.getTableName(annotationRetriever, target_class);
		String target_id_field_name = OrmliteReflection.getFieldName(annotationRetriever, target_id_field);

		String update_select_clause = target_id_field_name + " " + in_clause;
		database.update("'" + target_table_name + "'", values, update_select_clause, target_id_args);

		if (!isOptionEnabled(options, OPTION_DISABLE_FOREIGN_COLLECTION_CLEANUP)) {
			// remove all objects that are not referenced to the parent anymore
			String delete_select_clause = target_id_field_name + " NOT " + in_clause + " AND " + target_foreign_field_name + " = " + QueryUtils.parseAttribute(parentId);
			database.delete("'" + target_table_name + "'", delete_select_clause, target_id_args);
		}
	}

	/**
	 * Check if an option is enabled
	 *
	 * @param optionsSet  the compound of option values (combined with logical OR operator)
	 * @param optionCheck one or more options to check (combined with logical OR operator)
	 * @return true when all the options from optionCheck are contained in optionsSet
	 */
	private static boolean isOptionEnabled(int optionsSet, int optionCheck) {
		return (optionsSet & optionCheck) == optionCheck;
	}

	/**
	 * All necessary data to map an array of objects onto the provided parent field.
	 */
	private static class ForeignCollectionMapping {
		private final Field field;
		private final JSONArray jsonArray;

		/**
		 * @param field
		 * @param jsonArray or null
		 */
		ForeignCollectionMapping(Field field, JSONArray jsonArray) {
			this.field = field;
			this.jsonArray = jsonArray;
		}

		Field getField() {
			return field;
		}

		JSONArray getJsonArray() {
			return jsonArray;
		}
	}
}
