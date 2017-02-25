package poetry.reflection;

import java.lang.reflect.Field;
import java.util.HashMap;

import poetry.annotations.Nullable;
import poetry.json.annotations.MapFrom;

/**
 * FieldRetriever caches {@link Field} objects to improve performance.
 * <p>
 * In a test with hundreds of objects, OrmliteReflection.findField() took over 50% CPU time.
 * When looking at the Android source code it shows that this is a fairly heavy method.
 * Considering that Poetry uses only a certain amount of model classes and fields, it
 * makes sense to cache this in memory.
 * <p>
 * Reference: http://grepcode.com/file/repo1.maven.org/maven2/org.robolectric/android-all/4.4_r1-robolectric-1/libcore/reflect/AnnotationAccess.java#AnnotationAccess.getDeclaredAnnotation%28java.lang.reflect.AnnotatedElement%2Cjava.lang.Class%29
 */
public class FieldRetriever {
	// Maps: model class -> json type -> Field instance
	private final HashMap<Class<?>, HashMap<String, Field>> fieldJsonCache = new HashMap<>();
	// Maps: model class -> field type -> Field instance
	private final HashMap<Class<?>, HashMap<Class<?>, Field>> fieldTypeCache = new HashMap<>();

	/**
	 * Retrieve a {@link Field} for a model.
	 *
	 * @param modelClass the class to search for the Field
	 * @param jsonKey    the json key that is mapped to the field
	 * @return the found Field or null
	 */
	@Nullable
	public Field getField(Class<?> modelClass, String jsonKey) {
		// Try to retrieve it from cache
		Field field = getCachedField(modelClass, jsonKey);

		// If not cached, try reflection
		if (field == null) {
			field = findField(modelClass, jsonKey);

			// Null values are also cached because it will make the next failure quicker
			setCachedField(modelClass, jsonKey, field);
		}

		return field;
	}

	@Nullable
	private Field getCachedField(Class<?> classObject, String fieldName) {
		HashMap<String, Field> field_map = fieldJsonCache.get(classObject);
		return (field_map != null) ? field_map.get(fieldName) : null;
	}

	private void setCachedField(Class<?> classObject, String fieldName, Field field) {
		HashMap<String, Field> field_map = fieldJsonCache.get(classObject);

		if (field_map == null) {
			field_map = new HashMap<>();
			fieldJsonCache.put(classObject, field_map);
		}

		field_map.put(fieldName, field);
	}

	/**
	 * Find a field in a model, providing its JSON attribute name
	 *
	 * @param modelClass the model class
	 * @param name       the name of the JSON field
	 * @return the Field that is found or null
	 */
	@Nullable
	private	Field findField(Class<?> modelClass, String name) {
		// Check all the fields in the model
		for (Field field : modelClass.getDeclaredFields()) {
			// Direct match?
			if (field.getName().equals(name)) {
				return field;
			}

			// MapFrom-annotated match?
			MapFrom map_from = field.getAnnotation(MapFrom.class);

			if (map_from != null && name.equals(map_from.value())) {
				return field;
			}
		}

		if (modelClass.getSuperclass() == null) {
			return null;
		}

		// Recursively check superclass
		return findField(modelClass.getSuperclass(), name);
	}

	/**
	 * Retrieve a {@link Field} for a model.
	 *
	 * @param parentClass the class to search for the Field
	 * @param fieldClass  the Field class to search for
	 * @return the found Field or null
	 */
	@Nullable
	public Field getFirstFieldOfType(Class<?> parentClass, Class<?> fieldClass) {
		// Try to retrieve it from cache
		Field field = getCachedField(parentClass, fieldClass);

		// If not cached, try reflection
		if (field == null) {
			field = findFirstFieldOfType(parentClass, fieldClass);

			// Null values are also cached because it will make the next failure quicker
			setCachedField(parentClass, fieldClass, field);
		}

		return field;
	}

	@Nullable
	private Field getCachedField(Class<?> parentClass, Class<?> fieldClass) {
		HashMap<Class<?>, Field> field_map = fieldTypeCache.get(parentClass);
		return (field_map != null) ? field_map.get(fieldClass) : null;
	}

	private void setCachedField(Class<?> parentClass, Class<?> fieldClass, Field field) {
		HashMap<Class<?>, Field> field_map = fieldTypeCache.get(parentClass);

		if (field_map == null) {
			field_map = new HashMap<>();
			fieldTypeCache.put(parentClass, field_map);
		}

		field_map.put(fieldClass, field);
	}

	/**
	 * Finds a field of a certain type in a given parent type
	 *
	 * @param parentClass the parent class that holds the field
	 * @param fieldClass  the field class to search for
	 * @return the found first Field of the specified type or null
	 */
	@Nullable
	public static Field findFirstFieldOfType(Class<?> parentClass, Class<?> fieldClass) {
		for (Field field : parentClass.getDeclaredFields()) {
			if (field.getType().equals(fieldClass)) {
				return field;
			}
		}

		if (parentClass.getSuperclass() == null) {
			return null;
		}

		// Recursively check superclass
		return findFirstFieldOfType(parentClass.getSuperclass(), fieldClass);
	}
}
