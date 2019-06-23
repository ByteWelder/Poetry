package poetry

object PoetryOptions {
	/**
	 * When a foreign collection is imported (one-to-many relationship),
	 * the normal behavior is that the old children are deleted.
	 * This options allows you to disable that behavior.
	 */
	const val DISABLE_FOREIGN_COLLECTION_CLEANUP = 0x0001
	/**
	 * Don't display warnings when JSON attributes are not annotated as a field in an object.
	 */
	const val DISABLE_IGNORED_ATTRIBUTES_WARNING = 0x0002

	const val DEFAULT = 0

	/**
	 * Check if an option is enabled
	 *
	 * @param optionsSet  the compound of option values (combined with logical OR operator)
	 * @param optionCheck one or more options to check (combined with logical OR operator)
	 * @return true when all the options from optionCheck are contained in optionsSet
	 */
	internal fun isEnabled(optionsSet: Int, optionCheck: Int): Boolean = optionsSet and optionCheck == optionCheck
}
