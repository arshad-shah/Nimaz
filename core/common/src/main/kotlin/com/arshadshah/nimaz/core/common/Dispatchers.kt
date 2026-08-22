package com.arshadshah.nimaz.core.common

import javax.inject.Qualifier

/**
 * The dispatcher for work that is CPU-bound rather than blocking on I/O.
 *
 * Injected rather than referenced as `Dispatchers.Default` so a test can substitute its own
 * scheduler and stay deterministic — without that, a `withContext(Dispatchers.Default)` runs
 * on real threads and `advanceUntilIdle()` has nothing to wait for.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * The dispatcher for work that blocks on I/O — a geocoder round trip, a location fix.
 *
 * Same reason as [DefaultDispatcher]: injected so a test stays deterministic.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

// These two moved down from `core/di/TimeModule.kt` in #560. The *bindings* that supply them
// stay there, in `:app`, and nothing about the DI graph changed: Hilt matches a qualifier by
// annotation type, not by where it is declared.
//
// They had to move because `core/di/` stays in `:app` until PR 22, while the files that inject
// `@IoDispatcher` are leaving — `AndroidDeviceLocationRepository` and `WidgetSettingsWatcher` in
// #560, `QuranAudioManager` in PR 20. A `:core:*` module reaching back into `:app` for an
// annotation is exactly the edge `moduleBoundary` fails the build for, and it would have blocked
// every one of those moves.
