package com.arshadshah.nimaz.presentation.viewmodel.location

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import com.arshadshah.nimaz.domain.model.CityRegion
import com.arshadshah.nimaz.domain.model.citiesForRegion
import com.arshadshah.nimaz.domain.model.defaultPopularCities
import com.arshadshah.nimaz.domain.model.groupCitiesByRegion
import com.arshadshah.nimaz.presentation.screens.settings.formatCoordinates

class LocationCatalogTest {

    @Test
    fun `every curated city has a region and a flag`() {
        assertThat(defaultPopularCities).isNotEmpty()
        defaultPopularCities.forEach { city ->
            assertThat(city.region).isNotNull()
            assertThat(city.flag).isNotNull()
            assertThat(city.flag).isNotEmpty()
        }
    }

    @Test
    fun `catalogue covers all five regions`() {
        val regions = defaultPopularCities.mapNotNull { it.region }.toSet()
        assertThat(regions).containsExactlyElementsIn(CityRegion.entries)
    }

    @Test
    fun `groupCitiesByRegion orders groups by region order`() {
        val grouped = groupCitiesByRegion(defaultPopularCities)
        val orders = grouped.map { it.first.order }
        assertThat(orders).isInOrder()
        // no empty groups
        grouped.forEach { assertThat(it.second).isNotEmpty() }
    }

    @Test
    fun `citiesForRegion returns all when region is null`() {
        assertThat(citiesForRegion(defaultPopularCities, null))
            .isEqualTo(defaultPopularCities)
    }

    @Test
    fun `citiesForRegion filters to a single region`() {
        val result = citiesForRegion(defaultPopularCities, CityRegion.EUROPE)
        assertThat(result).isNotEmpty()
        assertThat(result.map { it.region }.toSet()).containsExactly(CityRegion.EUROPE)
    }

    @Test
    fun `formatCoordinates uses correct hemispheres`() {
        assertThat(formatCoordinates(21.4225, 39.8262)).isEqualTo("21.4225° N, 39.8262° E")
        assertThat(formatCoordinates(-6.2088, 106.8456)).isEqualTo("6.2088° S, 106.8456° E")
        assertThat(formatCoordinates(40.7128, -74.0060)).isEqualTo("40.7128° N, 74.0060° W")
        assertThat(formatCoordinates(-34.6037, -58.3816)).isEqualTo("34.6037° S, 58.3816° W")
    }
}
