package com.arshadshah.nimaz.core.navigation

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Navigation routes are `@Serializable` because type-safe Compose Navigation
 * encodes them into the back-stack. If a route stops serializing or a typed
 * argument is dropped, navigation to that destination breaks at runtime. These
 * tests pin the route contract (round-trip + argument preservation + defaults)
 * and the bottom-navigation wiring.
 */
class RouteSerializationTest {

    private val json = Json

    private inline fun <reified T : Route> roundTrip(route: T): Route {
        val encoded = json.encodeToString(Route.serializer(), route)
        return json.decodeFromString(Route.serializer(), encoded)
    }

    @Test
    fun `object routes round-trip to the same singleton`() {
        val objects = listOf(
            Route.Home, Route.Quran, Route.Tasbih, Route.QiblaNav, Route.More,
            Route.PrayerTimes, Route.ZakatCalculator, Route.Qibla, Route.Settings,
            Route.Onboarding, Route.GlobalSearch, Route.KhatamList
        )
        for (route in objects) {
            assertThat(roundTrip(route)).isEqualTo(route)
        }
    }

    @Test
    fun `parameterized routes preserve their arguments`() {
        assertThat(roundTrip(Route.QuranReader(surahNumber = 2, ayahNumber = 255)))
            .isEqualTo(Route.QuranReader(2, 255))
        assertThat(roundTrip(Route.HadithBook(bookId = "bukhari")))
            .isEqualTo(Route.HadithBook("bukhari"))
        assertThat(roundTrip(Route.TasbihCounter(presetId = 7L)))
            .isEqualTo(Route.TasbihCounter(7L))
        assertThat(roundTrip(Route.IslamicMonth(month = 9, year = 1446)))
            .isEqualTo(Route.IslamicMonth(9, 1446))
        assertThat(roundTrip(Route.KhatamDetail(khatamId = 3L)))
            .isEqualTo(Route.KhatamDetail(3L))
        assertThat(roundTrip(Route.AsmaUlHusnaDetail(nameId = 42)))
            .isEqualTo(Route.AsmaUlHusnaDetail(42))
    }

    @Test
    fun `default route arguments are applied and survive a round-trip`() {
        // QuranReader.ayahNumber and PrayerTracker.initialTab have defaults.
        assertThat(Route.QuranReader(surahNumber = 1).ayahNumber).isEqualTo(1)
        assertThat(Route.PrayerTracker().initialTab).isEqualTo(0)
        assertThat(Route.TasbihCounter().presetId).isNull()

        val decoded = roundTrip(Route.QuranReader(surahNumber = 5))
        assertThat(decoded).isEqualTo(Route.QuranReader(5, 1))
    }

    @Test
    fun `nullable route arguments round-trip when present and absent`() {
        assertThat(roundTrip(Route.TasbihCounter(presetId = null)))
            .isEqualTo(Route.TasbihCounter(null))
        assertThat(roundTrip(Route.TasbihCounter(presetId = 99L)))
            .isEqualTo(Route.TasbihCounter(99L))
    }

    @Test
    fun `bottom navigation exposes the five main destinations in order`() {
        assertThat(BottomNavDestination.values().map { it.title })
            .containsExactly("Home", "Quran", "Tasbih", "Qibla", "More").inOrder()
        assertThat(BottomNavDestination.HOME.route).isEqualTo(Route.Home)
        assertThat(BottomNavDestination.QURAN.route).isEqualTo(Route.Quran)
        assertThat(BottomNavDestination.QIBLA.route).isEqualTo(Route.QiblaNav)
    }
}
