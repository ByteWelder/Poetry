package poetry.android.test.integration.kotlin.models

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable
class UserTag(
	@DatabaseField(generatedId = true, columnName = "id")
	var id: Int = 0,

	@DatabaseField(foreign = true, columnName = "user_id")
	var user: User? = null,

	@DatabaseField(columnName = "value")
	var tag: String? = null
)
