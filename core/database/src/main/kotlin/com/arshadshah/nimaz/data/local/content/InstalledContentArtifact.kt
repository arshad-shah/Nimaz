package com.arshadshah.nimaz.data.local.content

import javax.inject.Qualifier

/**
 * The SHA-256 of the content artifact this build ships, as a string.
 *
 * A qualifier rather than a direct `BuildConfig` read, because `CONTENT_ARTIFACT_SHA256` is a
 * field of the **application's** `BuildConfig` and a library's carries only its own. When
 * `DatabaseModule` moved here in PR 22 of #551 that read could not come with it, so `:app`
 * provides the value and this module consumes it — the same inversion `IntegrityTokenProvider`
 * already uses for the Play Integrity project number.
 *
 * It is a plain `String` behind a qualifier rather than a wrapper type on purpose: the value is
 * compared for equality against what the device last installed, and nothing else.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InstalledContentArtifact
