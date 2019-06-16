package poetry.internal.database

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.json.JSONException

internal fun SQLiteDatabase.enableWriteAheadLoggingSafely() {
	try {
		// Write Ahead Logging (WAL) mode cannot be enabled or disabled while there are transactions in progress.
		if (!inTransaction()) {
			enableWriteAheadLogging()
		}
	} catch (e: IllegalStateException) {
		Log.w("SQLiteDatabase", "Write Ahead Logging is not enabled because a transaction was active")
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

private fun SQLiteDatabase.endTransactionSafely() {
	if (inTransaction()) {
		try {
			endTransaction()
		} catch (e: IllegalStateException) {
			Log.w("SQLiteDatabase", "endTransaction() failed - this does not mean there was a rollback, it just means that the transaction was closed earlier than expected.")
		}
	}
}
