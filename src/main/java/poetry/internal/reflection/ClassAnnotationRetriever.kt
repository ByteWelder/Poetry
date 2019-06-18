package poetry.internal.reflection

import java.lang.reflect.Field

/**
 * AnnotationRetriever caches annotations to improve performance.
 * Fetching annotations is a CPU-heavy operation, so this classes caches the results.
 */
internal class ClassAnnotationRetriever {
	private val classAnnotationCache = HashMap<Class<*>, HashMap<Class<out Annotation>, Annotation?>>()

	/**
	 * Retrieve a [Field] for a class.
	 *
	 * @param parentClass      the class to retrieve the annotation from
	 * @param annotationClass  the annotation type to search for
	 * @param <AnnotationType> the annotation type to search for
	 * @return the Annotation or null
	</AnnotationType> */
	fun <AnnotationType : Annotation> findAnnotation(parentClass: Class<*>, annotationClass: Class<AnnotationType>): AnnotationType? {
		// Try to retrieve it from cache
		var annotation = findCachedAnnotation(parentClass, annotationClass)

		// If not cached, try reflection
		if (annotation == null) {
			annotation = parentClass.getAnnotation(annotationClass)

			// Null values are also cached because it will make the next failure quicker
			setCachedAnnotation(parentClass, annotationClass, annotation)
		}

		return annotation as AnnotationType?
	}

	private fun findCachedAnnotation(parentClass: Class<*>, annotationClass: Class<out Annotation>): Annotation? {
		val annotationMap = classAnnotationCache[parentClass]

		return if (annotationMap != null) annotationMap[annotationClass] else null
	}

	private fun setCachedAnnotation(parentClass: Class<*>, annotationClass: Class<out Annotation>, annotation: Annotation?) {
		var annotationMap: HashMap<Class<out Annotation>, Annotation?>? = classAnnotationCache[parentClass]

		if (annotationMap == null) {
			annotationMap = HashMap()
			classAnnotationCache[parentClass] = annotationMap
		}

		annotationMap[annotationClass] = annotation
	}
}
