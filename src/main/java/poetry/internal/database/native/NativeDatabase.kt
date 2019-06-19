package poetry.internal.database.native

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import poetry.internal.database.Database
import poetry.internal.database.NO_ID

private const val logTag = "NativeDatabase"

class NativeDatabase(
	private val db: SQLiteDatabase
) : Database {

	init {
		db.enableWriteAheadLoggingSafely()
	}

	override fun beginTransaction() {
		db.beginTransaction()
	}

	override fun endTransaction() {
		db.endTransaction()
	}

	override fun inTransaction() = db.inTransaction()

	override fun setTransactionSuccessful() {
		db.setTransactionSuccessful()
	}

	override fun queryFirst(table: String, column: String, value: String): Long {
		val cursor = db.rawQuery("SELECT column FROM '$table' WHERE $column = ? LIMIT 1", arrayOf(value))
		return cursor.use {
			if (cursor.hasItems()) {
				cursor.getLong(0)
			} else {
				NO_ID
			}
		}
	}

	override fun insert(table: String, values: ContentValues, columnHack: String?): Long {
		return db.insert("'$table'", null, values)
	}

	override fun insertOrThrow(table: String, values: ContentValues, columnHack: String?): Long {
		return db.insertOrThrow("'$table'", columnHack, values)
	}

	override fun update(table: String, values: ContentValues, whereClause: String, whereArgs: Array<String?>): Int {
		return db.update("'$table'", values, whereClause, whereArgs)
	}

	override fun delete(table: String, whereClause: String, whereArgs: Array<String?>): Int {
		return db.delete("'$table'", whereClause, whereArgs)
	}
}

private fun SQLiteDatabase.enableWriteAheadLoggingSafely() {
	try {
		// Write Ahead Logging (WAL) mode cannot be enabled or disabled while there are transactions in progress.
		if (!inTransaction()) {
			enableWriteAheadLogging()
		}
	} catch (e: IllegalStateException) {
		Log.w(logTag, "Write Ahead Logging is not enabled because a transaction was active")
	}
}
