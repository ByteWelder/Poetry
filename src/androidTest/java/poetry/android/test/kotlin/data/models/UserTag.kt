package poetry.android.test.kotlin.data.models

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable
class UserTag(
	@DatabaseField(generatedId = true, columnName = "id")
	val id: Int,

	@DatabaseField(foreign = true, columnName = "user_id")
	val user: User,

	@DatabaseField(columnName = "value")
	val tag: String
) {
	internal constructor(): this(0, User.PlaceHolder, "")
}
