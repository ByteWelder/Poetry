package poetry.android.test.java.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.support.ConnectionSource;

import org.jetbrains.annotations.NotNull;

import poetry.android.test.java.data.models.Group;
import poetry.android.test.java.data.models.User;
import poetry.android.test.java.data.models.UserGroup;
import poetry.android.test.java.data.models.UserTag;
import poetry.database.DatabaseConfiguration;

public class DatabaseHelper extends poetry.database.DatabaseHelper
{
	private final static DatabaseConfiguration sConfiguration = new DatabaseConfiguration(
		1,
		new Class<?>[]
		{
			User.class,
			Group.class,
			UserTag.class,
			UserGroup.class
		}
	);

	public DatabaseHelper(Context context)
	{
		super(context, sConfiguration);
	}

	@Override
	public void onUpgrade(
		@NotNull SQLiteDatabase db,
		@NotNull ConnectionSource connectionSource,
		int oldVersion,
		int newVersion)
	{
		super.onUpgrade(db, connectionSource, oldVersion, newVersion);

		// When calling the parent class, the whole database is deleted and re-created.
		// Custom upgrade code goes here to override that behavior.
	}
}
