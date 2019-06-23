package poetry

import kotlin.reflect.KClass

class DatabaseConfiguration @JvmOverloads constructor(
	val modelVersion: Int,
	val modelClasses: Array<Class<*>>,
	val databaseName: String = DEFAULT_NAME
) {
	companion object {
		const val DEFAULT_NAME = "database"
	}
}

fun databaseConfigurationOf(dbName: String, vararg types: KClass<*>): DatabaseConfiguration {
	val classList = types.map { it.java }
	return DatabaseConfiguration(1, classList.toTypedArray(), dbName)
}

fun databaseConfigurationOf(dbName: String, vararg types: Class<*>): DatabaseConfiguration {
	return DatabaseConfiguration(1, arrayOf(*types), dbName)
}
