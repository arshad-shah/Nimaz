package com.arshadshah.nimaz.testing

import android.app.Application
import android.app.Service
import com.arshadshah.nimaz.core.util.BootReceiver
import com.arshadshah.nimaz.core.util.BootReceiver_GeneratedInjector
import com.arshadshah.nimaz.data.announcement.NimazMessagingService
import com.arshadshah.nimaz.data.announcement.NimazMessagingService_GeneratedInjector
import com.arshadshah.nimaz.data.audio.AdhanDownloadService
import com.arshadshah.nimaz.data.audio.AdhanDownloadService_GeneratedInjector
import com.arshadshah.nimaz.data.audio.AdhanPlaybackService
import com.arshadshah.nimaz.data.audio.AdhanPlaybackService_GeneratedInjector
import com.arshadshah.nimaz.data.audio.QuranAudioService
import com.arshadshah.nimaz.data.audio.QuranAudioService_GeneratedInjector
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.internal.builders.ServiceComponentBuilder
import dagger.hilt.android.internal.managers.ServiceComponentManager
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager

/**
 * The smallest thing that satisfies Hilt for `:app`'s receiver and its three services.
 *
 * `@AndroidEntryPoint` is not a no-op at run time. The Hilt Gradle plugin rewrites
 * `BootReceiver.onReceive` and every `Service.onCreate` to inject first, and the generated base
 * class then asks the **application** for a component. With an ordinary `android.app.Application`
 * that throws before a line of the subject runs:
 *
 *     Hilt BroadcastReceiver must be attached to an @HiltAndroidApp Application
 *
 * which is why the receiver, the two adhan services and the Quran audio service had no unit tests
 * at all despite being where every notification and every sound the app makes comes from.
 *
 * `HiltTestApplication` would satisfy it and is the wrong tool: it builds the **real** singleton
 * graph, so a test about an `AlarmManager` call ends up opening a Room database from a private
 * content artifact this machine cannot fetch. This class implements the two interfaces Hilt
 * actually reaches for — [GeneratedComponentManager] for the receiver, and
 * [ServiceComponentBuilder] via [ServiceComponentManager] for the services — and hands back
 * doubles the test chose. It is marked [GeneratedComponent] because `EntryPoints.get` walks the
 * chain application → component and refuses anything at the end of it that does not say it is
 * one.
 *
 * Set the relevant lambda in `@Before`; each subject reads it once, on first injection.
 */
class TestEntryPointApplication : Application(), GeneratedComponentManager<Any> {

    override fun generatedComponent(): Any = Injector

    /**
     * One object playing three parts: the singleton component (for the receiver), the
     * `ServiceComponentBuilderEntryPoint` Hilt looks up on it, and the service component that
     * builder returns. Hilt only ever casts to the interface it needs next, so a single object
     * implementing all of them is enough and keeps the doubles in one place.
     */
    object Injector :
        GeneratedComponent,
        ServiceComponent,
        ServiceComponentManager.ServiceComponentBuilderEntryPoint,
        ServiceComponentBuilder,
        BootReceiver_GeneratedInjector,
        AdhanPlaybackService_GeneratedInjector,
        AdhanDownloadService_GeneratedInjector,
        QuranAudioService_GeneratedInjector,
        NimazMessagingService_GeneratedInjector {

        var bootReceiver: (BootReceiver) -> Unit = {}
        var adhanPlayback: (AdhanPlaybackService) -> Unit = {}
        var adhanDownload: (AdhanDownloadService) -> Unit = {}
        var quranAudio: (QuranAudioService) -> Unit = {}
        var messaging: (NimazMessagingService) -> Unit = {}

        /** Drop every double, so one test class cannot leak its stubs into the next. */
        fun reset() {
            bootReceiver = {}
            adhanPlayback = {}
            adhanDownload = {}
            quranAudio = {}
            messaging = {}
        }

        override fun serviceComponentBuilder(): ServiceComponentBuilder = this
        override fun service(service: Service): ServiceComponentBuilder = this
        override fun build(): ServiceComponent = this

        override fun injectBootReceiver(instance: BootReceiver) = bootReceiver(instance)
        override fun injectAdhanPlaybackService(instance: AdhanPlaybackService) =
            adhanPlayback(instance)

        override fun injectAdhanDownloadService(instance: AdhanDownloadService) =
            adhanDownload(instance)

        override fun injectQuranAudioService(instance: QuranAudioService) = quranAudio(instance)
        override fun injectNimazMessagingService(instance: NimazMessagingService) =
            messaging(instance)
    }
}
