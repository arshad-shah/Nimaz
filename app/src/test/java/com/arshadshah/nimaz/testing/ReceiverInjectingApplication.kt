package com.arshadshah.nimaz.testing

import android.app.Application
import com.arshadshah.nimaz.core.util.BootReceiver
import com.arshadshah.nimaz.core.util.BootReceiver_GeneratedInjector
import dagger.hilt.internal.GeneratedComponentManager

/**
 * The smallest thing that satisfies Hilt's `BroadcastReceiver` injection.
 *
 * `@AndroidEntryPoint` on a receiver is not a no-op at run time: the Hilt Gradle plugin rewrites
 * `BootReceiver.onReceive` to call `super.onReceive` first, and the generated superclass fetches
 * the application, casts it to a [GeneratedComponentManager] and asks it for the component. With
 * an ordinary `android.app.Application` that throws
 *
 *     Hilt BroadcastReceiver must be attached to an @HiltAndroidApp Application
 *
 * before a line of `BootReceiver` runs — which is why the receiver has never had a unit test
 * despite being where every prayer notification is actually produced.
 *
 * `HiltTestApplication` would work and is the wrong tool here: it builds the **real** singleton
 * graph, which means a real Room database opened from the private content artifact, real
 * DataStore files, and a WorkManager. This class does the one thing Hilt actually asks for —
 * hand back an object that can inject a `BootReceiver` — and lets the test decide what the six
 * injected fields are.
 *
 * Set [inject] in `@Before`; the receiver reads it on its first `onReceive`.
 */
class ReceiverInjectingApplication : Application(), GeneratedComponentManager<Any> {

    override fun generatedComponent(): Any = Injector

    object Injector : BootReceiver_GeneratedInjector {
        /** Replaced per test. Default is a deliberate no-op so a misconfigured test fails loudly
         *  on an uninitialised `lateinit` rather than silently using stale doubles. */
        var inject: (BootReceiver) -> Unit = {}

        override fun injectBootReceiver(bootReceiver: BootReceiver) = inject(bootReceiver)
    }
}
