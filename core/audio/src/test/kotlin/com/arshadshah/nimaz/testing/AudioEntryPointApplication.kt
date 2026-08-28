package com.arshadshah.nimaz.testing

import android.app.Application
import android.app.Service
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
 * The smallest thing that satisfies Hilt for this module's three foreground services.
 *
 * **The audio half of `:app`'s `TestEntryPointApplication`**, which stayed there with the two
 * broadcast receivers and the FCM service. It had to be split rather than shared: the class names
 * the generated `*_GeneratedInjector` interfaces, and those are generated into whichever module
 * declares the service — so one copy cannot serve both sides of the boundary, and a `:core:*`
 * module cannot see `:app`'s in any case.
 *
 * `@AndroidEntryPoint` is not a no-op at run time. The Hilt Gradle plugin rewrites every
 * `Service.onCreate` to inject first, and the generated base class then asks the **application**
 * for a component. With an ordinary `android.app.Application` that throws before a line of the
 * subject runs, which is why these three services had no unit tests at all until
 * `TestEntryPointApplication` existed.
 *
 * `HiltTestApplication` would satisfy Hilt and is the wrong tool: it builds the **real** singleton
 * graph, so a test about a notification ends up opening a Room database from a private content
 * artifact this machine cannot fetch. This implements only the interfaces Hilt actually reaches
 * for — [GeneratedComponentManager], and [ServiceComponentBuilder] via [ServiceComponentManager] —
 * and hands back doubles the test chose. It is marked [GeneratedComponent] because
 * `EntryPoints.get` walks application → component and refuses anything at the end of that chain
 * that does not say it is one.
 *
 * Set the relevant lambda in `@Before`; each subject reads it once, on first injection.
 */
class AudioEntryPointApplication : Application(), GeneratedComponentManager<Any> {

    override fun generatedComponent(): Any = Injector

    /**
     * One object playing three parts: the singleton component, the
     * `ServiceComponentBuilderEntryPoint` Hilt looks up on it, and the service component that
     * builder returns. Hilt only ever casts to the interface it needs next.
     */
    object Injector :
        GeneratedComponent,
        ServiceComponent,
        ServiceComponentManager.ServiceComponentBuilderEntryPoint,
        ServiceComponentBuilder,
        AdhanPlaybackService_GeneratedInjector,
        AdhanDownloadService_GeneratedInjector,
        QuranAudioService_GeneratedInjector {

        var adhanPlayback: (AdhanPlaybackService) -> Unit = {}
        var adhanDownload: (AdhanDownloadService) -> Unit = {}
        var quranAudio: (QuranAudioService) -> Unit = {}

        /** Drop every double, so one test class cannot leak its stubs into the next. */
        fun reset() {
            adhanPlayback = {}
            adhanDownload = {}
            quranAudio = {}
        }

        override fun serviceComponentBuilder(): ServiceComponentBuilder = this
        override fun service(service: Service): ServiceComponentBuilder = this
        override fun build(): ServiceComponent = this

        override fun injectAdhanPlaybackService(instance: AdhanPlaybackService) =
            adhanPlayback(instance)

        override fun injectAdhanDownloadService(instance: AdhanDownloadService) =
            adhanDownload(instance)

        override fun injectQuranAudioService(instance: QuranAudioService) = quranAudio(instance)
    }
}
