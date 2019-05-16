package poetry.database

class DatabaseConfiguration @JvmOverloads constructor(
	val modelVersion: Int,
	val modelClasses: Array<Class<*>>,
	val databaseName: String = DEFAULT_NAME
) {
	companion object {
		val DEFAULT_NAME = "database"
	}
}

