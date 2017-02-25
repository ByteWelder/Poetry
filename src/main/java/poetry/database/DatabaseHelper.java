package poetry.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.HashMap;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
	private static DatabaseConfiguration configuration;
	protected static final HashMap<Class<?>, Dao<?, ?>> cachedDaos = new HashMap<>();

	public DatabaseHelper(Context context) {
		super(context, configuration.getDatabaseName(), null, configuration.getModelVersion());
	}

	public DatabaseHelper(Context context, DatabaseConfiguration configuration) {
		super(context, configuration.getDatabaseName(), null, configuration.getModelVersion());

		DatabaseHelper.configuration = configuration;
	}

	public static void setConfiguration(DatabaseConfiguration configuration) {
		DatabaseHelper.configuration = configuration;
	}

	public static DatabaseHelper getHelper(Context context) {
		return OpenHelperManager.getHelper(context, DatabaseHelper.class);
	}

	public static <T extends DatabaseHelper> T getHelper(Context context, Class<T> classObject) {
		return OpenHelperManager.getHelper(context, classObject);
	}

	public static void releaseHelper() {
		OpenHelperManager.releaseHelper();
	}

	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		createDatabase();
	}

	public void createTable(Class<?> classObject) {
		try {
			TableUtils.createTable(getConnectionSource(), classObject);
		} catch (SQLException e) {
			Log.d(DatabaseHelper.class.getName(), "Can't create database", e);
			throw new RuntimeException(e);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		recreateDatabase();
	}

	public <T> void dropTable(Class<T> classObject) {
		try {
			TableUtils.dropTable(getConnectionSource(), classObject, true);

			if (cachedDaos.containsKey(classObject)) {
				cachedDaos.remove(classObject);
			}
		} catch (SQLException e) {
			Log.e(DatabaseHelper.class.getName(), "can't drop table", e);
		}
	}

	public void recreateDatabase() {
		dropDatabase();
		createDatabase();
	}

	/**
	 * Drops all tables.
	 */
	public void dropDatabase() {
		for (Class<?> classObject : configuration.getModelClasses()) {
			dropTable(classObject);
		}
	}

	private void createDatabase() {
		for (Class<?> classObject : configuration.getModelClasses()) {
			createTable(classObject);
		}
	}

	@Override
	public <D extends com.j256.ormlite.dao.Dao<T, ?>, T> D getDao(java.lang.Class<T> clazz) throws java.sql.SQLException {
		@SuppressWarnings("unchecked")
		D dao = (D) cachedDaos.get(clazz);

		if (dao == null) {
			dao = super.getDao(clazz);
			cachedDaos.put(clazz, dao);
		}
		return dao;
	}
}
