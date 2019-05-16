package poetry.json

import android.content.ContentValues
import org.json.JSONException
import org.json.JSONObject
import java.util.*

internal object JsonUtils {

	@Throws(JSONException::class)
	fun getValue(jsonObject: JSONObject, jsonKey: String, type: Class<*>): Any? {
		if (!jsonObject.has(jsonKey)) {
			return null
		}

		return if (Int::class.java.isAssignableFrom(type) || java.lang.Integer::class.java.isAssignableFrom(type)) {
			jsonObject.getInt(jsonKey)
		} else if (Long::class.java.isAssignableFrom(type) || java.lang.Long::class.java.isAssignableFrom(type)) {
			jsonObject.getLong(jsonKey)
		} else if (Boolean::class.java.isAssignableFrom(type) || java.lang.Boolean::class.java.isAssignableFrom(type)) {
			jsonObject.getBoolean(jsonKey)
		} else if (CharSequence::class.java.isAssignableFrom(type) || java.lang.CharSequence::class.java.isAssignableFrom(type)) {
			jsonObject.getString(jsonKey)
		} else if (Double::class.java.isAssignableFrom(type) || java.lang.Double::class.java.isAssignableFrom(type)) {
			jsonObject.getDouble(jsonKey)
		} else if (Float::class.java.isAssignableFrom(type) || java.lang.Float::class.java.isAssignableFrom(type)) {
			jsonObject.getDouble(jsonKey).toFloat()
		} else {
			throw RuntimeException("unsupported type: " + type.name + " (only Integer, Long, Boolean, String, Double and Float are supported)")
		}
	}

	@Throws(JSONException::class)
	fun copyContentValue(jsonObject: JSONObject, jsonKey: String, values: ContentValues, key: String): Boolean {
		if (!jsonObject.has(jsonKey)) {
			return false
		} else {
			val value = jsonObject.get(jsonKey)
			return copyValue(value, key, values)
		}
	}

	fun copyValue(value: Any, key: String, values: ContentValues): Boolean {
		val valueClass = value.javaClass

		if (Int::class.java == valueClass || java.lang.Integer::class.java == valueClass) {
			values.put(key, value as Int)
		} else if (Long::class.java == valueClass || java.lang.Long::class.java == valueClass) {
			values.put(key, value as Long)
		} else if (Short::class.java == valueClass || java.lang.Short::class.java == valueClass) {
			values.put(key, value as Short)
		} else if (Byte::class.java == valueClass || java.lang.Byte::class.java == valueClass) {
			values.put(key, value as Byte)
		} else if (Boolean::class.java == valueClass || java.lang.Boolean::class.java == valueClass) {
			values.put(key, value as Boolean)
		} else if (Float::class.java == valueClass || java.lang.Float::class.java == valueClass) {
			values.put(key, value as Float)
		} else if (Double::class.java == valueClass || java.lang.Double::class.java == valueClass) {
			values.put(key, value as Double)
		} else if (CharSequence::class.java.isAssignableFrom(valueClass)
				|| java.lang.CharSequence::class.java.isAssignableFrom(valueClass)
				|| Date::class.java.isAssignableFrom(valueClass)) {
			values.put(key, value.toString())
		} else {
			return false
		}

		return true
	}
}