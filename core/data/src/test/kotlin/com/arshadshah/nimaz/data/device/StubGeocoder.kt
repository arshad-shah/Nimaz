package com.arshadshah.nimaz.data.device

import android.location.Address
import android.location.Geocoder
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.IOException

/**
 * A `Geocoder` a test can program.
 *
 * Robolectric's own `ShadowGeocoder` keeps its answers on the *instance*, and
 * [AndroidDeviceLocationRepository] constructs its own `Geocoder` inside a private function —
 * so there is no instance for a test to reach. This one keeps them statically, and covers the
 * blocking overloads as well as the listener ones so both sides of the API-33 split are
 * reachable.
 */
@Implements(Geocoder::class)
class StubGeocoder {

    @Implementation
    fun getFromLocation(latitude: Double, longitude: Double, maxResults: Int): List<Address> =
        answer()

    @Implementation
    fun getFromLocation(
        latitude: Double,
        longitude: Double,
        maxResults: Int,
        listener: Geocoder.GeocodeListener,
    ) = listener.onGeocode(answer())

    @Implementation
    fun getFromLocationName(locationName: String, maxResults: Int): List<Address> = answer()

    @Implementation
    fun getFromLocationName(
        locationName: String,
        maxResults: Int,
        listener: Geocoder.GeocodeListener,
    ) = listener.onGeocode(answer())

    private fun answer(): List<Address> {
        error?.let { throw IOException(it) }
        return addresses
    }

    companion object {
        var addresses: List<Address> = emptyList()

        /** Set to make every lookup throw, as a real geocoder does on a flaky connection. */
        var error: String? = null

        fun reset() {
            addresses = emptyList()
            error = null
        }
    }
}
