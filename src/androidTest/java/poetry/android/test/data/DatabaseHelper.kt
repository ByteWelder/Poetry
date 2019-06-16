package poetry.android.test.data

import android.content.Context
import com.j256.ormlite.android.apptools.OpenHelperManager
import poetry.database.DatabaseConfiguration
import poetry.android.test.data.models.Group
import poetry.android.test.data.models.User
import poetry.android.test.data.models.UserGroup
import poetry.android.test.data.models.UserTag

fun getDatabaseHelper(context: Context): DatabaseHelper =
	OpenHelperManager.getHelper(context, DatabaseHelper::class.java)

class DatabaseHelper(context: Context) : poetry.database.DatabaseHelper(context, sConfiguration) {

	/**
	 * When calling the parent class, the whole database is deleted and re-created.
	 * Custom upgrade code goes here (after super call) to override that behavior.
 	 */
//	override fun onUpgrade(db: SQLiteDatabase, connectionSource: ConnectionSource, oldVersion: Int, newVersion: Int) {
//		super.onUpgrade(db, connectionSource, oldVersion, newVersion)
//	}

	companion object {
		val sConfiguration = DatabaseConfiguration(7, arrayOf(User::class.java, Group::class.java, UserTag::class.java, UserGroup::class.java))
	}
}