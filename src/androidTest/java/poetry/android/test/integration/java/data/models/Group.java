package poetry.android.test.integration.java.data.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import poetry.annotations.MapFrom;

@DatabaseTable
public class Group
{
    @DatabaseField(id = true, columnName = "id")
	@MapFrom("id")
    private int id;

    @DatabaseField(columnName = "name")
	@MapFrom("name")
    private String name;

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}
}