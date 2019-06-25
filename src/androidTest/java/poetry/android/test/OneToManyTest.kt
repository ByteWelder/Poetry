package poetry.android.test

import android.content.Context
import com.j256.ormlite.dao.ForeignCollection
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.table.DatabaseTable
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import poetry.android.test.internal.DatabaseHelperRule
import poetry.android.test.internal.test
import poetry.android.test.internal.toJsonObject
import poetry.toListOrEmptyList
import poetry.databaseConfigurationOf

@DatabaseTable
class Message(
	@DatabaseField(id = true)
	var id: Long = 0,

	@DatabaseField
	var text: String = "",

	@ForeignCollectionField(eager = true)
	var tags: ForeignCollection<Tag>? = null
)

@DatabaseTable
class Tag(
	@DatabaseField(id = true)
	var id: Long = 0,

	@DatabaseField
	var name: String = "",

	@DatabaseField(foreign = true, foreignAutoRefresh = true)
	var message: Message? = null
)

class OneToManyHelper(context: Context) : poetry.DatabaseHelper(
	context,
	databaseConfigurationOf(
		"OneToManyTest",
		Message::class,
		Tag::class
	)
)

class OneToManyTest {
	@get:Rule
	val helperRule = DatabaseHelperRule(OneToManyHelper::class.java)

	@get:Rule
	val expectedException = ExpectedException.none()!!

	@Test
	fun object_stored_then_retrieved() {
		"""
		{
			"id": 123,
			"text": "anything",
			"tags": [
				{
					"id": 1,
					"name": "one"
				},
				{
					"id": 2,
					"name": "two"
				}
			]
		}
		""".toJsonObject().test(
			helperRule.helper,
			Message::class
		) { model ->
			Assert.assertEquals(123, model.id)
			Assert.assertEquals("anything", model.text)
			Assert.assertEquals(2, model.tags.toListOrEmptyList().size)
		}
	}
}