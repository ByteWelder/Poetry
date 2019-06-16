package poetry.android.test.kotlin

import com.j256.ormlite.dao.Dao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import poetry.android.test.R
import poetry.android.test.internal.DatabaseHelperRule
import poetry.android.test.internal.loadJsonObject
import poetry.android.test.kotlin.data.DatabaseHelper
import poetry.android.test.kotlin.data.models.Group
import poetry.android.test.kotlin.data.models.User
import poetry.JsonPathResolver
import poetry.JsonPersister

class JsonTestCaseKotlin {
	@get:Rule
	val helperRule = DatabaseHelperRule(DatabaseHelper::class.java)

	@Test
	@Throws(Exception::class)
	fun testJsonMapper() {
		// Load JSON
		val json = loadJsonObject(R.raw.test)

		// Get child arrays from JSON
		val usersJson = JsonPathResolver.resolveArray(json, "users")
		val groupsJson = JsonPathResolver.resolveArray(json, "groups")

		// Persist arrays to database
		val persister = JsonPersister(helperRule.helper.writableDatabase)
		persister.persistArray<Any>(User::class.java, usersJson)
		persister.persistArray<Any>(Group::class.java, groupsJson)

		val userDao = helperRule.helper.getDao<Dao<User, Int>, User>(User::class.java)
		val groupDao = helperRule.helper.getDao<Dao<Group, Int>, Group>(Group::class.java)

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