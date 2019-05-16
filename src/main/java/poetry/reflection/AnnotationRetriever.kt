package poetry.reflection

import poetry.annotations.Nullable
import java.lang.reflect.Field
import java.util.*

/**
 * AnnotationRetriever caches annotations to improve performance.
 *
 *
 * In a test with hundreds of objects, Field.getAnnotation() took over 50% CPU time.
 * When looking at the Android source code it shows that this is a fairly heavy method.
 * Considering that Poetry uses only a certain amount of model classes and fields, it
 * makes sense to cache this in memory.
 *
 *
 * Reference: http://grepcode.com/file/repo1.maven.org/maven2/org.robolectric/android-all/4.4_r1-robolectric-1/libcore/reflect/AnnotationAccess.java#AnnotationAccess.getDeclaredAnnotation%28java.lang.reflect.AnnotatedElement%2Cjava.lang.Class%29
 */
class AnnotationRetriever {
	private val fieldAnnotationCache = HashMap<Field, HashMap<Class<out Annotation>, Annotation?>>()
	private val classAnnotationCache = HashMap<Class<*>, HashMap<Class<out Annotation>, Annotation?>>()

	/**
	 * Retrieve a [Field] for a Field.
	 *
	 * @param field            the Field to search for the Annotation
	 * @param annotationClass  the annotation class to search for
	 * @param <AnnotationType> the annotation type to search for
	 * @return the found Annotation or null
	 */
	fun <AnnotationType : Annotation> getAnnotation(field: Field, annotationClass: Class<AnnotationType>): AnnotationType? {
		// Try to retrieve it from cache
		var annotation = getCachedAnnotation(field, annotationClass)

		// If not cached, try reflection
		if (annotation == null) {
			annotation = field.getAnnotation(annotationClass)

			// Null values are also cached because it will make the next failure quicker
			setCachedAnnotation(field, annotationClass, annotation)
		}

		return annotation as AnnotationType?
	}

	private fun getCachedAnnotation(field: Field, annotationClass: Class<out Annotation>): Annotation? {
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

	/**
	 * Retrieve a [Field] for a class.
	 *
	 * @param parentClass      the class to retrieve the annotation from
	 * @param annotationClass  the annotation type to search for
	 * @param <AnnotationType> the annotation type to search for
	 * @return the Annotation or null
	</AnnotationType> */
	fun <AnnotationType : Annotation> getAnnotation(parentClass: Class<*>, annotationClass: Class<AnnotationType>): AnnotationType? {
		// Try to retrieve it from cache
		var annotation = getCachedAnnotation(parentClass, annotationClass)

		// If not cached, try reflection
		if (annotation == null) {
			annotation = parentClass.getAnnotation(annotationClass)

			// Null values are also cached because it will make the next failure quicker
			setCachedAnnotation(parentClass, annotationClass, annotation)
		}

		return annotation as AnnotationType?
	}

	@Nullable
	private fun getCachedAnnotation(parentClass: Class<*>, annotationClass: Class<out Annotation>): Annotation? {
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
