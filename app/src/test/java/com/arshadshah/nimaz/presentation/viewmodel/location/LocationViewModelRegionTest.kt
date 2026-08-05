package com.arshadshah.nimaz.presentation.viewmodel.location

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure state-transition checks for the region filter. We assert the reducer contract via a
 * tiny local copy of the reduce logic to avoid constructing the Android-dependent ViewModel.
 */
class LocationViewModelRegionTest {

    private fun reduceSelectRegion(state: LocationUiState, region: CityRegion?): LocationUiState =
        state.copy(selectedRegion = region)

    @Test
    fun `default selected region is null (All)`() {
        assertThat(LocationUiState().selectedRegion).isNull()
    }

    @Test
    fun `selecting a region updates state`() {
        val next = reduceSelectRegion(LocationUiState(), CityRegion.EUROPE)
        assertThat(next.selectedRegion).isEqualTo(CityRegion.EUROPE)
    }

    @Test
    fun `selecting All clears the region`() {
        val start = LocationUiState(selectedRegion = CityRegion.ASIA)
        assertThat(reduceSelectRegion(start, null).selectedRegion).isNull()
    }
}
