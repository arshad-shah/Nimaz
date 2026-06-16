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
 *
 * Each concrete route is round-tripped via its own (reified) serializer — the
 * `Route` parent interface is intentionally not `@Serializable`, only its
 * subtypes are, matching how Navigation encodes typed destinations.
 */
class RouteSerializationTest {

    private val json = Json

    private inline fun <reified T : Route> assertRoundTrips(route: T) {
        val decoded: T = json.decodeFromString(json.encodeToString(route))
        assertThat(decoded).isEqualTo(route)
    }

    @Test
    fun `object routes round-trip to the same singleton`() {
        assertRoundTrips(Route.Home)
        assertRoundTrips(Route.Quran)
        assertRoundTrips(Route.Tasbih)
        assertRoundTrips(Route.QiblaNav)
        assertRoundTrips(Route.More)
        assertRoundTrips(Route.PrayerTimes)
        assertRoundTrips(Route.ZakatCalculator)
        assertRoundTrips(Route.Qibla)
        assertRoundTrips(Route.Settings)
        assertRoundTrips(Route.Onboarding)
        assertRoundTrips(Route.GlobalSearch)
        assertRoundTrips(Route.KhatamList)
    }

    @Test
    fun `parameterized routes preserve their arguments`() {
        assertRoundTrips(Route.QuranReader(surahNumber = 2, ayahNumber = 255))
        assertRoundTrips(Route.HadithBook(bookId = "bukhari"))
        assertRoundTrips(Route.HadithChapter(bookId = "bukhari", chapterId = "1"))
        assertRoundTrips(Route.TasbihCounter(presetId = 7L))
        assertRoundTrips(Route.IslamicMonth(month = 9, year = 1446))
        assertRoundTrips(Route.KhatamDetail(khatamId = 3L))
        assertRoundTrips(Route.AsmaUlHusnaDetail(nameId = 42))
        assertRoundTrips(Route.LicenseDetail(libraryHashCode = 123))
    }

    @Test
    fun `default route arguments are applied and survive a round-trip`() {
        assertThat(Route.QuranReader(surahNumber = 1).ayahNumber).isEqualTo(1)
        assertThat(Route.PrayerTracker().initialTab).isEqualTo(0)
        assertThat(Route.TasbihCounter().presetId).isNull()

        // A QuranReader created with only the surah keeps its default ayah of 1.
        assertRoundTrips(Route.QuranReader(surahNumber = 5))
        assertThat(Route.QuranReader(surahNumber = 5)).isEqualTo(Route.QuranReader(5, 1))
    }

    @Test
    fun `nullable route arguments round-trip when present and absent`() {
        assertRoundTrips(Route.TasbihCounter(presetId = null))
        assertRoundTrips(Route.TasbihCounter(presetId = 99L))
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
