package poetry.utils

import android.util.Log

import com.j256.ormlite.dao.Dao

import poetry.reflection.AnnotationRetriever
import poetry.reflection.OrmliteReflection

/**
 * Docs: http://www.sqlite.org/datatype3.html
 */
internal enum class ColumnType {
	INTEGER, // for int and boolean
	REAL, // float,  double, etc.
	TEXT, // String, etc.
	BLOB,
	NUMERIC
}

/**
 * A set of utilities for Ormlite Dao querying.
 */
internal object DaoUtils {

	/**
	 * Execute a raw query.
	 * It exists to provide logging of all DaoUtils queries.
	 *
	 * @param dao   the Dao to execute the query for
	 * @param query the raw query to execute
	 * @throws java.sql.SQLException when the query fails to run
	 */
	@Throws(java.sql.SQLException::class)
	private fun executeQuery(dao: Dao<*, *>, query: String) {
		Log.d(DaoUtils::class.java.name, "query: $query")
		dao.executeRawNoArgs(query)
	}

	/**
	 * Add a column to a table.
	 *
	 * @param dao        the Dao to execute the query for
	 * @param columnName the column to add
	 * @param columnType the type of column to add
	 * @throws java.sql.SQLException when the query fails to run
	 */
	@Throws(java.sql.SQLException::class)
	fun addColumn(dao: Dao<*, *>, columnName: String, columnType: ColumnType) {
		val query = String.format("ALTER TABLE %s ADD COLUMN %s %s",
				OrmliteReflection.getTableName(AnnotationRetriever(), dao.dataClass),
				columnName,
				columnType.toString())

		executeQuery(dao, query)
	}

	/**
	 * Add a column to a table with default value for column inserts without value.
	 *
	 * @param dao          the Dao to execute the query for
	 * @param columnName   the column to add
	 * @param columnType   the type of column to add
	 * @param defaultValue the default value for newly inserted rows that don't have a value specified for this column
	 * @throws java.sql.SQLException when the query fails to run
	 */
	@Throws(java.sql.SQLException::class)
	fun addColumn(dao: Dao<*, *>, columnName: String, columnType: ColumnType, defaultValue: String) {
		val query = String.format("ALTER TABLE %s ADD COLUMN %s %s DEFAULT %s",
				OrmliteReflection.getTableName(AnnotationRetriever(), dao.dataClass),
				columnName,
				columnType.toString(),
				defaultValue)

		executeQuery(dao, query)
	}

	/**
	 * Copy values from an existing column to another existing column.
	 *
	 * @param dao      the Dao to execute the query for
	 * @param fromName the column to copy from
	 * @param toName   the column to copy to
	 * @throws java.sql.SQLException when the query fails to run
	 */
	@Throws(java.sql.SQLException::class)
	fun copyColumn(dao: Dao<*, *>, fromName: String, toName: String) {
		val query = String.format("UPDATE %s SET %s = %s",
				OrmliteReflection.getTableName(AnnotationRetriever(), dao.dataClass),
				toName,
				fromName)

		executeQuery(dao, query)
	}

	/**
	 * Create an index for a specific column.
	 *
	 * @param dao        the Dao to execute the query for
	 * @param columnName the column to create the index for
	 * @param indexName  the name of the index to create
	 * @throws java.sql.SQLException when the query fails to run
	 */
	@Throws(java.sql.SQLException::class)
	@JvmOverloads
	fun createIndex(dao: Dao<*, *>, columnName: String, indexName: String = String.format("%s_index", columnName)) {
		val query = String.format("CREATE INDEX %s ON %s (%s)",
				indexName,
				OrmliteReflection.getTableName(AnnotationRetriever(), dao.dataClass),
				columnName)

		executeQuery(dao, query)
	}
}
