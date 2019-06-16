package poetry.annotations

/**
 * Specify the JSON property (and optionally a child object) to map from
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MapFrom(
	/**
	 * @return The name of the property.
	 */
	val value: String
)
