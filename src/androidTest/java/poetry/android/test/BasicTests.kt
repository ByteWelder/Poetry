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
import poetry.android.test.internal.test
import poetry.android.test.internal.toJsonObject
import poetry.annotations.MapFrom
import poetry.databaseConfigurationOf

@DatabaseTable
class SimpleModel(
	@DatabaseField(id = true)
	var id: Long,

	@DatabaseField
	var value: String
) {
	internal constructor(): this(0, "")
}

@DatabaseTable
class ModelWithKeyNamesChanged(
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
		SimpleModel::class,
		ModelWithKeyNamesChanged::class
	)
)

class BasicTest {
	@get:Rule
	val helperRule = DatabaseHelperRule(BasicHelper::class.java)

	@get:Rule
	val expectedException = ExpectedException.none()!!

	@Test
	fun object_stored_then_retrieved() {
		"""
		{
			"id": 123,
			"value": "something"
		}
		""".toJsonObject().test(
				helperRule.helper,
				SimpleModel::class
		) { model ->
			assertEquals(123, model.id)
			assertEquals("something", model.value)
		}
	}

	@Test
	fun object_with_key_name_overridden_sstored_then_retrieved() {
		"""
		{
			"theId": 123,
			"theValue": "something"
		}
		""".toJsonObject().test(
				helperRule.helper,
				ModelWithKeyNamesChanged::class
		) { model ->
			assertEquals(123, model.id)
			assertEquals("something", model.value)
		}
	}

	@Test
	fun wrong_id_should_throw_exception() {
		expectedException.expect(JSONException::class.java)
		expectedException.expectMessage("Value stringInsteadOfLong at id of type java.lang.String cannot be converted to long")

		"""
		{
			"id": "stringInsteadOfLong",
			"value": "anything"
		}
		""".toJsonObject().test(
			helperRule.helper,
			SimpleModel::class
		) { }
	}
}

