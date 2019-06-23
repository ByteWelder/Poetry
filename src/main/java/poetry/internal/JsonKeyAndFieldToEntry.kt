package poetry.internal

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import org.json.JSONException
import org.json.JSONObject
import poetry.annotations.ManyToManyField
import poetry.internal.reflection.FieldAnnotationRetriever

/**
 * Converts a JSON key and Field information the provided class to create a `JsonKeyAndField` instance using reflection.
 *
 * It uses the `FieldAnnotationRetriever` to find a `DatabaseField` or `ForeignCollectionField` annotation.
 * Based on that, it can infer what kind of database entry is behind the key/value pair in JSON.
 */
internal fun JsonKeyAndField.toEntry(parentJsonObject: JSONObject, fieldAnnotationRetriever: FieldAnnotationRetriever): Entry {
	// TODO: Make test that uses JsonProperty annotation to get an optional name override
	val field = field
	val jsonKey = jsonKey
	val databaseField = fieldAnnotationRetriever.findAnnotation(field, DatabaseField::class.java)

	return if (databaseField != null) {
		// Value or one-to-one reference fields
		Entry.FieldEntry(field, jsonKey, databaseField)
	} else if (fieldAnnotationRetriever.hasAnnotation(field, ForeignCollectionField::class.java)) {
		// One-to-many or many-to-many relationship
		val jsonArray = parentJsonObject.optJSONArray(jsonKey) ?:
		throw JSONException("There was a recoverable JSON error: Mapping ${field.name} for type ${field.type.name} was null.")
		val manyToManyField = fieldAnnotationRetriever.findAnnotation(field, ManyToManyField::class.java)
		if (manyToManyField != null) {
			Entry.ManyToManyEntry(field, jsonKey, jsonArray, manyToManyField)
		} else {
			Entry.ManyToOneEntry(field, jsonKey, jsonArray)
		}
	} else {
		Entry.Invalid(field, jsonKey)
	}
}