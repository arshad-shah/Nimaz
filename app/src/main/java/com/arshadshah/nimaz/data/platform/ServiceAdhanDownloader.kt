package com.arshadshah.nimaz.data.platform

import android.content.Context
import com.arshadshah.nimaz.data.audio.AdhanDownloadService
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.domain.repository.AdhanDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Split out of `AndroidAppLocale.kt` in #560, which held both this and [AndroidAppLocale].
 *
 * They had nothing in common but a package. `AndroidAppLocale` needs only `LocaleHelper` and moved
 * to `:core:data`; this one starts `AdhanDownloadService`, so it is bound to `data/audio` and
 * travels to `:core:audio` in PR 20. Leaving them in one file would have made that file
 * unmovable — the direct PR 9 / PR 20 conflict #560's validation flagged.
 */
@Singleton
class ServiceAdhanDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) : AdhanDownloader {
    override fun download(adhanSoundName: String) =
        AdhanDownloadService.downloadSelected(context, AdhanSound.fromName(adhanSoundName))
}
