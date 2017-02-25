package poetry.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;

import poetry.annotations.Nullable;

/**
 * AnnotationRetriever caches annotations to improve performance.
 * <p>
 * In a test with hundreds of objects, Field.getAnnotation() took over 50% CPU time.
 * When looking at the Android source code it shows that this is a fairly heavy method.
 * Considering that Poetry uses only a certain amount of model classes and fields, it
 * makes sense to cache this in memory.
 * <p>
 * Reference: http://grepcode.com/file/repo1.maven.org/maven2/org.robolectric/android-all/4.4_r1-robolectric-1/libcore/reflect/AnnotationAccess.java#AnnotationAccess.getDeclaredAnnotation%28java.lang.reflect.AnnotatedElement%2Cjava.lang.Class%29
 */
public class AnnotationRetriever {
	private final HashMap<Field, HashMap<Class<? extends Annotation>, Annotation>> fieldAnnotationCache = new HashMap<>();
	private final HashMap<Class<?>, HashMap<Class<? extends Annotation>, Annotation>> classAnnotationCache = new HashMap<>();

	/**
	 * Retrieve a {@link Field} for a Field.
	 *
	 * @param field            the Field to search for the Annotation
	 * @param annotationClass  the annotation class to search for
	 * @param <AnnotationType> the annotation type to search for
	 * @return the found Annotation or null
	 */
	@Nullable
	public <AnnotationType extends Annotation> AnnotationType getAnnotation(Field field, Class<AnnotationType> annotationClass) {
		// Try to retrieve it from cache
		Annotation annotation = getCachedAnnotation(field, annotationClass);

		// If not cached, try reflection
		if (annotation == null) {
			annotation = field.getAnnotation(annotationClass);

			// Null values are also cached because it will make the next failure quicker
			setCachedAnnotation(field, annotationClass, annotation);
		}

		return (AnnotationType) annotation;
	}

	@Nullable
	private Annotation getCachedAnnotation(Field field, Class<? extends Annotation> annotationClass) {
		HashMap<Class<? extends Annotation>, Annotation> annotation_map = fieldAnnotationCache.get(field);
		return (annotation_map != null) ? annotation_map.get(annotationClass) : null;
	}

	private void setCachedAnnotation(Field field, Class<? extends Annotation> annotationClass, Annotation annotation) {
		HashMap<Class<? extends Annotation>, Annotation> annotation_map = fieldAnnotationCache.get(field);

		if (annotation_map == null) {
			annotation_map = new HashMap<>();
			fieldAnnotationCache.put(field, annotation_map);
		}

		annotation_map.put(annotationClass, annotation);
	}

	/**
	 * Retrieve a {@link Field} for a class.
	 *
	 * @param parentClass      the class to retrieve the annotation from
	 * @param annotationClass  the annotation type to search for
	 * @param <AnnotationType> the annotation type to search for
	 * @return the Annotation or null
	 */
	@Nullable
	public <AnnotationType extends Annotation> AnnotationType getAnnotation(Class<?> parentClass, Class<AnnotationType> annotationClass) {
		// Try to retrieve it from cache
		Annotation annotation = getCachedAnnotation(parentClass, annotationClass);

		// If not cached, try reflection
		if (annotation == null) {
			annotation = parentClass.getAnnotation(annotationClass);

			// Null values are also cached because it will make the next failure quicker
			setCachedAnnotation(parentClass, annotationClass, annotation);
		}

		return (AnnotationType) annotation;
	}

	@Nullable
	private Annotation getCachedAnnotation(Class<?> parentClass, Class<? extends Annotation> annotationClass) {
		HashMap<Class<? extends Annotation>, Annotation> annotation_map = classAnnotationCache.get(parentClass);

		return (annotation_map != null) ? annotation_map.get(annotationClass) : null;
	}

	private void setCachedAnnotation(Class<?> parentClass, Class<? extends Annotation> annotationClass, Annotation annotation) {
		HashMap<Class<? extends Annotation>, Annotation> annotation_map = classAnnotationCache.get(parentClass);

		if (annotation_map == null) {
			annotation_map = new HashMap<>();
			classAnnotationCache.put(parentClass, annotation_map);
		}

		annotation_map.put(annotationClass, annotation);
	}
}
