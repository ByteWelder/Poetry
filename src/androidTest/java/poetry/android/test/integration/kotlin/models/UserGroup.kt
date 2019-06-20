package poetry.android.test.integration.kotlin.models

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import poetry.annotations.MapFrom

/**
 * Maps a User onto a Group
 */
@DatabaseTable
class UserGroup(
	@DatabaseField(generatedId = true)
	@MapFrom("id")
	var id: Int = 0,

	@DatabaseField(foreign = true, columnName = "user_id")
	@MapFrom("user")
	var user: User? = null,

	@DatabaseField(foreign = true, columnName = "group_id")
	@MapFrom("group")
	var group: Group? = null
)
