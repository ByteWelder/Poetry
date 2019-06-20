package poetry.internal.reflection

import java.lang.reflect.Field

internal typealias FieldToAnnotationMap = HashMap<Field, AnnotationMap>

/**
 * AnnotationRetriever caches annotations to improve performance.
 * Fetching annotations is a CPU-heavy operation, so this classes caches the results.
 */
internal class FieldAnnotationRetriever(
	private val fieldAnnotationCache: FieldToAnnotationMap = FieldToAnnotationMap()
) {

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

		return annotation
	}

	fun <AnnotationType : Annotation> findAnnotationOrThrow(field: Field, annotationClass: Class<AnnotationType>): AnnotationType {
		return checkNotNull(findAnnotation(field, annotationClass)) {
			"${annotationClass.name} annotation not found on field ${field.name}"
		}
	}

	private fun <AnnotationType : Annotation> findCachedAnnotation(field: Field, annotationClass: Class<out AnnotationType>): AnnotationType? {
		val annotationMap = fieldAnnotationCache[field]
		return if (annotationMap != null) {
			@Suppress("UNCHECKED_CAST")
			annotationMap[annotationClass] as AnnotationType?
		} else {
			null
		}
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
