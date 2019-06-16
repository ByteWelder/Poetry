package poetry.android.test.kotlin.data

import android.content.Context
import poetry.android.test.kotlin.data.models.Group
import poetry.android.test.kotlin.data.models.User
import poetry.android.test.kotlin.data.models.UserGroup
import poetry.android.test.kotlin.data.models.UserTag
import poetry.database.DatabaseConfiguration

class DatabaseHelper(context: Context) : poetry.database.DatabaseHelper(context, sConfiguration) {

	/**
	 * When calling the parent class, the whole database is deleted and re-created.
	 * Custom upgrade code goes here (after super call) to override that behavior.
 	 */
//	override fun onUpgrade(db: SQLiteDatabase, connectionSource: ConnectionSource, oldVersion: Int, newVersion: Int) {
//		super.onUpgrade(db, connectionSource, oldVersion, newVersion)
//	}

	companion object {
		val sConfiguration = DatabaseConfiguration(
				1,
				arrayOf(
					User::class.java,
					Group::class.java,
					UserTag::class.java,
					UserGroup::class.java
				)
		)
	}
}