package poetry.internal.reflection

import java.lang.reflect.Field

/**
 * AnnotationRetriever caches annotations to improve performance.
 * Fetching annotations is a CPU-heavy operation, so this classes caches the results.
 */
internal class FieldAnnotationRetriever {
	private val fieldAnnotationCache = HashMap<Field, HashMap<Class<out Annotation>, Annotation?>>()

	/**
	 * Retrieve a [Field] for a Field.
	 *
	 * @param field            the Field to search for the Annotation
	 * @param annotationClass  the annotation class to search for
	 * @param <AnnotationType> the annotation type to search for
	 * @return the found Annotation or null
	 */
	fun <AnnotationType : Annotation> findAnnotation(field: Field, annotationClass: Class<AnnotationType>): AnnotationType? {
		// Try to retrieve it from cache
		var annotation = findCachedAnnotation(field, annotationClass)

		// If not cached, try reflection
		if (annotation == null) {
			annotation = field.getAnnotation(annotationClass)

			// Null values are also cached because it will make the next failure quicker
			setCachedAnnotation(field, annotationClass, annotation)
		}

		return annotation as AnnotationType?
	}

	private fun findCachedAnnotation(field: Field, annotationClass: Class<out Annotation>): Annotation? {
		val annotationMap = fieldAnnotationCache[field]
		return if (annotationMap != null) annotationMap[annotationClass] else null
	}

	private fun setCachedAnnotation(field: Field, annotationClass: Class<out Annotation>, annotation: Annotation?) {
		var annotationMap: HashMap<Class<out Annotation>, Annotation?>? = fieldAnnotationCache[field]

		if (annotationMap == null) {
			annotationMap = HashMap()
			fieldAnnotationCache[field] = annotationMap
		}

		annotationMap[annotationClass] = annotation
	}
}
