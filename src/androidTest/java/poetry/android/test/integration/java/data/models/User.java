package poetry.android.test.integration.java.data.models;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.List;

import poetry.annotations.ForeignCollectionFieldSingleTarget;
import poetry.annotations.ManyToManyField;
import poetry.annotations.MapFrom;

@DatabaseTable
public class User
{
    @DatabaseField(id = true, columnName = "id")
	@MapFrom("id")
    private int id;

    @DatabaseField(columnName = "name")
	@MapFrom("name")
    private String name;

    /**
     * Many-to-many relationships.
     *
     * OrmLite requires a ForeignCollectionField with the helper-type UserGroup to assist in the database relational mapping.
     * JSON-to-SQLite persistence also requires the additional annotation "ManyToManyField"
     */
    @ForeignCollectionField(eager = true)
    @ManyToManyField(targetType = Group.class)
	@MapFrom("groups")
    private ForeignCollection<UserGroup> groups;

    /**
     * One-to-many relationships on simple types (arrays of strings/integers/etc.)
     *
     * OrmLite requries a ForeignCollectionField with the helper-type UserTag.
     * JSON-to-SQLite persistence also requires the additional annotation "ForeignCollectionFieldSingleTarget" to
     * specify in which field of the UserTag table the simple type is stored. In this case the column name is "value":
     */
    @ForeignCollectionField(eager = true)
    @ForeignCollectionFieldSingleTarget(targetField = "value")
	@MapFrom("tags")
    private ForeignCollection<UserTag> tags;

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

    public List<String> getTags()
    {
        List<String> tagList = new ArrayList<>();

        for (UserTag tag : tags)
        {
            tagList.add(tag.getTag());
        }

        return tagList;
    }
}