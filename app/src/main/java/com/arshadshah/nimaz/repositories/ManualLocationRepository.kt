package com.arshadshah.nimaz.repositories

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.arshadshah.nimaz.viewModel.ViewModelLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "Nimaz: ManualLocationRepo"
        private const val MAX_RETRIES = 2
        private const val RESULTS_LIMIT = 1
    }

    private val geocoder by lazy {
        Geocoder(context, Locale.getDefault())
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<Location> = withContext(
        Dispatchers.IO
    ) {
        ViewModelLogger.d(TAG, "🔄 Reverse geocoding: ($latitude, $longitude)")
        try {
            validateCoordinates(latitude, longitude)

            for (attempt in 1..MAX_RETRIES) {
                ViewModelLogger.d(TAG, "🔄 Attempt $attempt of $MAX_RETRIES")
                try {
                    val addresses = geocoder.getFromLocation(latitude, longitude, RESULTS_LIMIT)

                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val locationName = determineLocationName(address)
                        ViewModelLogger.d(TAG, "✅ Location found: $locationName")
                        return@withContext Result.success(
                            Location(
                                latitude = latitude,
                                longitude = longitude,
                                locationName = locationName
                            )
                        ) // Exit as soon as the result is found
                    }

                    ViewModelLogger.w(TAG, "⚠️ No addresses found")
                    return@withContext Result.failure(Exception("No addresses found"))
                } catch (e: Exception) {
                    ViewModelLogger.e(TAG, "❌ Attempt $attempt failed: ${e.message}")
                    if (attempt == MAX_RETRIES) throw e
                    continue
                }
            }

            Result.failure(Exception("Unable to find address after $MAX_RETRIES attempts")) // Only reach here if all retries fail
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Reverse geocoding failed", e)
            Result.failure(e)
        }
    }

    suspend fun forwardGeocode(cityName: String): Result<Location> = withContext(Dispatchers.IO) {
        ViewModelLogger.d(TAG, "🔍 Forward geocoding: '$cityName'")
        try {
            if (cityName.isBlank()) {
                ViewModelLogger.e(TAG, "❌ Empty city name provided")
                return@withContext Result.failure(IllegalArgumentException("City name cannot be empty"))
            }

            val normalizedCityName = cityName.trim()
            ViewModelLogger.d(TAG, "🔄 Normalized city name: '$normalizedCityName'")

            for (attempt in 1..MAX_RETRIES) {
                ViewModelLogger.d(TAG, "🔄 Attempt $attempt of $MAX_RETRIES")
                try {
                    val addresses = geocoder.getFromLocationName(normalizedCityName, RESULTS_LIMIT)

                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val locationName = determineLocationName(address)
                        validateCoordinates(address.latitude, address.longitude)

                        ViewModelLogger.d(
                            TAG,
                            "✅ Location found: $locationName (${address.latitude}, ${address.longitude})"
                        )
                        return@withContext Result.success(
                            Location(
                                latitude = address.latitude,
                                longitude = address.longitude,
                                locationName = locationName
                            )
                        ) // Exit as soon as the result is found
                    }

                    ViewModelLogger.w(TAG, "⚠️ No locations found")
                } catch (e: Exception) {
                    ViewModelLogger.e(TAG, "❌ Attempt $attempt failed: ${e.message}")
                    if (attempt == MAX_RETRIES) throw e
                    continue
                }
            }

            Result.failure(Exception("Unable to find location after $MAX_RETRIES attempts")) // Only reach here if all retries fail
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Forward geocoding failed", e)
            Result.failure(e)
        }
    }

    private fun validateCoordinates(latitude: Double, longitude: Double) {
        ViewModelLogger.d(TAG, "✅ Validating coordinates: ($latitude, $longitude)")
        if (latitude !in -90.0..90.0) {
            ViewModelLogger.e(TAG, "❌ Invalid latitude: $latitude")
            throw IllegalArgumentException("Invalid latitude: $latitude")
        }
        if (longitude !in -180.0..180.0) {
            ViewModelLogger.e(TAG, "❌ Invalid longitude: $longitude")
            throw IllegalArgumentException("Invalid longitude: $longitude")
        }
    }

    private fun determineLocationName(address: Address): String {
        ViewModelLogger.d(TAG, "🏷️ Determining location name from address")
        return listOfNotNull(
            address.locality,
            address.subAdminArea,
            address.adminArea,
            address.countryName
        ).firstOrNull() ?: run {
            ViewModelLogger.e(TAG, "❌ No valid location name components found")
            throw IllegalStateException("No valid location name found")
        }.also {
            ViewModelLogger.d(TAG, "✅ Location name determined: $it")
        }
    }
}

data class Location(
    val latitude: Double,
    val longitude: Double,
    val locationName: String
)