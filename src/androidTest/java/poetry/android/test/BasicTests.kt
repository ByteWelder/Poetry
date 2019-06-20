package poetry.android.test

import android.content.Context
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import poetry.android.test.internal.DatabaseHelperRule
import poetry.android.test.internal.databaseConfigurationOf
import poetry.android.test.internal.test
import poetry.android.test.internal.toJsonObject
import poetry.annotations.MapFrom

@DatabaseTable
class BasicModel(
	@DatabaseField(id = true, columnName = "_id")
	@MapFrom("theId")
	var id: Long,

	@DatabaseField(columnName = "_value")
	@MapFrom("theValue")
	var value: String
) {
	internal constructor(): this(0, "")
}

class BasicHelper(context: Context) : poetry.DatabaseHelper(
	context,
	databaseConfigurationOf(
		"StringIdTest",
			BasicModel::class
	)
)

class BasicTest {
	@get:Rule
	val helperRule = DatabaseHelperRule(BasicHelper::class.java)

	@get:Rule
	val expectedException = ExpectedException.none()!!

	@Test
	fun basic_persistence_should_work() {
		"""
		{
			"theId": 123,
			"theValue": "something"
		}
		""".toJsonObject().test(
				helperRule.helper,
				BasicModel::class
		) { model ->
			assertEquals(123, model.id)
			assertEquals("something", model.value)
		}
	}

	@Test
	fun wrong_id_should_throw_exception() {
		expectedException.expect(JSONException::class.java)
		expectedException.expectMessage("Value stringInsteadOfLong at theId of type java.lang.String cannot be converted to long")

		"""
		{
			"theId": "stringInsteadOfLong",
			"theValue": "anything"
		}
		""".toJsonObject().test(
			helperRule.helper,
				BasicModel::class
		) { model ->
			assertEquals("idValue", model.id)
			assertEquals("value", model.value)
		}
	}
}

