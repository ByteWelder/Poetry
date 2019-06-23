package poetry.internal.database.native

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import poetry.internal.database.Database

private const val logTag = "NativeDatabase"

class NativeDatabase(
	private val db: SQLiteDatabase,
	private val logging: Boolean = true
) : Database {

	init {
		db.enableWriteAheadLoggingSafely()
	}

	override fun beginTransaction() {
		if (logging) {
			Log.d(logTag, "beginTransaction")
		}
		db.beginTransaction()
	}

	override fun endTransaction() {
		if (logging) {
			Log.d(logTag, "endTransaction")
		}
		db.endTransaction()
	}

	override fun inTransaction() = db.inTransaction()

	override fun setTransactionSuccessful() {
		if (logging) {
			Log.d(logTag, "setTransactionSuccessful")
		}
		db.setTransactionSuccessful()
	}

	override fun queryFirst(table: String, column: String, value: String): Long? {
		if (logging) {
			Log.d(logTag, "queryFirst($table, $column = $value)")
		}
		return db.query("'$table'", arrayOf("ROWID"), "$column = ?", arrayOf(value), null, null, null).use {
			if (it.hasItems()) {
				it.getLong(0)
			} else {
				null
			}
		}
	}

	override fun insert(table: String, values: ContentValues, columnHack: String?): Long {
		if (logging) {
			Log.d(logTag, "insert($table, $values, $columnHack)")
		}
		return db.insert("'$table'", null, values)
	}

	override fun insertOrThrow(table: String, values: ContentValues, columnHack: String?): Long {
		if (logging) {
			Log.d(logTag, "insert($table, $values, $columnHack)")
		}
		return db.insertOrThrow("'$table'", columnHack, values)
	}

	override fun update(table: String, values: ContentValues, whereClause: String, whereArgs: Array<String?>): Int {
		if (logging) {
			Log.d(logTag, "update($table, $values, $whereArgs, $whereArgs)")
		}
		return db.update("'$table'", values, whereClause, whereArgs)
	}

	override fun delete(table: String, whereClause: String, whereArgs: Array<String?>): Int {
		if (logging) {
			Log.d(logTag, "delete($table, $whereClause, $whereArgs")
		}
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
