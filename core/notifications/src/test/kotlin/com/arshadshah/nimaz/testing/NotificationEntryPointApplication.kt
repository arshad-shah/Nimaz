package com.arshadshah.nimaz.testing

import android.app.Application
import com.arshadshah.nimaz.core.util.PrayerAlarmReceiver
import com.arshadshah.nimaz.core.util.PrayerAlarmReceiver_GeneratedInjector
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager

/**
 * The smallest thing that satisfies Hilt for [PrayerAlarmReceiver].
 *
 * **The notifications half of `:app`'s `TestEntryPointApplication`**, which keeps `BootReceiver`
 * and the FCM service; `AudioEntryPointApplication` in `:core:audio` is the third. Each has to be
 * its own class rather than one shared: it names the generated `*_GeneratedInjector` interface,
 * and that is generated into whichever module declares the receiver.
 *
 * `@AndroidEntryPoint` is not a no-op at run time. The Hilt Gradle plugin rewrites
 * `onReceive` to inject first, and the generated base class then asks the **application** for a
 * component — so an ordinary `android.app.Application` throws *"Hilt BroadcastReceiver must be
 * attached to an @HiltAndroidApp Application"* before a line of the subject runs. That is why the
 * receiver where every prayer notification is produced had no unit tests at all until this
 * existed.
 *
 * `HiltTestApplication` would satisfy Hilt and is the wrong tool: it builds the **real** singleton
 * graph, so a test about an `AlarmManager` call ends up opening a Room database from a private
 * content artifact this machine cannot fetch. This implements only [GeneratedComponentManager],
 * which is what Hilt reaches for, and hands back the double the test chose. It is marked
 * [GeneratedComponent] because `EntryPoints.get` walks application → component and refuses
 * anything at the end of that chain that does not say it is one.
 *
 * There is no `ServiceComponentBuilder` here, unlike the other two: this module declares no
 * `Service`.
 */
class NotificationEntryPointApplication : Application(), GeneratedComponentManager<Any> {

    override fun generatedComponent(): Any = Injector

    object Injector : GeneratedComponent, PrayerAlarmReceiver_GeneratedInjector {

        var prayerAlarmReceiver: (PrayerAlarmReceiver) -> Unit = {}

        /** Drop the double, so one test class cannot leak its stubs into the next. */
        fun reset() {
            prayerAlarmReceiver = {}
        }

        override fun injectPrayerAlarmReceiver(instance: PrayerAlarmReceiver) =
            prayerAlarmReceiver(instance)
    }
}
