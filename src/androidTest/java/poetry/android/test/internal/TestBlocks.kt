package poetry.android.test.internal

import com.j256.ormlite.dao.Dao
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import poetry.Poetry
import kotlin.reflect.KClass

fun <ModelType : Any> JSONObject.test(
	helper: poetry.DatabaseHelper,
	modelClass: KClass<ModelType>,
	evaluationBlock: (ModelType) -> (Unit)
) {
	// Persist arrays to database
	val poetry = Poetry(helper.writableDatabase)
	poetry.writeObject(modelClass, this)

	val dao = helper.getDao<Dao<ModelType, *>, ModelType>(modelClass.java)
	val models = dao.queryForAll()
	Assert.assertEquals(1, models.size.toLong())

	val model = models.first()
	evaluationBlock(model)
}

fun <ModelType : Any> JSONArray.test(
	helper: poetry.DatabaseHelper,
	modelClass: KClass<ModelType>,
	evaluationBlock: (List<ModelType>) -> (Unit)
) {
	// Persist arrays to database
	val poetry = Poetry(helper.writableDatabase)
	poetry.writeArray(modelClass, this)

	val dao = helper.getDao<Dao<ModelType, *>, ModelType>(modelClass.java)
	val models = dao.queryForAll()
	evaluationBlock(models)
}
