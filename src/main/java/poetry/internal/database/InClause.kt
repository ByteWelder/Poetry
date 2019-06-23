package poetry.internal.database

class InClause(val selector: String, val values: Array<String?>)

fun inClauseOf(values: List<Any?>): InClause {
	val inClauseIdValues = arrayOfNulls<String>(values.size)
	val inClauseBuilder = StringBuilder(4 + (values.size * 2))

	inClauseBuilder.append("IN (")
	for (index in values.indices) {
		inClauseIdValues[index] = QueryUtils.parseAttribute(values[index])
		inClauseBuilder.append('?')
		if (index != values.lastIndex) {
			inClauseBuilder.append(',')
		}
	}
	inClauseBuilder.append(")")

	return InClause(inClauseBuilder.toString(), inClauseIdValues)
}
