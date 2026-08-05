package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.SearchLocation

/**
 * Where the device thinks it is, and what a place name means — the two things a ViewModel needs
 * from Android's location stack, stated without naming Android.
 *
 * `LocationViewModel` and `OnboardingViewModel` each built a `Geocoder` and a
 * `FusedLocationProviderClient` from an injected `@ApplicationContext`, wrapped both in
 * `suspendCancellableCoroutine`, and duplicated the address→name flattening. Neither ViewModel
 * could be **constructed** on the JVM as a result, which is why both have zero tests — and the
 * location-search debounce shipped untested for exactly that reason.
 *
 * The permission check lives on [PermissionChecker] rather than here: a repository that silently
 * returns null when a permission is missing cannot be told apart from one that looked and found
 * nothing, and those need different UI.
 */
interface DeviceLocationRepository {

    /**
     * The device's current coordinates, or null when the fix comes back empty.
     *
     * Throws whatever the platform throws — a caller that wants to distinguish "no fix" from
     * "location services are off" needs the exception, and swallowing it here is what made the
     * two look identical.
     */
    suspend fun currentCoordinates(): Coordinates?

    /** Places matching a typed query, best match first. Empty when nothing matches. */
    suspend fun search(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): List<SearchLocation>

    /** The display name for a point, or null when the lookup yields nothing usable. */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String?

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 10
    }
}

/** A point on the earth. Deliberately not `android.location.Location`. */
data class Coordinates(val latitude: Double, val longitude: Double)

/**
 * Whether a runtime permission has been granted.
 *
 * `ContextCompat.checkSelfPermission` was called from two ViewModels and one screen, three times
 * over, each with its own idea of which permissions count as "location". One implementation, and
 * a fake in tests.
 */
interface PermissionChecker {

    /** True when either fine **or** coarse location has been granted. */
    fun hasLocationPermission(): Boolean

    /** True when notifications may be posted — always true below Android 13. */
    fun hasNotificationPermission(): Boolean
}

/**
 * Whether the app is exempt from battery optimisation, and how to ask.
 *
 * `getBatteryOptimizationIntent(): Intent` was a **public ViewModel method returning an
 * `android.content.Intent`**, duplicated verbatim in `HomeViewModel` and `OnboardingViewModel`.
 * A ViewModel handing a screen an Intent is the UI layer asking the state layer for a piece of
 * Android — the dependency arrow pointing the wrong way, twice.
 */
interface PowerSettings {

    /** True when the app is already exempt, so nothing needs to be asked. */
    fun isIgnoringBatteryOptimizations(): Boolean
}
