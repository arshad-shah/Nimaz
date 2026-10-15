package com.arshadshah.nimaz.data.repository

/**
 * This list re-ordered to follow [keys], the order the search index ranked them in.
 *
 * Every by-ids lookup a search ends in (`getHadithsByIds`, `getDuasByIds`,
 * `getAyahsWithTextByIds`, …) carries its own `ORDER BY` — book, id, ayah — which silently
 * throws the ranking away. Rows whose key is not in [keys] go last, in their existing order.
 */
internal fun <T, K> List<T>.inOrderOf(keys: List<K>, key: (T) -> K): List<T> {
    val rank = HashMap<K, Int>(keys.size)
    keys.forEachIndexed { index, k -> rank.putIfAbsent(k, index) }
    return sortedBy { rank[key(it)] ?: Int.MAX_VALUE }
}
