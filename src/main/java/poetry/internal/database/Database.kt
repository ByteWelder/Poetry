package poetry.internal.database

import android.content.ContentValues
import android.util.Log

private const val logTag = "NativeDatabase"

interface Database {
	fun beginTransaction()
	fun endTransaction()
	fun inTransaction(): Boolean
	fun setTransactionSuccessful()

	fun queryFirst(table: String, column: String, value: String): Long?
	fun insert(table: String, values: ContentValues, columnHack: String? = null): Long
	fun insertOrThrow(table: String, values: ContentValues, columnHack: String? = null): Long
	fun update(table: String, values: ContentValues, whereClause: String, whereArgs: Array<String?>): Int
	fun delete(table: String, whereClause: String, whereArgs: Array<String?>): Int
}

internal fun <TransactionResult> Database.transaction(
	transactionBlock: () -> (TransactionResult)
): TransactionResult {
	try {
		beginTransaction()
		val result = transactionBlock()
		setTransactionSuccessful()
		return result
	} catch (caught: Exception) {
		throw caught
	} finally {
		endTransactionSafely()
	}
}

private fun Database.endTransactionSafely() {
	if (inTransaction()) {
		try {
			endTransaction()
		} catch (e: IllegalStateException) {
			Log.w(logTag, "endTransaction() failed: ${e.message}")
		}
	}
}
