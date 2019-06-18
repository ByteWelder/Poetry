package poetry.internal.reflection

import com.j256.ormlite.dao.ForeignCollection
import com.j256.ormlite.field.DatabaseField
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

// Reference: http://sourceforge.net/p/ormlite/code/HEAD/tree/ormlite-core/trunk/src/main/java/com/j256/ormlite/field/FieldType.java
private const val FOREIGN_ID_FIELD_SUFFIX = "_id"

/**
 * Get SQLite column name for a given Field.
 *
 * @param field the model's field
 * @return the SQLite column name
 */
internal fun FieldAnnotationRetriever.getColumnNameForField(field: Field): String {
	val databaseField = findAnnotation(field, DatabaseField::class.java)
			?: throw RuntimeException("DatabaseField annotation not found in ${field.declaringClass.name} for ${field.name}")
	return getColumnNameForField(field, databaseField)
}

/**
 * Get SQLite column name for a given Field.
 *
 * @param field the model's field
 * @param databaseField the DatabaseField annotation for the specified Field
 * @return the SQLite column name
 */
internal fun getColumnNameForField(field: Field, databaseField: DatabaseField) = when {
	databaseField.columnName.isNotEmpty() -> databaseField.columnName
	databaseField.isForeign() -> field.name + FOREIGN_ID_FIELD_SUFFIX
	else -> field.name
}

/**
 * Check if the provided DatabaseField is a foreign field.
 *
 * @return true if foreign() is true, foreignAutoRefresh() is true or foreignColumnName() is set to a non-empty string
 */
internal fun DatabaseField.isForeign() = foreign ||
		foreignAutoRefresh ||
		foreignColumnName.isNotEmpty()

/**
 * @return true if id() or generatedId() are true
 */
internal fun DatabaseField.isId() = id || generatedId

/**
 * Retrieves the generic type argument: the type that is held by the specified ForeignCollection Field
 *
 * @return the class
 * @throws RuntimeException when the Field is not a ForeignCollection
 */
fun Field.getForeignCollectionParameterType(): Class<*> {
	if (type != ForeignCollection::class.java) {
		throw RuntimeException(declaringClass.name + " declares the field \"$name\" which is not a ForeignCollection but is annotated by ForeignCollectionField")
	}

	val parameterizedType = genericType as ParameterizedType
	val childType = parameterizedType.actualTypeArguments[0]
	return childType as Class<*> // TODO: check conversion?
}

/**
 * Find a Field with a DatabaseField annotation that defines it as being an id column.
 * This function recursively checks all base classes.
 *
 * @param modelClass the class to find the ID field in
 * @return the Field or null
 */
internal fun FieldAnnotationRetriever.findIdField(modelClass: Class<*>): Field? {
	return findIdFieldWithoutInheritance(modelClass) ?: if (modelClass.superclass == null) {
		null
	} else {
		// Recursively check superclass
		findIdField(modelClass.superclass as Class<*>)
	}
}

internal fun FieldAnnotationRetriever.findIdFieldOrThrow(modelClass: Class<*>): Field {
	val field = findIdField(modelClass)
	if (field == null) {
		throw RuntimeException("Failed to find id field in ${modelClass.name}")
	} else {
		return field
	}
}

/**
 * Find a Field with a DatabaseField annotation that defines it as being an id column.
 * This function recursively checks only the specified modelClass. (not recursively)
 *
 * @param modelClass the class to find the ID field in
 * @return the Field or null
 */
private fun FieldAnnotationRetriever.findIdFieldWithoutInheritance(modelClass: Class<*>): Field? {
	return modelClass.declaredFields.firstOrNull { field ->
		val annotation = findAnnotation(field, DatabaseField::class.java)
		annotation != null && (annotation.id || annotation.generatedId)
	}
}

/**
 * Find a Field with a DatabaseField annotation that defines it as foreign.
 *
 * @param parentClass the class to search for the Field
 * @param findClass the field class to search for
 * @return a Field or null
 */
internal fun FieldAnnotationRetriever.findForeignField(parentClass: Class<*>, findClass: Class<*>): Field? {
	return findForeignFieldWithoutInheritance(parentClass, findClass)
			?: if (parentClass.superclass == null) {
				null
			} else {
				// Recursively check superclass
				findForeignField(parentClass.superclass as Class<*>, findClass)
			}
}

private fun FieldAnnotationRetriever.findForeignFieldWithoutInheritance(parentClass: Class<*>, findClass: Class<*>): Field? {
	return parentClass.declaredFields.firstOrNull { field ->
		val databaseField = findAnnotation(field, DatabaseField::class.java)
		databaseField != null &&
				databaseField.isForeign() &&
				findClass.isAssignableFrom(field.type)
	}
}