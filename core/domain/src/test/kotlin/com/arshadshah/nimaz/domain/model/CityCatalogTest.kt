package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The curated city list behind the Location screen, and the two functions that shape it.
 *
 * The catalogue is hand-maintained data, which is exactly why it needs assertions: nothing about
 * adding a city to a list of forty fails a build, and three of the ways to get it wrong are only
 * visible on a device.
 *
 * **A duplicate key is fatal.** [SearchLocation.key] is the identity a lazy list is keyed by, and
 * it is built from the coordinates rather than the name on purpose — the same city name recurs
 * across countries. Two curated rows sharing coordinates crashes the list rather than showing a
 * duplicate, the same failure `NimazScrollSpyIndex` carries a test for.
 *
 * **A missing flag is a hole in a row.** These flags are the one sanctioned emoji use in the app
 * (`docs/ARCHITECTURE.md` §7/§9), so a city added without one leaves a gap where every other row
 * has a glyph.
 *
 * **A region with no cities is a header with nothing under it.** [groupCitiesByRegion] drops
 * empty groups for that reason, and the catalogue is expected to reach all five.
 *
 * Moved here from `:app/src/test`, where it was testing another module's code: it predates the
 * split and stayed behind when `SearchLocation` came to `:core:domain`.
 */
class CityCatalogTest {

    @Test
    fun `every curated city carries a region and a flag`() {
        assertThat(defaultPopularCities).isNotEmpty()
        defaultPopularCities.forEach { city ->
            assertThat(city.region).isNotNull()
            assertThat(city.flag).isNotNull()
            assertThat(city.flag).isNotEmpty()
        }
    }

    @Test
    fun `no two curated cities share a lazy-list key`() {
        // A repeated key is not a duplicate row; it is a crash.
        val keys = defaultPopularCities.map { it.key }
        assertThat(keys).containsNoDuplicates()
    }

    @Test
    fun `the key is the coordinates, so two cities of the same name stay distinct`() {
        val hyderabadIndia = SearchLocation("Hyderabad", "India", 17.3850, 78.4867)
        val hyderabadPakistan = SearchLocation("Hyderabad", "Pakistan", 25.3960, 68.3578)

        assertThat(hyderabadIndia.key).isNotEqualTo(hyderabadPakistan.key)
    }

    @Test
    fun `every curated city sits somewhere real`() {
        defaultPopularCities.forEach { city ->
            assertThat(city.latitude).isAtLeast(-90.0)
            assertThat(city.latitude).isAtMost(90.0)
            assertThat(city.longitude).isAtLeast(-180.0)
            assertThat(city.longitude).isAtMost(180.0)
            assertThat(city.name).isNotEmpty()
            assertThat(city.country).isNotEmpty()
        }
    }

    @Test
    fun `the catalogue reaches all five regions`() {
        val regions = defaultPopularCities.mapNotNull { it.region }.toSet()
        assertThat(regions).containsExactlyElementsIn(CityRegion.entries)
    }

    // ---- Grouping ----

    @Test
    fun `groups come back in the region order, and none of them is empty`() {
        val grouped = groupCitiesByRegion(defaultPopularCities)

        assertThat(grouped.map { it.first.order }).isInOrder()
        grouped.forEach { assertThat(it.second).isNotEmpty() }
    }

    @Test
    fun `grouping keeps every city that has a region`() {
        val grouped = groupCitiesByRegion(defaultPopularCities)

        assertThat(grouped.sumOf { it.second.size })
            .isEqualTo(defaultPopularCities.count { it.region != null })
    }

    @Test
    fun `a city with no region is not given one`() {
        // Geocoder and recent-search results arrive without a region; they must not be filed
        // under whichever group happens to be first.
        val fromGeocoder = SearchLocation("Somewhere", "Nowhere", 1.0, 1.0, region = null)

        val grouped = groupCitiesByRegion(listOf(fromGeocoder) + defaultPopularCities)

        assertThat(grouped.flatMap { it.second }).doesNotContain(fromGeocoder)
    }

    @Test
    fun `grouping nothing produces no headers`() {
        assertThat(groupCitiesByRegion(emptyList())).isEmpty()
    }

    // ---- Filtering ----

    @Test
    fun `no region means the whole catalogue, in its own order`() {
        assertThat(citiesForRegion(defaultPopularCities, null))
            .isEqualTo(defaultPopularCities)
    }

    @Test
    fun `a region narrows to that region alone`() {
        val result = citiesForRegion(defaultPopularCities, CityRegion.EUROPE)

        assertThat(result).isNotEmpty()
        assertThat(result.map { it.region }.toSet()).containsExactly(CityRegion.EUROPE)
    }

    @Test
    fun `filtering a list with none of that region gives nothing, not everything`() {
        val onlyEurope = defaultPopularCities.filter { it.region == CityRegion.EUROPE }

        assertThat(citiesForRegion(onlyEurope, CityRegion.AFRICA)).isEmpty()
    }
}
