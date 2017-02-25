package poetry.test;

import android.test.AndroidTestCase;

import com.j256.ormlite.dao.Dao;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import poetry.json.JsonPathResolver;
import poetry.json.JsonPersister;
import poetry.test.data.DatabaseHelper;
import poetry.test.data.JsonLoader;
import poetry.test.data.models.Group;
import poetry.test.data.models.User;

public class JsonTestCase extends AndroidTestCase
{
    public void testJsonMapper() throws Exception
    {
        DatabaseHelper helper = DatabaseHelper.getHelper(getContext());

        // Load JSON
        JSONObject json = JsonLoader.loadObject(getContext(), nl.elastique.poetry.data.test.R.raw.test);

        // Get child arrays from JSON
        JSONArray users_json = JsonPathResolver.resolveArray(json, "users");
        JSONArray groups_json = JsonPathResolver.resolveArray(json, "groups");

        // Persist arrays to database
        JsonPersister persister = new JsonPersister(helper.getWritableDatabase());
        persister.persistArray(User.class, users_json);
		persister.persistArray(Group.class, groups_json);

        Dao<User, Integer> user_dao = helper.getDao(User.class);
		Dao<Group, Integer> group_dao = helper.getDao(Group.class);

		List<User> users = user_dao.queryForAll();
        assertEquals(2, users.size());

        List<Group> groups = group_dao.queryForAll();
        assertEquals(3, groups.size());

        User user = user_dao.queryForId(1);
        assertNotNull(user);
        assertEquals("John", user.getName());
        assertEquals(2, user.getTags().size());
        assertEquals("tag2", user.getTags().get(1));

        Group group = group_dao.queryForId(2);
        assertNotNull(group);
        assertEquals("Group B", group.getName());

        DatabaseHelper.releaseHelper();
    }
}