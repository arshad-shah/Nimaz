package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.core.text.StringProvider
import com.arshadshah.nimaz.domain.repository.PermissionChecker
import com.arshadshah.nimaz.domain.repository.PowerSettings
import com.arshadshah.nimaz.domain.repository.WidgetRefresher
import com.arshadshah.nimaz.domain.repository.settings.ZakatSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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

/**
 * A [ZakatSettings] fixed at one currency.
 *
 * The zakat currency is the app's only currency setting, so anything rendering money reads it —
 * `FastingViewModel` needs it for the fidya total. Shared rather than duplicated per test file,
 * because a private copy in each one collides the moment two live in the same package.
 */
class FakeZakatSettings(
    private val code: String = "USD",
    private val gold: Double = 65.0,
    private val silver: Double = 0.80,
) : ZakatSettings {
    override val zakatGoldPricePerGram: Flow<Double> = flowOf(gold)
    override suspend fun setZakatGoldPricePerGram(pricePerGram: Double) = Unit
    override val zakatSilverPricePerGram: Flow<Double> = flowOf(silver)
    override suspend fun setZakatSilverPricePerGram(pricePerGram: Double) = Unit
    override val zakatCurrency: Flow<String> = flowOf(code)
    override suspend fun setZakatCurrency(currency: String) = Unit
}
