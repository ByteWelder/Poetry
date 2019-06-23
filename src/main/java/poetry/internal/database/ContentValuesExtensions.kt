package poetry.internal.database

import android.content.ContentValues
import java.util.Date

fun ContentValues.put(key: String, value: Any?): Boolean {
	if (value == null) {
		putNull(key)
		return true
	}

	val valueClass = checkNotNull(value).javaClass

	if (Int::class.java == valueClass || java.lang.Integer::class.java == valueClass) {
		put(key, value as Int)
	} else if (Long::class.java == valueClass || java.lang.Long::class.java == valueClass) {
		put(key, value as Long)
	} else if (Short::class.java == valueClass || java.lang.Short::class.java == valueClass) {
		put(key, value as Short)
	} else if (Byte::class.java == valueClass || java.lang.Byte::class.java == valueClass) {
		put(key, value as Byte)
	} else if (Boolean::class.java == valueClass || java.lang.Boolean::class.java == valueClass) {
		put(key, value as Boolean)
	} else if (Float::class.java == valueClass || java.lang.Float::class.java == valueClass) {
		put(key, value as Float)
	} else if (Double::class.java == valueClass || java.lang.Double::class.java == valueClass) {
		put(key, value as Double)
	} else if (CharSequence::class.java.isAssignableFrom(valueClass)
			|| java.lang.CharSequence::class.java.isAssignableFrom(valueClass)
			|| Date::class.java.isAssignableFrom(valueClass)) {
		put(key, value.toString())
	} else {
		return false
	}

	return true
}

fun ContentValues.putOrThrow(key: String, value: Any?) {
	// TODO: make test with null value
	if (!put(key, value)) {
		val typeString = value?.javaClass?.name ?: "[unknown]"
		throw RuntimeException("failed to copy value \"$value\" from key \"$key\" because the type $typeString is not supported")
	}
}
