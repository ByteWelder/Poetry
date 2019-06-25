package poetry.android.test.integration.kotlin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import poetry.Poetry
import poetry.android.test.R
import poetry.android.test.integration.kotlin.models.Group
import poetry.android.test.integration.kotlin.models.User
import poetry.android.test.internal.DatabaseHelperRule
import poetry.android.test.internal.toJsonObject
import poetry.toListOrEmptyList

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
		val persister = Poetry(helperRule.helper.writableDatabase)
		persister.writeArray(User::class.java, usersJson)
		persister.writeArray(Group::class.java, groupsJson)

		val userDao = helperRule.helper.getDao<User, Int>(User::class)
		val groupDao = helperRule.helper.getDao<Group, Int>(Group::class)

		val users = userDao.queryForAll()
		assertEquals(2, users.size.toLong())

		val groups = groupDao.queryForAll()
		assertEquals(3, groups.size.toLong())

		val user = userDao.queryForId(1)
		assertNotNull(user)
		assertEquals("John", user.name)
		val tagList = user.tags.toListOrEmptyList()
		assertEquals(2, tagList.size.toLong())
		assertEquals("tag1", tagList[0].tag)
		assertEquals("tag2", tagList[1].tag)

		val group = groupDao.queryForId(2)
		assertNotNull(group)
		assertEquals("Group B", group.name)
	}
}