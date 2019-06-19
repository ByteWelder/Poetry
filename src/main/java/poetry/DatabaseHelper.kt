package poetry

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import java.sql.SQLException

open class DatabaseHelper : OrmLiteSqliteOpenHelper {

	companion object {
		@JvmStatic
		private var configuration: DatabaseConfiguration? = null

		@JvmStatic
		protected val cachedDaos = HashMap<Class<*>, Dao<*, *>>()
	}

	constructor(context: Context) : super(
			context,
			configuration!!.databaseName,
			null,
			configuration!!.modelVersion
	)

	constructor(
		context: Context,
		configuration: DatabaseConfiguration
	) : super(context, configuration.databaseName, null, configuration.modelVersion) {
		Companion.configuration = configuration
	}

	override fun onCreate(db: SQLiteDatabase, connectionSource: ConnectionSource) {
		createDatabase()
	}

	private fun createTable(classObject: Class<*>) {
		try {
			TableUtils.createTable(getConnectionSource(), classObject)
		} catch (e: SQLException) {
			Log.d(DatabaseHelper::class.java.name, "Can't create database", e)
			throw RuntimeException(e)
		}
	}

	override fun onUpgrade(
		db: SQLiteDatabase,
		connectionSource: ConnectionSource,
		oldVersion: Int,
		newVersion: Int
	) = recreateDatabase()

	private fun <T> dropTable(classObject: Class<T>) {
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
	private fun dropDatabase() {
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
	override fun <D : Dao<T, *>, T> getDao(clazz: Class<T>): D {
		@Suppress("UNCHECKED_CAST")
		val cachedDao = cachedDaos[clazz] as D?
		return if (cachedDao != null) {
			 cachedDao
		} else {
			// fetch new Dao
			@Suppress("UNCHECKED_CAST")
			val superDao = super.getDao(clazz) as D
			cachedDaos[clazz] = superDao
			superDao
		}
	}
}
