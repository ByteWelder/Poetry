package poetry.android.test.kotlin.data.models

import com.j256.ormlite.dao.ForeignCollection
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.table.DatabaseTable
import poetry.annotations.ForeignCollectionFieldSingleTarget
import poetry.annotations.ManyToManyField
import poetry.annotations.MapFrom

@DatabaseTable
class User(
	@DatabaseField(id = true, columnName = "id")
	@MapFrom("id")
	val id: Int,

	@DatabaseField(columnName = "name")
	@MapFrom("name")
	val name: String,

	/**
	 * Many-to-many relationships.
	 *
	 * OrmLite requires a ForeignCollectionField with the helper-type UserGroup to assist in the database relational mapping.
	 * JSON-to-SQLite persistence also requires the additional annotation "ManyToManyField"
	 */
	@ForeignCollectionField(eager = true)
	@ManyToManyField(targetType = Group::class)
	@MapFrom("groups")
	val groups: ForeignCollection<UserGroup>? = null,

	/**
	 * One-to-many relationships on simple types (arrays of strings/integers/etc.)
	 *
	 * OrmLite requries a ForeignCollectionField with the helper-type UserTag.
	 * JSON-to-SQLite persistence also requires the additional annotation "ForeignCollectionFieldSingleTarget" to
	 * specify in which field of the UserTag table the simple type is stored. In this case the column name is "value":
	 */
	@ForeignCollectionField(eager = true)
	@ForeignCollectionFieldSingleTarget(targetField = "value")
	@MapFrom("tags")
	val tags: ForeignCollection<UserTag>? = null
) {

	internal constructor(): this(0, "")

	companion object {
		internal val PlaceHolder = User(0, "")
	}

	val tagsAsList: List<String?>
		get() {
			val safeTags = tags ?: return emptyList()
			val tags = ArrayList<String?>(safeTags.size)

			for (safeTag in safeTags) {
				tags.add(safeTag.tag)
			}

			return tags
		}
}
