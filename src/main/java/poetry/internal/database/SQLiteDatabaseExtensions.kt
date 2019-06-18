package poetry.internal.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.json.JSONException

private const val logTag = "SQLiteDatabase"

internal fun SQLiteDatabase.enableWriteAheadLoggingSafely() {
	try {
		// Write Ahead Logging (WAL) mode cannot be enabled or disabled while there are transactions in progress.
		if (!inTransaction()) {
			enableWriteAheadLogging()
		}
	} catch (e: IllegalStateException) {
		Log.w(logTag, "Write Ahead Logging is not enabled because a transaction was active")
	}
}

@Throws(JSONException::class)
internal fun <TransactionResult> SQLiteDatabase.transactionNonExclusive(
	transactionBlock: () -> (TransactionResult)
): TransactionResult {
	try {
		beginTransactionNonExclusive()
		val result = transactionBlock()
		setTransactionSuccessful()
		return result
	} catch (caught: JSONException) {
		throw caught
	} finally {
		endTransactionSafely()
	}
}

/**
 * Create a row in the database if it doesn't exist yet.
 *
 * @param idColumnName the name of the id column
 * @param id            the id to create a row for (if it doesn't exist yet)
 * @param tableName     the table to insert a new row in case the ID is not found in the database
 * @throws JSONException when the ID field value cannot be determined
 */
@Throws(JSONException::class)
internal fun SQLiteDatabase.createRowIfNotExists(tableName: String, idColumnName: String, id: Any) {
	val sql = "SELECT * FROM '$tableName' WHERE $idColumnName = ? LIMIT 1"
	val selectionArgs = arrayOf(id.toString())
	val cursor = rawQuery(sql, selectionArgs)
	val objectExists = cursor.count > 0
	cursor.close()

	if (!objectExists) {
		val values = ContentValues(1).apply {
			putOrThrow(idColumnName, id)
		}
		val insertedId = insertOrThrow("'$tableName'", null, values)
		Log.i(logTag, "prepared $tableName row (id=$id/${java.lang.Long.toString(insertedId)})")
	}
}

private fun SQLiteDatabase.endTransactionSafely() {
	if (inTransaction()) {
		try {
			endTransaction()
		} catch (e: IllegalStateException) {
			Log.w(logTag, "endTransaction() failed - this does not mean there was a rollback, it just means that the transaction was closed earlier than expected.")
		}
	}
}
