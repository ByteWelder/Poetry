package poetry.internal.database.native

import android.database.Cursor

internal fun <Result> Cursor.use(block: (Cursor) -> (Result)): Result {
	moveToFirst()
	val result = block(this)
	close()
	return result
}
