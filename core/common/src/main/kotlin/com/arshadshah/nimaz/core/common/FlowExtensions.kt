package com.arshadshah.nimaz.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Map every item of each emitted list.
 *
 * Shorthand for the very common `flow.map { list -> list.map(transform) }` used
 * throughout the repositories to turn a [Flow] of database entities into a
 * [Flow] of domain models, e.g. `dao.getAll().mapItems { it.toDomain() }`.
 *
 * In `:core:common` rather than pushed down to `:core:data`, which #560's triage suggested on
 * the grounds that the 14 repository impls are its only callers today. It is a generic `Flow`
 * extension with no knowledge of data at all, it sits with the other generic helpers this module
 * took in #579, and `:core:data` already depends on it — so "its only consumer" would be an
 * accident of the current call sites rather than a property of the code.
 */
inline fun <T, R> Flow<List<T>>.mapItems(crossinline transform: (T) -> R): Flow<List<R>> =
    map { list -> list.map(transform) }
