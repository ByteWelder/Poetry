package poetry.android.test.java;

import com.j256.ormlite.dao.Dao;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import poetry.android.test.internal.DatabaseHelperRule;
import poetry.android.test.java.data.DatabaseHelper;
import poetry.android.test.java.data.models.Group;
import poetry.android.test.java.data.models.User;
import poetry.JsonPathResolver;
import poetry.JsonPersister;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static poetry.android.test.internal.JsonLoaderKt.loadJsonObject;

/**
 * This test case is to verify Java syntax compatibility.
 */
public class JsonTestCaseJava
{
	@Rule
	public DatabaseHelperRule<DatabaseHelper> helperRule = new DatabaseHelperRule<>(DatabaseHelper.class);

	@Test
	public void testJsonMapper() throws Exception
	{
		// Load JSON
		JSONObject json = loadJsonObject(poetry.android.test.R.raw.test);

		// Get child arrays from JSON
		JSONArray usersJson = JsonPathResolver.resolveArray(json, "users");
		JSONArray groupsJson = JsonPathResolver.resolveArray(json, "groups");

		// Persist arrays to database
		JsonPersister persister = new JsonPersister(helperRule.helper.getWritableDatabase());
		persister.persistArray(User.class, usersJson);
		persister.persistArray(Group.class, groupsJson);

		Dao<User, Integer> userDao = helperRule.helper.getDao(User.class);
		Dao<Group, Integer> GroupDao = helperRule.helper.getDao(Group.class);

		List<User> users = userDao.queryForAll();
		assertEquals(2, users.size());

		List<Group> groups = GroupDao.queryForAll();
		assertEquals(3, groups.size());

		User user = userDao.queryForId(1);
		assertNotNull(user);
		assertEquals("John", user.getName());
		assertEquals(2, user.getTags().size());
		assertEquals("tag2", user.getTags().get(1));

		Group group = GroupDao.queryForId(2);
		assertNotNull(group);
		assertEquals("Group B", group.getName());
	}
}
