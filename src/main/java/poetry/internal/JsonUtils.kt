package poetry.internal

import android.content.ContentValues
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import poetry.internal.database.put

class JsonArrayAnyIterator(private val array: JSONArray) : Iterator<Any> {
	private var currentIndex = 0
	private val lastIndex = array.length() - 1

	override fun hasNext() = currentIndex < array.length()

	override fun next() = array[++currentIndex]
}

class JsonArrayJsonObjectIterator(private val array: JSONArray) : Iterator<JSONObject> {
	private var currentIndex = 0
	private val lastIndex = array.length() - 1

	override fun hasNext() = currentIndex < array.length()

	override fun next() = array.getJSONObject(++currentIndex)
}

fun JSONArray.toIterable(): Iterable<Any> {
	val jsonArray = this
	return object : Iterable<Any> {
		override fun iterator(): Iterator<Any> {
			return JsonArrayAnyIterator(jsonArray)
		}
	}
}

fun JSONArray.toIterableJsonObject(): Iterable<JSONObject> {
	val jsonArray = this
	return object : Iterable<JSONObject> {
		override fun iterator(): Iterator<JSONObject> {
			return JsonArrayJsonObjectIterator(jsonArray)
		}
	}
}

@Throws(JSONException::class)
fun JSONObject.getValue(jsonKey: String, type: Class<*>): Any {
	if (!has(jsonKey)) {
		throw RuntimeException("Failed to get a value from JSON with key $jsonKey and type ${type.name} (key not found)")
	}

	return if (Int::class.java.isAssignableFrom(type) || java.lang.Integer::class.java.isAssignableFrom(type)) {
		getInt(jsonKey)
	} else if (Long::class.java.isAssignableFrom(type) || java.lang.Long::class.java.isAssignableFrom(type)) {
		getLong(jsonKey)
	} else if (Boolean::class.java.isAssignableFrom(type) || java.lang.Boolean::class.java.isAssignableFrom(type)) {
		getBoolean(jsonKey)
	} else if (CharSequence::class.java.isAssignableFrom(type) || java.lang.CharSequence::class.java.isAssignableFrom(type)) {
		getString(jsonKey)
	} else if (Double::class.java.isAssignableFrom(type) || java.lang.Double::class.java.isAssignableFrom(type)) {
		getDouble(jsonKey)
	} else if (Float::class.java.isAssignableFrom(type) || java.lang.Float::class.java.isAssignableFrom(type)) {
		getDouble(jsonKey).toFloat()
	} else {
		throw RuntimeException("Unsupported type ${type.name} (only Integer, Long, Boolean, String, Double and Float are supported)")
	}
}

internal object JsonUtils {

	@Throws(JSONException::class)
	fun copyContentValue(jsonObject: JSONObject, jsonKey: String, contentValues: ContentValues, key: String): Boolean {
		return if (!jsonObject.has(jsonKey)) {
			false
		} else {
			val jsonValue = jsonObject.get(jsonKey)
			// TODO: should we call putOrThrow() instead?
			contentValues.put(key, jsonValue)
		}
	}
}