package poetry

import com.j256.ormlite.dao.ForeignCollection

fun <T> ForeignCollection<T>?.toListOrEmptyList(): List<T> = this?.toList() ?: emptyList()
