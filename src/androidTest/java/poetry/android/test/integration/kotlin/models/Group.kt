package poetry.android.test.integration.kotlin.models

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import poetry.annotations.MapFrom

@DatabaseTable
class Group(
	@DatabaseField(id = true, columnName = "id")
	@MapFrom("id")
	var id: Int = 0,

	@DatabaseField(columnName = "name")
	@MapFrom("name")
	var name: String? = null
)
