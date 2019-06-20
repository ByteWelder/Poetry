package poetry.android.test.internal

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

internal fun String.toJsonObject() = JSONObject(JSONTokener(this))

internal fun String.toJsonArray() = JSONArray(JSONTokener(this))
