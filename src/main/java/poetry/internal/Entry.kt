package poetry.internal

import com.j256.ormlite.field.DatabaseField
import org.json.JSONArray
import poetry.annotations.ManyToManyField
import java.lang.reflect.Field

internal sealed class Entry(
	val field: Field,
	val jsonKey: String
) {

	val modelClass = field.declaringClass

	class FieldEntry(
		field: Field,
		jsonKey: String,
		val databaseField: DatabaseField
	) : Entry(field, jsonKey)

	class ManyToManyEntry(
		field: Field,
		jsonKey: String,
		val jsonArray: JSONArray,
		val manyToManyField: ManyToManyField
	) : Entry(field, jsonKey)

	class ManyToOneEntry(
		field: Field,
		jsonKey: String,
		val jsonArray: JSONArray
	) : Entry(field, jsonKey)

	class Invalid(field: Field, jsonKey: String) : Entry(field, jsonKey)
}