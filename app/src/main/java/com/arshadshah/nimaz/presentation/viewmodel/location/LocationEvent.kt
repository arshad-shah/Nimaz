package com.arshadshah.nimaz.presentation.viewmodel.location

sealed interface LocationEvent {
    data class UpdateSearchQuery(val query: String) : LocationEvent
    data object Search : LocationEvent
    data object ClearSearch : LocationEvent
    data class SelectLocation(val location: SearchLocation) : LocationEvent
    data class SelectRegion(val region: CityRegion?) : LocationEvent
    data object UseCurrentGpsLocation : LocationEvent
    data object LoadCurrentLocation : LocationEvent
    data object DismissError : LocationEvent
}
