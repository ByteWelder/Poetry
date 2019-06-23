package poetry.android.test

import android.content.Context
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import poetry.android.test.internal.DatabaseHelperRule
import poetry.android.test.internal.test
import poetry.android.test.internal.toJsonObject
import poetry.databaseConfigurationOf

@DatabaseTable
class StringIdModel(
	@DatabaseField(id = true)
	var id: String
) {
	internal constructor(): this("")
}

class StringIdHelper(context: Context) : poetry.DatabaseHelper(
	context,
	databaseConfigurationOf(
		"StringIdTest",
		StringIdModel::class
	)
)

class StringIdTest {
	@get:Rule
	val helperRule = DatabaseHelperRule(StringIdHelper::class.java)

	@Test
	fun id_value_should_be_persisted_properly() {
		"""{ "id": "idValue" } """.toJsonObject().test(
			helperRule.helper,
			StringIdModel::class
		) { model ->
			assertEquals("idValue", model.id)
		}
	}
}

