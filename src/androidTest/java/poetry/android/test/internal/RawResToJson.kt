package poetry.android.test.internal

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException

@Throws(IOException::class, JSONException::class)
fun Int.toJsonObject(): JSONObject {
	val context = InstrumentationRegistry.getInstrumentation().context
	val inputStream = context.resources.openRawResource(this)
	val tokener = JSONTokener(inputStream.reader().readText())
	return JSONObject(tokener)
}
