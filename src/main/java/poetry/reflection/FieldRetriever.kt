package poetry.reflection

import poetry.json.annotations.MapFrom
import java.lang.reflect.Field
import java.util.*

/**
 * FieldRetriever caches [Field] objects to improve performance.
 *
 *
 * In a test with hundreds of objects, OrmliteReflection.findField() took over 50% CPU time.
 * When looking at the Android source code it shows that this is a fairly heavy method.
 * Considering that Poetry uses only a certain amount of model classes and fields, it
 * makes sense to cache this in memory.
 *
 *
 * Reference: http://grepcode.com/file/repo1.maven.org/maven2/org.robolectric/android-all/4.4_r1-robolectric-1/libcore/reflect/AnnotationAccess.java#AnnotationAccess.getDeclaredAnnotation%28java.lang.reflect.AnnotatedElement%2Cjava.lang.Class%29
 */
class FieldRetriever {
	// Maps: model class -> json type -> Field instance
	private val fieldJsonCache = HashMap<Class<*>, HashMap<String, Field?>>()
	// Maps: model class -> field type -> Field instance
	private val fieldTypeCache = HashMap<Class<*>, HashMap<Class<*>, Field?>>()

	/**
	 * Retrieve a [Field] for a model.
	 *
	 * @param modelClass the class to search for the Field
	 * @param jsonKey    the json key that is mapped to the field
	 * @return the found Field or null
	 */
	fun getField(modelClass: Class<*>, jsonKey: String): Field? {
		// Try to retrieve it from cache
		var field = getCachedField(modelClass, jsonKey)

		// If not cached, try reflection
		if (field == null) {
			field = findField(modelClass, jsonKey)

			// Null values are also cached because it will make the next failure quicker
			setCachedField(modelClass, jsonKey, field)
		}

		return field
	}

	private fun getCachedField(classObject: Class<*>, fieldName: String): Field? {
		val field_map = fieldJsonCache[classObject]
		return if (field_map != null) field_map[fieldName] else null
	}

	private fun setCachedField(classObject: Class<*>, fieldName: String, field: Field?) {
		var field_map = fieldJsonCache[classObject]

		if (field_map == null) {
			field_map = HashMap()
			fieldJsonCache[classObject] = field_map
		}

		field_map[fieldName] = field
	}

	/**
	 * Find a field in a model, providing its JSON attribute name
	 *
	 * @param modelClass the model class
	 * @param name       the name of the JSON field
	 * @return the Field that is found or null
	 */
	private fun findField(modelClass: Class<*>, name: String): Field? {
		// Check all the fields in the model
		for (field in modelClass.declaredFields) {
			// Direct match?
			if (field.name == name) {
				return field
			}

			// MapFrom-annotated match?
			val map_from = field.getAnnotation(MapFrom::class.java)

			if (map_from != null && name == map_from.value) {
				return field
			}
		}

		return if (modelClass.superclass == null) {
			null
		} else findField(modelClass.superclass!!, name)

		// Recursively check superclass
	}

	/**
	 * Retrieve a [Field] for a model.
	 *
	 * @param parentClass the class to search for the Field
	 * @param fieldClass  the Field class to search for
	 * @return the found Field or null
	 */
	fun getFirstFieldOfType(parentClass: Class<*>, fieldClass: Class<*>): Field? {
		// Try to retrieve it from cache
		var field = getCachedField(parentClass, fieldClass)

		// If not cached, try reflection
		if (field == null) {
			field = findFirstFieldOfType(parentClass, fieldClass)

			// Null values are also cached because it will make the next failure quicker
			setCachedField(parentClass, fieldClass, field)
		}

		return field
	}

	private fun getCachedField(parentClass: Class<*>, fieldClass: Class<*>): Field? {
		val fieldMap = fieldTypeCache[parentClass]
		return if (fieldMap != null) fieldMap[fieldClass] else null
	}

	private fun setCachedField(parentClass: Class<*>, fieldClass: Class<*>, field: Field?) {
		var fieldMap = fieldTypeCache[parentClass]

		if (fieldMap == null) {
			fieldMap = HashMap()
			fieldTypeCache[parentClass] = fieldMap
		}

		fieldMap[fieldClass] = field
	}

	companion object {

		/**
		 * Finds a field of a certain type in a given parent type
		 *
		 * @param parentClass the parent class that holds the field
		 * @param fieldClass  the field class to search for
		 * @return the found first Field of the specified type or null
		 */
		fun findFirstFieldOfType(parentClass: Class<*>, fieldClass: Class<*>): Field? {
			for (field in parentClass.declaredFields) {
				if (field.type == fieldClass) {
					return field
				}
			}

			return if (parentClass.superclass == null) {
				null
			} else findFirstFieldOfType(parentClass.superclass!!, fieldClass)

			// Recursively check superclass
		}
	}
}
