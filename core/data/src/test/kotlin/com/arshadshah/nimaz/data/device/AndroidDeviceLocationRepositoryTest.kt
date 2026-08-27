package com.arshadshah.nimaz.data.device

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Location
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import java.util.Locale

/**
 * The platform adapters two ViewModels used to build for themselves.
 *
 * Both wrapped `FusedLocationProviderClient` and `Geocoder` in their own
 * `suspendCancellableCoroutine`, each carried its own copy of the API-33 geocoder split, and
 * each flattened an `Address` with its own copy of the same fallback chain — which is why
 * neither ViewModel could be constructed in a JVM test and neither had one.
 *
 * What these pin:
 *
 *  - **a geocoder failure is an empty result, not a crash.** `Geocoder` throws on a transient
 *    network failure, and a search box that crashes the app on a flaky connection is the
 *    defect the `runCatching` exists for;
 *  - **the display-name fallback chain, in order.** Reverse-geocoding a point in open country
 *    returns an address with no locality; without the chain the location came back blank, so
 *    the prayer-times header showed nothing at all;
 *  - **"has location permission" means fine OR coarse.** Requiring fine would tell a user who
 *    granted approximate location that they had granted nothing;
 *  - **notification permission is a runtime grant only from Tiramisu.** Asking
 *    `checkSelfPermission` below that returns denied on every device, so the app would nag
 *    forever about a permission that cannot be granted;
 *  - **`getSystemService` is null-checked, not cast.** Both ViewModels cast, in a path called
 *    from `init`, so constructing one on a device without the service threw.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [StubGeocoder::class])
class AndroidDeviceLocationRepositoryTest {

    private lateinit var context: Context
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var repository: AndroidDeviceLocationRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fused = mockk(relaxed = true)
        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(any<Context>()) } returns fused
        repository = AndroidDeviceLocationRepository(context, UnconfinedTestDispatcher())
        StubGeocoder.reset()
    }

    @After
    fun tearDown() {
        StubGeocoder.reset()
        unmockkAll()
    }

    // ── the current position ──────────────────────────────────────────────────

    @Test
    fun `a fix resolves to coordinates`() = runTest {
        givenFix(Location("test").apply { latitude = 53.35; longitude = -6.26 })

        val coordinates = repository.currentCoordinates()!!

        assertThat(coordinates.latitude).isEqualTo(53.35)
        assertThat(coordinates.longitude).isEqualTo(-6.26)
    }

    @Test
    fun `a device that cannot get a fix reports none rather than zero zero`() = runTest {
        givenFix(null)

        // (0, 0) is a real place in the Gulf of Guinea, and prayer times for it are wrong
        // everywhere else.
        assertThat(repository.currentCoordinates()).isNull()
    }

    @Test
    fun `a location provider that fails raises rather than reporting no location`() = runTest {
        givenFixFailure(IllegalStateException("location off"))

        // "Location services are off" and "you are nowhere" are different things, and only one
        // of them is worth telling the user about.
        val thrown = runCatching { repository.currentCoordinates() }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
    }

    // ── geocoding ─────────────────────────────────────────────────────────────

    @Test
    fun `a search maps each result to a named place`() = runTest {
        stubGeocoder(listOf(address(locality = "Dublin", country = "Ireland", lat = 53.35, lon = -6.26)))

        val results = repository.search("dublin", limit = 5)

        assertThat(results).hasSize(1)
        assertThat(results.single().name).isEqualTo("Dublin")
        assertThat(results.single().country).isEqualTo("Ireland")
        assertThat(results.single().latitude).isEqualTo(53.35)
    }

    @Test
    fun `a result the platform cannot name at all is dropped, not shown blank`() = runTest {
        stubGeocoder(listOf(address()))

        assertThat(repository.search("nowhere", limit = 5)).isEmpty()
    }

    @Test
    fun `a result with no country still lists, with an empty country`() = runTest {
        stubGeocoder(listOf(address(locality = "Dublin", country = null)))

        assertThat(repository.search("dublin", 5).single().country).isEmpty()
    }

    @Test
    fun `a geocoder that throws returns nothing rather than crashing the search box`() = runTest {
        stubGeocoderThrows()

        assertThat(repository.search("dublin", 5)).isEmpty()
        assertThat(repository.reverseGeocode(53.35, -6.26)).isNull()
    }

    @Test
    fun `reverse geocoding names the point`() = runTest {
        stubGeocoder(listOf(address(locality = "Dublin")))

        assertThat(repository.reverseGeocode(53.35, -6.26)).isEqualTo("Dublin")
    }

    @Test
    fun `a point the platform knows nothing about has no name`() = runTest {
        stubGeocoder(emptyList())

        assertThat(repository.reverseGeocode(0.0, 0.0)).isNull()
    }

    @Test
    fun `the display name falls back through county, then state, then feature`() = runTest {
        stubGeocoder(listOf(address(subAdminArea = "Laois")))
        assertThat(repository.reverseGeocode(0.0, 0.0)).isEqualTo("Laois")

        stubGeocoder(listOf(address(adminArea = "Leinster")))
        assertThat(repository.reverseGeocode(0.0, 0.0)).isEqualTo("Leinster")

        stubGeocoder(listOf(address(featureName = "Slieve Bloom")))
        // Reverse-geocoding a point in open country returns exactly this shape; without the
        // chain the prayer-times header showed nothing.
        assertThat(repository.reverseGeocode(0.0, 0.0)).isEqualTo("Slieve Bloom")
    }

    @Test
    fun `a blank locality is skipped rather than shown as an empty name`() = runTest {
        stubGeocoder(listOf(address(locality = "   ", subAdminArea = "Laois")))

        assertThat(repository.reverseGeocode(0.0, 0.0)).isEqualTo("Laois")
    }

    // ── permissions ───────────────────────────────────────────────────────────

    @Test
    fun `approximate location counts as location permission`() {
        val checker = AndroidPermissionChecker(context)
        assertThat(checker.hasLocationPermission()).isFalse()

        shadowApp().grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        // A user who granted approximate location has granted something; requiring fine would
        // tell them they had granted nothing.
        assertThat(checker.hasLocationPermission()).isTrue()
    }

    @Test
    fun `precise location counts as location permission`() {
        shadowApp().grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        assertThat(AndroidPermissionChecker(context).hasLocationPermission()).isTrue()
    }

    @Config(sdk = [33])
    @Test
    fun `from Tiramisu the notification permission is a real runtime grant`() {
        val checker = AndroidPermissionChecker(context)
        assertThat(checker.hasNotificationPermission()).isFalse()

        shadowApp().grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertThat(checker.hasNotificationPermission()).isTrue()
    }

    @Config(sdk = [31])
    @Test
    fun `before Tiramisu notifications are always permitted`() {
        // `checkSelfPermission` for a permission that does not exist yet returns denied, so
        // asking would nag forever about something the user cannot grant.
        assertThat(AndroidPermissionChecker(context).hasNotificationPermission()).isTrue()
    }

    // ── battery optimisation ──────────────────────────────────────────────────

    @Test
    fun `a phone that exempts the app reports the exemption`() {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(power).setIgnoringBatteryOptimizations(context.packageName, true)

        assertThat(AndroidPowerSettings(context).isIgnoringBatteryOptimizations()).isTrue()
    }

    @Test
    fun `a phone that does not exempt the app says so`() {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(power).setIgnoringBatteryOptimizations(context.packageName, false)

        assertThat(AndroidPowerSettings(context).isIgnoringBatteryOptimizations()).isFalse()
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun shadowApp(): ShadowApplication = shadowOf(context as android.app.Application)

    /**
     * A `Task` whose listeners fire inline. The real one posts to the main looper, which under
     * Robolectric is the very thread the test is suspended on.
     */
    private fun givenFix(location: Location?) {
        val task = mockk<Task<Location>>(relaxed = true)
        every { task.addOnSuccessListener(any<OnSuccessListener<Location>>()) } answers {
            firstArg<OnSuccessListener<Location>>().onSuccess(location)
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        every { fused.getCurrentLocation(any<Int>(), any()) } returns task
    }

    private fun givenFixFailure(error: Exception) {
        val task = mockk<Task<Location>>(relaxed = true)
        every { task.addOnSuccessListener(any<OnSuccessListener<Location>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
            firstArg<OnFailureListener>().onFailure(error)
            task
        }
        every { fused.getCurrentLocation(any<Int>(), any()) } returns task
    }

    private fun stubGeocoder(addresses: List<Address>) {
        StubGeocoder.addresses = addresses
    }

    private fun stubGeocoderThrows() {
        StubGeocoder.error = "network down"
    }

    private fun address(
        locality: String? = null,
        subAdminArea: String? = null,
        adminArea: String? = null,
        featureName: String? = null,
        country: String? = null,
        lat: Double = 0.0,
        lon: Double = 0.0,
    ) = Address(Locale.US).apply {
        this.locality = locality
        this.subAdminArea = subAdminArea
        this.adminArea = adminArea
        this.featureName = featureName
        this.countryName = country
        latitude = lat
        longitude = lon
    }
}
