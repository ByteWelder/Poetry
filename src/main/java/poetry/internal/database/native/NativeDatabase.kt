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

	override fun hasAny(table: String, column: String, value: String): Boolean {
		if (logging) {
			Log.d(logTag, "queryFirst('$table', '$column' = '$value')")
		}
		return db.query("'$table'", emptyArray(), "$column = ?", arrayOf(value), null, null, null).use {
			it.hasItems()
		}
	}

	override fun insert(table: String, values: ContentValues, columnHack: String?): Long {
		if (logging) {
			val columnHackPostfix = columnHackToLogPostfix(columnHack)
			Log.d(logTag, "insert('$table', $values$columnHackPostfix)")
		}
		return db.insert("'$table'", null, values)
	}

	override fun insertOrThrow(table: String, values: ContentValues, columnHack: String?): Long {
		if (logging) {
			val columnHackPostfix = columnHackToLogPostfix(columnHack)
			Log.d(logTag, "insert('$table', $values$columnHackPostfix)")
		}
		return db.insertOrThrow("'$table'", columnHack, values)
	}

	override fun update(table: String, values: ContentValues, whereClause: String, whereArgs: Array<String?>): Int {
		if (logging) {
			Log.d(logTag, "update('$table', $values, '$whereClause', ${whereArgs.joinToString(",")})")
		}
		return db.update("'$table'", values, whereClause, whereArgs)
	}

	override fun delete(table: String, whereClause: String, whereArgs: Array<String?>): Int {
		if (logging) {
			Log.d(logTag, "delete('$table', '$whereClause', ${whereArgs.joinToString(",")}")
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

private fun columnHackToLogPostfix(value: String?): String {
	return if (value != null) {
		", columnHack='$value'"
	} else {
		""
	}
}