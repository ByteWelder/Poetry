package poetry.test

import android.support.test.InstrumentationRegistry
import com.j256.ormlite.dao.Dao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import poetry.database.DatabaseHelper.Companion.releaseHelper
import poetry.json.JsonPathResolver
import poetry.json.JsonPersister
import poetry.test.data.getDatabaseHelper
import poetry.test.data.loadJsonObject
import poetry.test.data.models.Group
import poetry.test.data.models.User

class JsonTestCaseKotlin {

	@Test
	@Throws(Exception::class)
	fun testJsonMapper() {
		val context = InstrumentationRegistry.getContext()
		val helper = getDatabaseHelper(context)

		// Load JSON
		val json = loadJsonObject(context, R.raw.test)

		// Get child arrays from JSON
		val usersJson = JsonPathResolver.resolveArray(json, "users")
		val groupsJson = JsonPathResolver.resolveArray(json, "groups")

		// Persist arrays to database
		val persister = JsonPersister(helper.writableDatabase)
		persister.persistArray<Any>(User::class.java, usersJson)
		persister.persistArray<Any>(Group::class.java, groupsJson)

		val userDao = helper.getDao<Dao<User, Int>, User>(User::class.java)
		val groupDao = helper.getDao<Dao<Group, Int>, Group>(Group::class.java)

		val users = userDao.queryForAll()
		assertEquals(2, users.size.toLong())

		val groups = groupDao.queryForAll()
		assertEquals(3, groups.size.toLong())

		val user = userDao.queryForId(1)
		assertNotNull(user)
		assertEquals("John", user.getName())
		assertEquals(2, user.getTags().size.toLong())
		assertEquals("tag2", user.getTags().get(1))

		val group = groupDao.queryForId(2)
		assertNotNull(group)
		assertEquals("Group B", group.getName())

		releaseHelper()
	}
}