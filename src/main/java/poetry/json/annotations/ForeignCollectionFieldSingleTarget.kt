package poetry.json.annotations

/**
 * Specifies that the foreign collection is one of base types, e.g. an array of String objects.
 * This annotation allows you to specify the name of the field where that base type is stored.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ForeignCollectionFieldSingleTarget(
	val targetField: String // target database column name
)
