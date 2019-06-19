package poetry.internal.reflection

import poetry.annotations.MapFrom
import java.lang.reflect.Field

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
internal class FieldRetriever {
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
		val fieldMap = fieldJsonCache[classObject]
		return if (fieldMap != null) fieldMap[fieldName] else null
	}

	private fun setCachedField(classObject: Class<*>, fieldName: String, field: Field?) {
		var fieldMap = fieldJsonCache[classObject]

		if (fieldMap == null) {
			fieldMap = HashMap()
			fieldJsonCache[classObject] = fieldMap
		}

		fieldMap[fieldName] = field
	}

	/**
	 * Find a field in a model, providing its JSON attribute name
	 *
	 * @param modelClass the model class
	 * @param name the name of the JSON field
	 * @return the Field that is found or null
	 */
	private fun findField(modelClass: Class<*>, name: String): Field? {
		// Check all the fields in the model
		for (field in modelClass.declaredFields) {
			// Direct match
			if (field.name == name) {
				return field
			}

			// MapFrom-annotated match
			val mapFrom = field.getAnnotation(MapFrom::class.java)
			if (mapFrom != null && name == mapFrom.value) {
				return field
			}
		}

		// Recursively check superclass if possible
		val superClass = modelClass.superclass
		return if (superClass != null) {
			findField(superClass, name)
		} else {
			null
		}
	}

	/**
	 * Finds a field of a certain type in a given parent type
	 *
	 * @param parentClass the parent class that holds the field
	 * @param fieldClass  the field class to search for
	 */
	fun findFirstFieldOfTypeOrThrow(parentClass: Class<*>, fieldClass: Class<*>): Field {
		return checkNotNull(findFirstFieldOfType(parentClass, fieldClass)) {
			"No field found of type ${fieldClass.name} in ${parentClass.name}"
		}
	}

	/**
	 * Retrieve a [Field] for a model.
	 *
	 * @param parentClass the class to search for the Field
	 * @param fieldClass  the Field class to search for
	 */
	private fun findFirstFieldOfType(parentClass: Class<*>, fieldClass: Class<*>): Field? {
		// Try to retrieve it from cache
		var field = getCachedField(parentClass, fieldClass)

		// If not cached, use reflection and cache
		if (field == null) {
			field = parentClass.findFirstFieldOfType(fieldClass)
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
}

/**
 * Finds a field of a certain type in a given parent type
 *
 * @param fieldClass  the field class to search for
 * @return the found first Field of the specified type or null
 */
private fun Class<*>.findFirstFieldOfType(fieldClass: Class<*>): Field? {
	val field = declaredFields.firstOrNull { it.type == fieldClass }
	if (field != null) {
		return field
	}

	// Recursively check superclass
	return superclass?.findFirstFieldOfType(fieldClass)
}