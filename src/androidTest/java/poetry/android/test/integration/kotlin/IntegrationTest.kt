package poetry.android.test.integration.kotlin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import poetry.JsonPersister
import poetry.android.test.R
import poetry.android.test.integration.kotlin.models.Group
import poetry.android.test.integration.kotlin.models.User
import poetry.android.test.internal.DatabaseHelperRule
import poetry.android.test.internal.toJsonObject

class IntegrationTest {
	@get:Rule
	val helperRule = DatabaseHelperRule(DatabaseHelper::class.java)

	@Test
	@Throws(Exception::class)
	fun testJsonMapper() {
		// Load JSON
		val json = R.raw.test.toJsonObject()

		// Get child arrays from JSON
		val usersJson = json.getJSONArray("users")
		val groupsJson = json.getJSONArray("groups")

		// Persist arrays to database
		val persister = JsonPersister(helperRule.helper.writableDatabase)
		persister.persistArray(User::class.java, usersJson)
		persister.persistArray(Group::class.java, groupsJson)

		val userDao = helperRule.helper.getDao<User, Int>(User::class)
		val groupDao = helperRule.helper.getDao<Group, Int>(Group::class)

		val users = userDao.queryForAll()
		assertEquals(2, users.size.toLong())

		val groups = groupDao.queryForAll()
		assertEquals(3, groups.size.toLong())

		val user = userDao.queryForId(1)
		assertNotNull(user)
		assertEquals("John", user.name)
		assertEquals(2, user.tagsAsList.size.toLong())
		assertEquals("tag2", user.tagsAsList[1])

		val group = groupDao.queryForId(2)
		assertNotNull(group)
		assertEquals("Group B", group.name)
	}
}