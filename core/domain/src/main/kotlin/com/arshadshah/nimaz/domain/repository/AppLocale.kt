package com.arshadshah.nimaz.domain.repository

/**
 * Applies the app's language to the platform.
 *
 * Persisting the choice and *applying* it are two different things: the preference lives in
 * `AppSettings.appLanguage`, while the per-app locale is Android state
 * (`LocaleManager` on 33+, `AppCompatDelegate` below). Only the second needs a `Context`, and
 * this is the seam that holds it so `SettingsViewModel` does not have to.
 */
interface AppLocale {
    /** Applies [languageCode] (an IETF tag such as `"en"` or `"ar"`) as the app's locale. */
    fun apply(languageCode: String)
}

/**
 * Starts the download of a selected adhan.
 *
 * `SettingsViewModel` called `AdhanDownloadService.downloadSelected(context, sound)` — a
 * ViewModel starting an Android `Service`, and the last thing keeping a `Context` in it.
 */
interface AdhanDownloader {
    /** Begins downloading [adhanSoundName]; a no-op if it is already present. */
    fun download(adhanSoundName: String)
}
