package poetry.android.test.internal

import androidx.test.platform.app.InstrumentationRegistry
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DatabaseHelperRule<T : OrmLiteSqliteOpenHelper>(
	private val databaseHelperClass: Class<T>
) : TestRule {
	lateinit var helper: T

	override fun apply(base: Statement, description: Description) = object : Statement() {
		override fun evaluate() {
			val context = InstrumentationRegistry.getInstrumentation().context
			// We must release the helper class manually, otherwise we get an error
			// when we call releaseHelper() and get a new one.
			OpenHelperManager.setOpenHelperClass(null)
			helper = OpenHelperManager.getHelper(context, databaseHelperClass)
			try {
				base.evaluate()
			} catch (caught: Exception) {
				// No-op
			} finally {
				OpenHelperManager.releaseHelper()
				OpenHelperManager.setOpenHelperClass(null)
			}
		}
	}
}