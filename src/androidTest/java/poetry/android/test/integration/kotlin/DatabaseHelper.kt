package poetry.android.test.integration.kotlin

import android.content.Context
import poetry.DatabaseHelper
import poetry.android.test.integration.kotlin.models.Group
import poetry.android.test.integration.kotlin.models.User
import poetry.android.test.integration.kotlin.models.UserGroup
import poetry.android.test.integration.kotlin.models.UserTag
import poetry.databaseConfigurationOf

class DatabaseHelper(context: Context) : DatabaseHelper(
	context,
	databaseConfigurationOf(
		"integration",
		User::class,
		Group::class,
		UserTag::class,
		UserGroup::class
	)
)