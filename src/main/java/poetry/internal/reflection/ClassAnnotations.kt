package poetry.internal.reflection

import com.j256.ormlite.table.DatabaseTable

/**
 * Get the SQLite table name for an OrmLite model.
 *
 * @param modelClass an OrmLite model class annotated with [DatabaseTable]
 * @return the SQLite table name
 */
internal fun ClassAnnotationRetriever.getTableNameOrThrow(modelClass: Class<*>): String {
	val tableAnnotation = findAnnotation(modelClass, DatabaseTable::class.java)
			?: throw RuntimeException("DatabaseTable annotation not found for ${modelClass.name}")
	return getTableName(modelClass, tableAnnotation)
}

/**
 * Get the SQLite table name for an OrmLite model.
 *
 * @param modelClass an OrmLite model class annotated with [DatabaseTable]
 * @param tableAnnotation the annotation to process
 * @return the SQLite table name
 */
internal fun getTableName(modelClass: Class<*>, tableAnnotation: DatabaseTable): String {
	return if (tableAnnotation.tableName.isNotEmpty()) tableAnnotation.tableName else modelClass.simpleName
}
