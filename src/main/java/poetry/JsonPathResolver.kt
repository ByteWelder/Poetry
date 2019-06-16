package poetry

import org.json.JSONArray
import org.json.JSONObject

/**
 * Given a JSON Object { 'root' : { 'key' : 'value' }}
 * It can resolve the String "value" when given the path "root.key"
 */
object JsonPathResolver {

	/**
	 * Resolve a path for the provided object.
	 *
	 * @param jsonObject the object to resolve the path for
	 * @param path   the path to resolve
	 * @return the found JSONObject on the path
	 * @throws JsonPathException if processing fails
	 */
	@JvmStatic
	@Throws(JsonPathException::class)
	fun resolveObject(jsonObject: JSONObject, path: String): JSONObject {
		if (path.isEmpty()) {
			return jsonObject
		}

		val keys = path.toKeysFromPath()
		var current = jsonObject

		keys.forEachIndexed { keyIndex, key ->
			val childObject = current.getChildOrJsonPathException(key)

			if (JSONArray::class.java.isAssignableFrom(childObject.javaClass)) {
				if (keyIndex == keys.lastIndex) {
					throw JsonPathException("last element \"$key\" is an array and not an object")
				} else {
					throw JsonPathException("array element for \"$key\" cannot be parsed at $path")
				}
			} else if (JSONObject::class.java.isAssignableFrom(childObject.javaClass)) {
				if (keyIndex == keys.lastIndex) {
					return childObject as JSONObject
				} else {
					current = childObject as JSONObject
				}
			} else {
				throw JsonPathException("can't parse element for $key on path $path because the type ${childObject.javaClass.name} is not supported")
			}
		}

		throw JsonPathException("json path mapping failed")
	}

	/**
	 * Resolve a path for the provided object.
	 *
	 * @param jsonObject the object to resolve the path for
	 * @param path   the path to resolve
	 * @return the found JSONArray on the path
	 * @throws JsonPathException if processing fails
	 */
	@JvmStatic
	@Throws(JsonPathException::class)
	fun resolveArray(jsonObject: JSONObject, path: String): JSONArray {
		if (path.isEmpty()) {
			throw JsonPathException("root of JSONObject can never be a JSONArray")
		}

		val keys = path.toKeysFromPath()
		var current = jsonObject

		keys.forEachIndexed { keyIndex, key ->
			val childObject = current.getChildOrJsonPathException(key)

			when {
				JSONArray::class.java.isAssignableFrom(childObject.javaClass) -> return if (keyIndex == keys.lastIndex) {
					childObject as JSONArray
				} else {
					throw JsonPathException("array element for \"$key\" is not the last element on the path $path")
				}
				JSONObject::class.java.isAssignableFrom(childObject.javaClass) -> current = childObject as JSONObject
				else -> throw JsonPathException("can't parse element for $key on path $path because the type ${childObject.javaClass.name} is not supported")
			}
		}

		throw JsonPathException("json path mapping failed")
	}
}

private fun JSONObject.getChildOrJsonPathException(key: String): Any = try {
	get(key)
} catch (e: Exception) {
	throw JsonPathException("failed to fetch element \"$key\"")
}

private fun String.toKeysFromPath() = split(".").dropLastWhile { it.isEmpty() }.toTypedArray()