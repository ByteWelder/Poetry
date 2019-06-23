package poetry.android.test

import android.content.Context
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import poetry.android.test.internal.DatabaseHelperRule
import poetry.android.test.internal.test
import poetry.android.test.internal.toJsonObject
import poetry.databaseConfigurationOf

class NoDatabaseTableModel

class NoDatabaseTableHelper(context: Context) : poetry.DatabaseHelper(
	context,
	databaseConfigurationOf(
		"StringIdTest",
		NoDatabaseTableModel::class
	)
)

class NoDatabaseTableTest {
	@get:Rule
	val helperRule = DatabaseHelperRule(BasicHelper::class.java)

	@get:Rule
	val expectedException = ExpectedException.none()!!

	@Test
	fun missing_annotation_should_throw_exception() {
		expectedException.expect(IllegalStateException::class.java)
		expectedException.expectMessage("DatabaseTable annotation not found for poetry.android.test.NoDatabaseTableHelper")

		"{}".toJsonObject().test(helperRule.helper, NoDatabaseTableHelper::class) { }
	}
}

