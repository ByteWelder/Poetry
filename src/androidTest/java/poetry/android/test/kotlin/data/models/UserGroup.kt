package poetry.android.test.kotlin.data.models

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import poetry.json.annotations.MapFrom

/**
 * Maps a User onto a Group
 */
@DatabaseTable
class UserGroup(
	@DatabaseField(generatedId = true)
	@MapFrom("id")
	val id: Int,

	@DatabaseField(foreign = true, columnName = "user_id")
	@MapFrom("user")
	val user: User,

	@DatabaseField(foreign = true, columnName = "group_id")
	@MapFrom("group")
	val group: Group? = null
) {
	internal constructor(): this(0, User.PlaceHolder)
}
