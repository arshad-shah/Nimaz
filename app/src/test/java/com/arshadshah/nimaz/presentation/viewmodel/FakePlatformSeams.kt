package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.core.text.StringProvider
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.repository.PermissionChecker
import com.arshadshah.nimaz.domain.repository.PowerSettings
import com.arshadshah.nimaz.domain.repository.WidgetRefresher
import com.arshadshah.nimaz.domain.repository.settings.ZakatSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
 * A writable [ZakatSettings] — eight members, not the two hundred of `SettingsRepository`.
 *
 * Shared rather than duplicated per test file, because a private copy in each one collides the
 * moment two live in the same package. Two very different tests lean on it: the fidya total in
 * `FastingViewModel` only needs the currency to be *some* fixed code, while the zakat settings
 * screen needs writes to be **observable** — the whole point of that screen is that a value
 * written through the seam comes back through the flow, and a `flowOf(…)` fake makes a
 * ViewModel that never persists anything look identical to one that does.
 */
class FakeZakatSettings(
    code: String = "USD",
    gold: Double = 65.0,
    silver: Double = 0.80,
    nisabType: String = NisabType.DEFAULT.name,
) : ZakatSettings {
    private val goldFlow = MutableStateFlow(gold)
    private val silverFlow = MutableStateFlow(silver)
    private val currencyFlow = MutableStateFlow(code)
    private val nisabFlow = MutableStateFlow(nisabType)

    override val zakatGoldPricePerGram: Flow<Double> = goldFlow
    override suspend fun setZakatGoldPricePerGram(pricePerGram: Double) {
        goldFlow.value = pricePerGram
    }

    override val zakatSilverPricePerGram: Flow<Double> = silverFlow
    override suspend fun setZakatSilverPricePerGram(pricePerGram: Double) {
        silverFlow.value = pricePerGram
    }

    override val zakatCurrency: Flow<String> = currencyFlow
    override suspend fun setZakatCurrency(currency: String) {
        currencyFlow.value = currency
    }

    override val zakatNisabType: Flow<String> = nisabFlow
    override suspend fun setZakatNisabType(type: String) {
        nisabFlow.value = type
    }
}
