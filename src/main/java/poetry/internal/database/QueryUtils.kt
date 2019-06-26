package poetry.internal.database

import java.security.InvalidParameterException
import java.util.Date

internal object QueryUtils {
	/**
	 * Convert an Object to a String so that it can be used as a query parameter.
	 * This method supports objects instantiated or derived from:
	 * Integer, Long, Float, Double, Boolean, Short, Byte, CharSequence and Date
	 *
	 * @param attribute the object to convert
	 * @return the String representing the input object
	 * @throws InvalidParameterException when the input object is not supported
	 */
	@Throws(InvalidParameterException::class)
	fun parseAttribute(attribute: Any?): String {
		return if (attribute == null) {
			return "NULL"
		} else if (Int::class.java.isAssignableFrom(attribute.javaClass)
				|| Integer::class.java.isAssignableFrom(attribute.javaClass)) {
			Integer.toString(attribute as Int)
		} else if (Long::class.java.isAssignableFrom(attribute.javaClass)
				|| java.lang.Long::class.java.isAssignableFrom(attribute.javaClass)) {
			java.lang.Long.toString(attribute as Long)
		} else if (Float::class.java.isAssignableFrom(attribute.javaClass)
				|| java.lang.Float::class.java.isAssignableFrom(attribute.javaClass)) {
			java.lang.Float.toString(attribute as Float)
		} else if (Double::class.java.isAssignableFrom(attribute.javaClass)
				|| java.lang.Double::class.java.isAssignableFrom(attribute.javaClass)) {
			java.lang.Double.toString(attribute as Double)
		} else if (Boolean::class.java.isAssignableFrom(attribute.javaClass)
				|| java.lang.Boolean::class.java.isAssignableFrom(attribute.javaClass)) {
			java.lang.Boolean.toString(attribute as Boolean)
		} else if (Short::class.java.isAssignableFrom(attribute.javaClass)
				|| java.lang.Short::class.java.isAssignableFrom(attribute.javaClass)) {
			java.lang.Short.toString(attribute as Short)
		} else if (Byte::class.java.isAssignableFrom(attribute.javaClass)
				|| java.lang.Byte::class.java.isAssignableFrom(attribute.javaClass)) {
			java.lang.Byte.toString(attribute as Byte)
		} else if (CharSequence::class.java.isAssignableFrom(attribute.javaClass)
				|| java.lang.CharSequence::class.java.isAssignableFrom(attribute.javaClass)
				|| Date::class.java.isAssignableFrom(attribute.javaClass)) {
			attribute.toString()
		} else {
			throw InvalidParameterException("parameter type not supported: " + attribute.javaClass.name)
		}
	}
}