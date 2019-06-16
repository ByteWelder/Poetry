package poetry

class DatabaseConfiguration @JvmOverloads constructor(
	val modelVersion: Int,
	val modelClasses: Array<Class<*>>,
	val databaseName: String = DEFAULT_NAME
) {
	companion object {
		const val DEFAULT_NAME = "database"
	}
}

