package poetry.json

class JsonPathException : Exception {
	constructor(message: String) : super(message)

	constructor(message: String, parent: Throwable) : super(message, parent)
}
