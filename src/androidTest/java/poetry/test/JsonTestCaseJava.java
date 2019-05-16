package poetry.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.j256.ormlite.dao.Dao;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import poetry.json.JsonPathResolver;
import poetry.json.JsonPersister;
import poetry.test.data.DatabaseHelper;
import poetry.test.data.models.Group;
import poetry.test.data.models.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static poetry.test.data.DatabaseHelperKt.getDatabaseHelper;
import static poetry.test.data.JsonLoaderKt.loadJsonObject;

/**
 * This test case is to verify Java syntax compatibility.
 */
public class JsonTestCaseJava
{
	@Test
	public void testJsonMapper() throws Exception
	{
		Context context = InstrumentationRegistry.getContext();
		DatabaseHelper helper = getDatabaseHelper(context);

		// Load JSON
		JSONObject json = loadJsonObject(context, R.raw.test);

		// Get child arrays from JSON
		JSONArray usersJson = JsonPathResolver.resolveArray(json, "users");
		JSONArray groupsJson = JsonPathResolver.resolveArray(json, "groups");

		// Persist arrays to database
		JsonPersister persister = new JsonPersister(helper.getWritableDatabase());
		persister.persistArray(User.class, usersJson);
		persister.persistArray(Group.class, groupsJson);

		Dao<User, Integer> userDao = helper.getDao(User.class);
		Dao<Group, Integer> GroupDao = helper.getDao(Group.class);

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

		DatabaseHelper.releaseHelper();
	}
}
