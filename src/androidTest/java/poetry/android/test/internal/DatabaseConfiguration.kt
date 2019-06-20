package poetry.android.test.internal

import poetry.DatabaseConfiguration
import kotlin.reflect.KClass

fun databaseConfigurationOf(dbName: String, vararg types: KClass<*>): DatabaseConfiguration {
	val classList = types.map { it.java }
	return DatabaseConfiguration(1, classList.toTypedArray(), dbName)
}

fun databaseConfigurationOf(dbName: String, vararg types: Class<*>): DatabaseConfiguration {
	return DatabaseConfiguration(1, arrayOf(*types), dbName)
}
