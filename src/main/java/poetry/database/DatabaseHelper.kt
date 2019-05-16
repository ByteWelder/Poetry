package poetry.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import java.sql.SQLException
import java.util.*

open class DatabaseHelper : OrmLiteSqliteOpenHelper {

	constructor(context: Context) : super(context, configuration!!.databaseName, null, configuration!!.modelVersion)

	constructor(context: Context, configuration: DatabaseConfiguration) : super(context, configuration.databaseName, null, configuration.modelVersion) {

		DatabaseHelper.configuration = configuration
	}

	override fun onCreate(db: SQLiteDatabase, connectionSource: ConnectionSource) {
		createDatabase()
	}

	fun createTable(classObject: Class<*>) {
		try {
			TableUtils.createTable(getConnectionSource(), classObject)
		} catch (e: SQLException) {
			Log.d(DatabaseHelper::class.java.name, "Can't create database", e)
			throw RuntimeException(e)
		}

	}

	override fun onUpgrade(db: SQLiteDatabase, connectionSource: ConnectionSource, oldVersion: Int, newVersion: Int) {
		recreateDatabase()
	}

	fun <T> dropTable(classObject: Class<T>) {
		try {
			TableUtils.dropTable<T, Any>(getConnectionSource(), classObject, true)

			if (cachedDaos.containsKey(classObject)) {
				cachedDaos.remove(classObject)
			}
		} catch (e: SQLException) {
			Log.e(DatabaseHelper::class.java.name, "can't drop table", e)
		}

	}

	fun recreateDatabase() {
		dropDatabase()
		createDatabase()
	}

	/**
	 * Drops all tables.
	 */
	fun dropDatabase() {
		for (classObject in configuration!!.modelClasses) {
			dropTable(classObject)
		}
	}

	private fun createDatabase() {
		for (classObject in configuration!!.modelClasses) {
			createTable(classObject)
		}
	}

	@Throws(java.sql.SQLException::class)
	override fun <D : com.j256.ormlite.dao.Dao<T, *>, T> getDao(clazz: java.lang.Class<T>): D {
		val dao: D? = cachedDaos[clazz] as D?
		return if (dao != null) {
			dao
		} else {
			val superDao = super.getDao(clazz)
			cachedDaos[clazz] = superDao
			superDao as D
		}
	}

	companion object {
		private var configuration: DatabaseConfiguration? = null
		protected val cachedDaos = HashMap<Class<*>, Dao<*, *>>()

		fun getHelper(context: Context): DatabaseHelper {
			return OpenHelperManager.getHelper(context, DatabaseHelper::class.java)
		}

		fun <T : DatabaseHelper> getHelper(context: Context, classObject: Class<T>): T {
			return OpenHelperManager.getHelper(context, classObject)
		}

		fun releaseHelper() {
			OpenHelperManager.releaseHelper()
		}
	}
}
