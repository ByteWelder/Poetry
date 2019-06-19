package poetry.internal.database.native

import android.database.Cursor

fun Cursor.hasItems() = count != 0