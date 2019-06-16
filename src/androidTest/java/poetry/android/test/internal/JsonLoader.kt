package poetry.android.test.internal

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException

@Throws(IOException::class, JSONException::class)
fun loadJsonObject(rawResId: Int): JSONObject {
	val context = InstrumentationRegistry.getInstrumentation().context
	val inputStream = context.resources.openRawResource(rawResId)
	val tokener = JSONTokener(inputStream.reader().readText())
	return JSONObject(tokener)
}
