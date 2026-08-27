package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * One answer to "has the reader set a location?", because the codebase had four.
 *
 * Onboarding can be skipped, and the location permission can be denied, so `(0, 0)` is very
 * reachable. Twenty sites decided what that meant, four different ways:
 *
 * | shape | where | effect |
 * |---|---|---|
 * | `lat == 0.0 && lng == 0.0` | `PrayerNotificationScheduler`, `NextWorshipResolver` | bail |
 * | `lat != 0.0 && lng != 0.0` | `HomeViewModel`, `LocationViewModel` | treat as unset |
 * | `lat != 0.0 \|\| lng != 0.0` | `QiblaViewModel`, `AppInitializer` | treat as set |
 * | per-axis substitution | both prayer-times ViewModels, `FastingViewModel`, 3 widget paths | **mix** |
 *
 * The last is the outright defect: `latitude = if (lat != 0.0) lat else 53.3498` tests each axis
 * on its own, so a reader **on the equator** keeps their longitude and gets Dublin's latitude,
 * and one **on the prime meridian** — Accra, Greenwich, much of Algeria — keeps their latitude
 * and gets Dublin's longitude. Prayer times for a place nobody is.
 *
 * The rest is a disagreement: at `(0, 5)` the qibla points somewhere while Home says there is no
 * location, and at `(0, 0)` the UI shows Dublin's times under the name "Dublin, Ireland" while
 * the scheduler quietly declines to fire a single notification.
 *
 * `(0, 0)` is Null Island, open water in the Gulf of Guinea, so treating it as the unset
 * sentinel costs nobody a real location — but it has to be *both* axes.
 */
class ResolvedLocationTest {

    @Test
    fun `an unset location falls back whole, not one axis at a time`() {
        val resolved = resolveLocation(latitude = 0.0, longitude = 0.0)

        assertThat(resolved.latitude).isEqualTo(FallbackLocation.LATITUDE)
        assertThat(resolved.longitude).isEqualTo(FallbackLocation.LONGITUDE)
        assertThat(resolved.isFallback).isTrue()
    }

    @Test
    fun `a reader on the equator keeps their own longitude`() {
        // Pontianak sits on the equator; Kismayo and Quito are within a few km of it. The old
        // per-axis test handed all of them Dublin's latitude — 53°N, a different hemisphere.
        val pontianak = resolveLocation(latitude = 0.0, longitude = 109.3333)

        assertThat(pontianak.isFallback).isFalse()
        assertThat(pontianak.latitude).isEqualTo(0.0)
        assertThat(pontianak.longitude).isEqualTo(109.3333)
    }

    @Test
    fun `a reader on the prime meridian keeps their own latitude`() {
        // The meridian runs through Accra, Tema, and a long stretch of Algeria, France and
        // eastern England. The old per-axis test gave every one of them Dublin's longitude.
        val onTheMeridian = resolveLocation(latitude = 5.6037, longitude = 0.0)

        assertThat(onTheMeridian.isFallback).isFalse()
        assertThat(onTheMeridian.latitude).isEqualTo(5.6037)
        assertThat(onTheMeridian.longitude).isEqualTo(0.0)
    }

    @Test
    fun `a set location is returned untouched`() {
        val jakarta = resolveLocation(-6.2088, 106.8456, "Jakarta, Indonesia")

        assertThat(jakarta.latitude).isEqualTo(-6.2088)
        assertThat(jakarta.longitude).isEqualTo(106.8456)
        assertThat(jakarta.name).isEqualTo("Jakarta, Indonesia")
        assertThat(jakarta.isFallback).isFalse()
    }

    @Test
    fun `the fallback carries its own name rather than borrowing a blank one`() {
        // `PrayerTimesViewModel` hardcoded the string "Dublin, Ireland" next to the coordinates,
        // in English, so the header asserted a city the reader had never chosen.
        assertThat(resolveLocation(0.0, 0.0).name).isEqualTo(FallbackLocation.NAME)
        assertThat(resolveLocation(0.0, 0.0, name = "").name).isEqualTo(FallbackLocation.NAME)
    }

    @Test
    fun `a set location with no name keeps its coordinates and reports no name`() {
        // Reverse geocoding can fail while the fix itself is good; that is not a reason to
        // relocate the reader to Dublin.
        val resolved = resolveLocation(21.4225, 39.8262, name = "  ")

        assertThat(resolved.latitude).isEqualTo(21.4225)
        assertThat(resolved.isFallback).isFalse()
        assertThat(resolved.name).isEmpty()
    }

    @Test
    fun `coordinates outside the globe are treated as unset`() {
        // A corrupted or wrongly-typed preference — the sync importer used to mistype six keys —
        // would otherwise reach the prayer-time calculator as a real position.
        listOf(
            91.0 to 0.5,
            -90.5 to 0.5,
            10.0 to 181.0,
            10.0 to -180.5,
            Double.NaN to 10.0,
            10.0 to Double.NaN,
        ).forEach { (lat, lng) ->
            assertThat(resolveLocation(lat, lng).isFallback).isTrue()
        }
    }

    @Test
    fun `the poles and the date line are inside the globe`() {
        assertThat(resolveLocation(90.0, 180.0).isFallback).isFalse()
        assertThat(resolveLocation(-90.0, -180.0).isFallback).isFalse()
    }

    @Test
    fun `isLocationSet agrees with the resolver`() {
        // The schedulers must keep *bailing* rather than falling back — an adhan for a city the
        // reader is not in is worse than no adhan — so they need the predicate, not the
        // resolved coordinates. The two must not drift apart again.
        listOf(
            0.0 to 0.0,
            91.0 to 10.0,
            Double.NaN to Double.NaN,
            0.0 to 109.3333,
            53.3498 to -6.2603,
        ).forEach { (lat, lng) ->
            assertThat(isLocationSet(lat, lng)).isEqualTo(!resolveLocation(lat, lng).isFallback)
        }
    }

    @Test
    fun `no site hardcodes the fallback coordinates any more`() {
        // The pair was written out as literals in seven places. Reads the sources directly, in
        // the shape of WidgetGlyphGuardTest; runs from the module dir.
        val offenders = java.io.File("src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.name == "PrayerModels.kt" } // where the constants now live
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    if ("53.3498" in line || "-6.2603" in line) {
                        "${file.path}:${index + 1}: ${line.trim()}"
                    } else {
                        null
                    }
                }
            }
            .toList()

        assertThat(offenders).isEmpty()
    }
}
