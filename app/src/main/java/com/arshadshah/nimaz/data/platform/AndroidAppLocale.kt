package com.arshadshah.nimaz.data.platform

import android.content.Context
import com.arshadshah.nimaz.core.util.LocaleHelper
import com.arshadshah.nimaz.data.audio.AdhanDownloadService
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.domain.repository.AdhanDownloader
import com.arshadshah.nimaz.domain.repository.AppLocale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppLocale @Inject constructor(
    @ApplicationContext private val context: Context
) : AppLocale {
    override fun apply(languageCode: String) = LocaleHelper.setLocale(context, languageCode)
}

@Singleton
class ServiceAdhanDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) : AdhanDownloader {
    override fun download(adhanSoundName: String) =
        AdhanDownloadService.downloadSelected(context, AdhanSound.fromName(adhanSoundName))
}
