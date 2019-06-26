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
import poetry.android.test.internal.toJsonArray
import poetry.annotations.ManyToManyField
import poetry.databaseConfigurationOf

@DatabaseTable
class User(
	@DatabaseField(id = true)
	var id: Long = 0,

	@ForeignCollectionField(eager = true, maxEagerLevel = 3)
	@ManyToManyField(targetType = Group::class)
	var groups: ForeignCollection<UserGroup>? = null
)

@DatabaseTable
class Group(
	@DatabaseField(id = true)
	var id: Long = 0,

	@DatabaseField
	var name: String = ""
)

@DatabaseTable
class UserGroup(
	@DatabaseField(id = true)
	var id: Long = 0,

	@DatabaseField(foreign = true)
	val user: User,

	@DatabaseField(foreign = true, foreignAutoRefresh = true)
	val group: Group
) {
	internal constructor(): this(0, User(), Group())
}

class ManyToManyHelper(context: Context) : poetry.DatabaseHelper(
	context,
	databaseConfigurationOf(
		"ManyToManyTest",
		User::class,
		Group::class,
		UserGroup::class
	)
)

class ManyToManyTest {
	@get:Rule
	val helperRule = DatabaseHelperRule(ManyToManyHelper::class.java)

	@get:Rule
	val expectedException = ExpectedException.none()!!

	@Test
	fun objects_stored_then_retrieved() {
		"""
		[
			{
				"id" : 1,
				"groups" : [
					{
						"id" : 1,
						"name" : "Group 1"
					},
					{
						"id" : 2,
						"name" : "Group 2"
					}
				]
			},
			{
				"id" : 2,
				"groups" : [
					{
						"id" : 1,
						"name" : "Group 1"
					}
				]
			}
		]""".toJsonArray().test(
			helperRule.helper,
			User::class
		) { users ->
			Assert.assertEquals(2, users.size)

			val firstUser = users[0]
			Assert.assertEquals(2, firstUser.groups?.size)
			val firstUserGroups = checkNotNull(firstUser.groups).toList()
			Assert.assertEquals(1, firstUserGroups[0].group.id)
			Assert.assertEquals("Group 1", firstUserGroups[0].group.name)
			Assert.assertEquals(2, firstUserGroups[1].group.id)
			Assert.assertEquals("Group 2", firstUserGroups[1].group.name)

			val secondUser = users[1]
			Assert.assertEquals(1, secondUser.groups?.size)
			val secondUserGroups = checkNotNull(secondUser.groups).toList()
			Assert.assertEquals(1, secondUserGroups[0].group.id)
			Assert.assertEquals("Group 1", secondUserGroups[0].group.name)
		}
	}
}