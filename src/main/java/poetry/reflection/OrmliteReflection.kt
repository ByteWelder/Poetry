package poetry.reflection

import com.j256.ormlite.dao.ForeignCollection
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

/**
 * A set of reflection utilities for OrmLite to process and retrieve fields and annotations.
 */
// TODO: most of these calls should be cached as is done in AnnotationRetriever/FieldRetriever
object OrmliteReflection {
	// Reference: http://sourceforge.net/p/ormlite/code/HEAD/tree/ormlite-core/trunk/src/main/java/com/j256/ormlite/field/FieldType.java
	private const val FOREIGN_ID_FIELD_SUFFIX = "_id"

	/**
	 * Get the SQLite table name for an OrmLite model.
	 *
	 * @param annotationRetriever the annotation retriever that caches the annotations
	 * @param modelClass          an OrmLite model class annotated with [DatabaseTable]
	 * @return the SQLite table name
	 */
	fun getTableName(annotationRetriever: AnnotationRetriever, modelClass: Class<*>): String {
		val tableAnnotation = annotationRetriever.getAnnotation(modelClass, DatabaseTable::class.java)
				?: throw RuntimeException("DatabaseTable annotation not found for " + modelClass.name)

		return getTableName(modelClass, tableAnnotation)
	}

	/**
	 * Get the SQLite table name for an OrmLite model.
	 *
	 * @param modelClass      an OrmLite model class annotated with [DatabaseTable]
	 * @param tableAnnotation the annotation to process
	 * @return the SQLite table name
	 */
	fun getTableName(modelClass: Class<*>, tableAnnotation: DatabaseTable): String {
		return if (!tableAnnotation.tableName.isEmpty()) tableAnnotation.tableName else modelClass.simpleName
	}

	/**
	 * Get SQLite column name for a given Field.
	 *
	 * @param annotationRetriever the annotation retriever that caches the annotations
	 * @param field               the model's field
	 * @return the SQLite column name
	 */
	fun getFieldName(annotationRetriever: AnnotationRetriever, field: Field): String {
		val databaseField = annotationRetriever.getAnnotation(field, DatabaseField::class.java)
				?: throw RuntimeException("DatabaseField annotation not found in " + field.declaringClass.name + " for " + field.name)

		return getFieldName(field, databaseField)
	}

	/**
	 * Get SQLite column name for a given Field.
	 *
	 * @param field         the model's field
	 * @param databaseField the DatabaseField annotation for the specified Field
	 * @return the SQLite column name
	 */
	fun getFieldName(field: Field, databaseField: DatabaseField): String {
		return if (!databaseField.columnName.isEmpty()) {
			databaseField.columnName
		} else if (OrmliteReflection.isForeign(databaseField)) {
			field.name + FOREIGN_ID_FIELD_SUFFIX
		} else {
			field.name
		}
	}

	/**
	 * Check if the provided DatabaseField is a foreign field.
	 *
	 * @param databaseField the annotation to check
	 * @return true if foreign() is true, foreignAutoRefresh() is true or foreignColumnName() is set to a non-empty string
	 */
	fun isForeign(databaseField: DatabaseField): Boolean {
		return (databaseField.foreign
				|| databaseField.foreignAutoRefresh
				|| !databaseField.foreignColumnName.isEmpty())
	}

	/**
	 * Check if the DatabaseField is an ID field.
	 *
	 * @param databaseField the annotation to check
	 * @return true if id() or generatedId() are true
	 */
	fun isId(databaseField: DatabaseField): Boolean {
		return databaseField.id || databaseField.generatedId
	}

	/**
	 * Retrieves the generic type argument: the type that is held by the specified ForeignCollection Field
	 *
	 * @param field a [Field] that holds the type [com.j256.ormlite.dao.ForeignCollection]
	 * @return the class
	 * @throws RuntimeException when the Field is not a ForeignCollection
	 */
	fun getForeignCollectionParameterType(field: Field): Class<*> {
		if (field.type != ForeignCollection::class.java) {
			throw RuntimeException(field.declaringClass.name + " declares the field \"" + field.name + "\" which is not a ForeignCollection but is annotated by ForeignCollectionField")
		}

		val type = field.genericType
		val parameterizedType = type as ParameterizedType
		val childType = parameterizedType.actualTypeArguments[0]

		return childType as Class<*> // TODO: check conversion?
	}

	/**
	 * Find a Field with a DatabaseField annotation that defines it as being an id column.
	 *
	 * @param annotationRetriever the annotation retriever that caches the annotations
	 * @param modelClass          the class to find the ID field in
	 * @return the Field or null
	 */
	fun findIdField(annotationRetriever: AnnotationRetriever, modelClass: Class<*>): Field? {
		for (field in modelClass.declaredFields) {
			val databaseField = annotationRetriever.getAnnotation(field, DatabaseField::class.java)
					?: continue

			if (databaseField.generatedId || databaseField.id) {
				return field
			}
		}

		return if (modelClass.superclass == null) {
			null
		} else {
			// Recursively check superclass
			findIdField(annotationRetriever, modelClass.superclass as Class<*>)
		}
	}

	/**
	 * Find a Field with a DatabaseField annotation that defines it as foreign.
	 *
	 * @param annotationRetriever the annotation retriever that caches the annotations
	 * @param parentClass         the class to search for the Field
	 * @param findClass           the field class to search for
	 * @return a Field or null
	 */
	fun findForeignField(annotationRetriever: AnnotationRetriever, parentClass: Class<*>, findClass: Class<*>): Field? {
		for (field in parentClass.declaredFields) {
			val databaseField = annotationRetriever.getAnnotation(field, DatabaseField::class.java)
			if (databaseField != null
					&& isForeign(databaseField)
					&& findClass.isAssignableFrom(field.type)) {
				return field
			}
		}

		return if (parentClass.superclass == null) {
			null
		} else {
			// Recursively check superclass
			findForeignField(annotationRetriever, parentClass.superclass as Class<*>, findClass)
		}
	}
}
