package poetry.android.test.java.data.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import poetry.annotations.MapFrom;

/**
 * Maps a User onto a Group
 */
@DatabaseTable
public class UserGroup
{
    @DatabaseField(generatedId = true)
	@MapFrom("id")
    private int id;

    @DatabaseField(foreign = true, columnName = "user_id")
	@MapFrom("user")
	private User user;

    @DatabaseField(foreign = true, columnName = "group_id")
	@MapFrom("group")
    private Group group;
}
