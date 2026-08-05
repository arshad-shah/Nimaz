package com.arshadshah.nimaz.data.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.arshadshah.nimaz.domain.model.SearchLocation
import com.arshadshah.nimaz.domain.repository.Coordinates
import com.arshadshah.nimaz.domain.repository.DeviceLocationRepository
import com.arshadshah.nimaz.domain.repository.PermissionChecker
import com.arshadshah.nimaz.domain.repository.PowerSettings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.arshadshah.nimaz.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The one place `Geocoder` and `FusedLocationProviderClient` are constructed.
 *
 * Two ViewModels each built both from an injected `@ApplicationContext`, wrapped them in their own
 * `suspendCancellableCoroutine`, and flattened an `Address` into a display name with their own
 * copy of the same four-way fallback. That made both ViewModels impossible to construct in a JVM
 * test, which is why both had none.
 */
@Singleton
class AndroidDeviceLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeviceLocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Caller must have checked [PermissionChecker.hasLocationPermission] — the suppression says
     * the check is somewhere else, not that there isn't one.
     */
    @SuppressLint("MissingPermission")
    override suspend fun currentCoordinates(): Coordinates? =
        suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token,
            ).addOnSuccessListener { location ->
                continuation.resume(
                    location?.let { Coordinates(it.latitude, it.longitude) },
                )
            }.addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }

            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
        }

    override suspend fun search(query: String, limit: Int): List<SearchLocation> =
        withContext(ioDispatcher) {
            geocode(
                listener = { geocoder, callback ->
                    geocoder.getFromLocationName(query, limit, callback)
                },
                blocking = { geocoder -> geocoder.getFromLocationName(query, limit) },
            ).mapNotNull { address ->
                address.displayName()?.let { name ->
                    SearchLocation(
                        name = name,
                        country = address.countryName.orEmpty(),
                        latitude = address.latitude,
                        longitude = address.longitude,
                    )
                }
            }
        }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): String? =
        withContext(ioDispatcher) {
            geocode(
                listener = { geocoder, callback ->
                    geocoder.getFromLocation(latitude, longitude, 1, callback)
                },
                blocking = { geocoder -> geocoder.getFromLocation(latitude, longitude, 1) },
            ).firstOrNull()?.displayName()
        }

    /**
     * The API-33 split, once.
     *
     * Below Tiramisu the geocoder blocks and returns; from Tiramisu it takes a listener. The two
     * ViewModels wrote this branch out twice each — four copies of the same `Build.VERSION`
     * check. A geocoder throws on a transient network failure, which is why the whole thing is
     * inside `runCatching`: a failed lookup is an empty result, not a crash.
     */
    @Suppress("DEPRECATION")
    private suspend fun geocode(
        listener: (Geocoder, Geocoder.GeocodeListener) -> Unit,
        blocking: (Geocoder) -> List<Address>?,
    ): List<Address> {
        val geocoder = Geocoder(context, Locale.getDefault())
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    listener(geocoder) { addresses -> continuation.resume(addresses) }
                }
            } else {
                blocking(geocoder).orEmpty()
            }
        }.getOrDefault(emptyList())
    }

    /**
     * An address as one line the user would recognise.
     *
     * The fallback order matters and was duplicated: locality (the town), then the sub-admin
     * area (the county), then the admin area (the state), then whatever the platform called the
     * feature. A geocoder result with no locality is common — reverse-geocoding a point in open
     * country returns one — and without the chain those came back blank.
     */
    private fun Address.displayName(): String? = listOfNotNull(
        locality,
        subAdminArea,
        adminArea,
        featureName,
    ).firstOrNull { it.isNotBlank() }
}

/** [PermissionChecker] over `ContextCompat`. */
@Singleton
class AndroidPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : PermissionChecker {

    override fun hasLocationPermission(): Boolean =
        granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    override fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/** [PowerSettings] over `PowerManager`. */
@Singleton
class AndroidPowerSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : PowerSettings {

    /**
     * `getSystemService` is null-checked rather than cast.
     *
     * Both ViewModels wrote `context.getSystemService(POWER_SERVICE) as PowerManager`, an
     * unchecked cast in a path called from `init` — so on any device or test environment without
     * the service, constructing the ViewModel threw.
     */
    override fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
