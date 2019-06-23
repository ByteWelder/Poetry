package poetry.internal

import android.os.Looper
import android.util.Log

private const val logTag = "Poetry"

internal fun warnIfOnMainThread(classAndMethod: String) {
	if (Looper.myLooper() == Looper.getMainLooper()) {
		Log.w(logTag, "Don't call $classAndMethod from the main thread")
	}
}
