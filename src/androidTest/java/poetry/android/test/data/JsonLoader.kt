package poetry.android.test.data

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException

@Throws(IOException::class, JSONException::class)
fun loadJsonObject(context: Context, rawResId: Int): JSONObject {
	val inputStream = context.resources.openRawResource(rawResId)
	val tokener = JSONTokener(inputStream.reader().readText())
	return JSONObject(tokener)
}
