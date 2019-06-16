package poetry.android.test.kotlin.data.models

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import poetry.json.annotations.MapFrom

@DatabaseTable
class Group(
	@DatabaseField(id = true, columnName = "id")
	@MapFrom("id")
	val id: Int,

	@DatabaseField(columnName = "name")
	@MapFrom("name")
	val name: String
) {
	internal constructor(): this(0, "")
}
