package poetry.android.test.java.data.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class UserTag
{
    @DatabaseField(generatedId = true, columnName = "id")
    private int id;

    @DatabaseField(foreign = true, columnName = "user_id")
    private User user;

    @DatabaseField(columnName = "value")
	private String value;

    public String getTag()
    {
        return value;
    }
}
