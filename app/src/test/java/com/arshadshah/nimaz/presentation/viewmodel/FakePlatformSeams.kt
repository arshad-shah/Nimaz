package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.core.text.StringProvider
import com.arshadshah.nimaz.domain.repository.PermissionChecker
import com.arshadshah.nimaz.domain.repository.PowerSettings
import com.arshadshah.nimaz.domain.repository.WidgetRefresher

/**
 * The platform seams ViewModels take instead of an `@ApplicationContext`.
 *
 * The point of the extraction is visible here: these fakes are a few lines each, where the
 * alternative was a Robolectric `ApplicationContext` for a ViewModel that wanted four strings
 * and a permission bit.
 */
class FakeStringProvider(
    private val format: (Int, List<Any>) -> String = { id, args ->
        if (args.isEmpty()) "string:$id" else "string:$id(${args.joinToString()})"
    }
) : StringProvider {
    override fun get(id: Int, vararg args: Any): String = format(id, args.toList())
    override fun quantity(id: Int, count: Int, vararg args: Any): String =
        format(id, listOf(count) + args.toList())
}

class FakePermissionChecker(
    private val location: Boolean = true,
    private val notification: Boolean = true,
) : PermissionChecker {
    override fun hasLocationPermission() = location
    override fun hasNotificationPermission() = notification
}

class FakePowerSettings(private val exempt: Boolean = true) : PowerSettings {
    override fun isIgnoringBatteryOptimizations() = exempt
}

class RecordingWidgetRefresher : WidgetRefresher {
    var refreshCount = 0
        private set

    override fun refreshPrayerTracker() {
        refreshCount++
    }
}
