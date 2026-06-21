package com.arshadshah.nimaz.core.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Map every item of each emitted list.
 *
 * Shorthand for the very common `flow.map { list -> list.map(transform) }` used
 * throughout the repositories to turn a [Flow] of database entities into a
 * [Flow] of domain models, e.g. `dao.getAll().mapItems { it.toDomain() }`.
 */
inline fun <T, R> Flow<List<T>>.mapItems(crossinline transform: (T) -> R): Flow<List<R>> =
    map { list -> list.map(transform) }
