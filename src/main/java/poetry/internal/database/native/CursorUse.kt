package poetry.internal.database.native

import android.database.Cursor

internal fun <Result> Cursor.use(block: () -> (Result)): Result {
	val result = block()
	close()
	return result
}
