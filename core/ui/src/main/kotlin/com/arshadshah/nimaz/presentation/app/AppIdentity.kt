package com.arshadshah.nimaz.presentation.app

import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Who the running app is: the version it reports and the icon it shows.
 *
 * `AboutScreen` read `BuildConfig.VERSION_NAME`, `BuildConfig.VERSION_CODE` and
 * `R.mipmap.ic_launcher_foreground` directly. None of the three can follow a screen out of
 * `:app`: a library's `BuildConfig` carries only its **own** fields, and `nonTransitiveRClass`
 * keeps the application's `R` off a library's classpath. The mipmaps in particular stayed behind
 * on purpose in PR 10 of #551 — `themes.xml` references them for the splash screen and would not
 * AAPT-link from here.
 *
 * Copying the values into each module's build file would make app identity something every
 * feature module restates, and copying the icon would duplicate a binary asset. So the
 * composition root states it once and features read it, which is the same inversion
 * `IntegrityTokenProvider` uses for its two `BuildConfig` reads — constructor parameters there,
 * a CompositionLocal here, because the reader is a composable rather than an injected class.
 *
 * @param iconRes the *foreground* layer of the adaptive launcher icon. `AboutScreen` draws it on
 *   a `primaryContainer` circle of its own, so the adaptive icon's own background must not come
 *   with it — which is why this is a specific resource rather than
 *   `PackageManager.getApplicationIcon()`.
 */
data class AppIdentity(
    val versionName: String,
    val versionCode: Int,
    @param:DrawableRes val iconRes: Int,
)

/**
 * Provided by `MainActivity`. The default is deliberately **not** a plausible-looking version:
 * a `@Preview` or a test that renders a screen without providing identity should look obviously
 * unconfigured rather than quietly claim to be some release.
 */
val LocalAppIdentity = staticCompositionLocalOf {
    AppIdentity(versionName = "—", versionCode = 0, iconRes = 0)
}
