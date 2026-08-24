package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
import com.arshadshah.nimaz.data.local.database.entity.LocationEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * How a chosen location is stored, and which one the app then reads back.
 *
 * Both defects here are invisible to a ViewModel test — they are about what the *table* ends up
 * holding after two taps — so they are tested against a real Room database.
 */
@RunWith(RobolectricTestRunner::class)
class LocationDaoTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var dao: LocationDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.locationDao()
    }

    @After
    fun tearDown() = db.close()

    // S7 — exactly one current location, and it is the one just chosen.

    @Test
    fun `selecting a second location replaces the first as current`() = runTest {
        dao.saveCurrentLocation(location("London", 51.507, -0.128), now = 1_000)
        dao.saveCurrentLocation(location("Cairo", 30.044, 31.236), now = 2_000)

        // `insertLocation` is `@Insert(onConflict = REPLACE)` on an autogenerate primary key, so
        // it always inserted. After two selections several rows carried isCurrentLocation = 1,
        // and both current-location reads are `… WHERE isCurrentLocation = 1 LIMIT 1` with no
        // ORDER BY — which returns the lowest rowid. **Selecting Cairo answered London.**
        assertThat(dao.getCurrentLocationSync()?.name).isEqualTo("Cairo")
    }

    @Test
    fun `only one row is ever flagged current`() = runTest {
        dao.saveCurrentLocation(location("London", 51.507, -0.128), now = 1_000)
        dao.saveCurrentLocation(location("Cairo", 30.044, 31.236), now = 2_000)
        dao.saveCurrentLocation(location("Delhi", 28.614, 77.209), now = 3_000)

        val flagged = dao.getAllLocationsSync().filter { it.isCurrentLocation }
        assertThat(flagged.map { it.name }).containsExactly("Delhi")
    }

    @Test
    fun `re-selecting the same place refreshes its row instead of duplicating it`() = runTest {
        dao.saveCurrentLocation(location("London", 51.507, -0.128), now = 1_000)
        dao.saveCurrentLocation(location("Cairo", 30.044, 31.236), now = 2_000)
        dao.saveCurrentLocation(location("London", 51.507, -0.128), now = 3_000)

        // The table grew by one row per selection, forever — the `distinctBy` in the ViewModel
        // only hid it in the recent row.
        assertThat(dao.getAllLocationsSync().map { it.name })
            .containsExactly("London", "Cairo")
    }

    @Test
    fun `coordinates within about a hundred metres are the same place`() = runTest {
        dao.saveCurrentLocation(location("London", 51.5074, -0.1278), now = 1_000)
        // The geocoder does not return byte-identical coordinates for the same query twice.
        dao.saveCurrentLocation(location("London", 51.50742, -0.12781), now = 2_000)

        assertThat(dao.getAllLocationsSync()).hasSize(1)
    }

    @Test
    fun `two genuinely different places are not merged`() = runTest {
        dao.saveCurrentLocation(location("London", 51.507, -0.128), now = 1_000)
        dao.saveCurrentLocation(location("Croydon", 51.372, -0.101), now = 2_000)

        assertThat(dao.getAllLocationsSync()).hasSize(2)
    }

    // S6 — the recent row is ordered by recency.

    @Test
    fun `recent locations are newest first, and include the one just saved`() = runTest {
        dao.saveCurrentLocation(location("Amsterdam", 52.370, 4.895), now = 1_000)
        dao.saveCurrentLocation(location("Berlin", 52.520, 13.405), now = 2_000)
        dao.saveCurrentLocation(location("Cairo", 30.044, 31.236), now = 3_000)
        dao.saveCurrentLocation(location("Delhi", 28.614, 77.209), now = 4_000)
        dao.saveCurrentLocation(location("Edinburgh", 55.953, -3.188), now = 5_000)
        dao.saveCurrentLocation(location("Zurich", 47.377, 8.542), now = 6_000)

        // `getAllLocations()` orders `isFavorite DESC, name ASC`, so taking five of it gave
        // Amsterdam…Edinburgh and **Zurich never appeared** in a row labelled "Recent".
        assertThat(dao.getRecentLocations(5).first().map { it.name })
            .containsExactly("Zurich", "Edinburgh", "Delhi", "Cairo", "Berlin").inOrder()
    }

    @Test
    fun `re-selecting an old location brings it back to the front`() = runTest {
        dao.saveCurrentLocation(location("Amsterdam", 52.370, 4.895), now = 1_000)
        dao.saveCurrentLocation(location("Berlin", 52.520, 13.405), now = 2_000)
        dao.saveCurrentLocation(location("Amsterdam", 52.370, 4.895), now = 3_000)

        assertThat(dao.getRecentLocations(5).first().map { it.name })
            .containsExactly("Amsterdam", "Berlin").inOrder()
    }

    // ---- Favourites and per-location calculation settings ----

    @Test
    fun `favouriting a place flips it, and flips it back`() = runTest {
        val id = dao.saveCurrentLocation(location("London", 51.507, -0.128), now = 1_000)
        val before = System.currentTimeMillis()

        // The timestamp defaults to now, and every caller in the app takes that default.
        dao.toggleFavorite(id)

        assertThat(dao.getAllLocationsSync().single().isFavorite).isTrue()
        assertThat(dao.getAllLocationsSync().single().updatedAt).isAtLeast(before)

        dao.toggleFavorite(id)

        assertThat(dao.getAllLocationsSync().single().isFavorite).isFalse()
    }

    @Test
    fun `favouriting one place leaves the others alone`() = runTest {
        val london = dao.saveCurrentLocation(location("London", 51.507, -0.128), now = 1_000)
        dao.saveCurrentLocation(location("Cairo", 30.044, 31.236), now = 2_000)

        dao.toggleFavorite(london)

        assertThat(dao.getAllLocationsSync().filter { it.isFavorite }.map { it.name })
            .containsExactly("London")
    }

    @Test
    fun `a place carries its own calculation settings`() = runTest {
        val id = dao.saveCurrentLocation(location("Reykjavik", 64.147, -21.942), now = 1_000)

        // High latitudes are exactly why this is per-location rather than global.
        dao.updateCalculationSettings(
            id = id,
            method = "MOON_SIGHTING_COMMITTEE",
            asrMethod = "HANAFI",
            fajrAngle = 12.0,
            ishaAngle = 12.0,
        )

        val saved = dao.getAllLocationsSync().single()
        assertThat(saved.calculationMethod).isEqualTo("MOON_SIGHTING_COMMITTEE")
        assertThat(saved.asrCalculation).isEqualTo("HANAFI")
        assertThat(saved.fajrAngle).isEqualTo(12.0)
        assertThat(saved.ishaAngle).isEqualTo(12.0)
    }

    @Test
    fun `angles can be cleared back to the method's own`() = runTest {
        val id = dao.saveCurrentLocation(location("London", 51.507, -0.128), now = 1_000)
        dao.updateCalculationSettings(id, "KARACHI", "SHAFI", fajrAngle = 18.0, ishaAngle = 18.0)

        dao.updateCalculationSettings(id, "KARACHI", "SHAFI", fajrAngle = null, ishaAngle = null)

        assertThat(dao.getAllLocationsSync().single().fajrAngle).isNull()
        assertThat(dao.getAllLocationsSync().single().ishaAngle).isNull()
    }

    @Test
    fun `deleting all user data empties the table`() = runTest {
        dao.saveCurrentLocation(location("London", 51.507, -0.128), now = 1_000)

        dao.deleteAllUserData()

        assertThat(dao.getAllLocationsSync()).isEmpty()
        assertThat(dao.getCurrentLocationSync()).isNull()
    }

    private fun location(name: String, latitude: Double, longitude: Double) = LocationEntity(
        id = 0,
        name = name,
        latitude = latitude,
        longitude = longitude,
        timezone = "UTC",
        country = "",
        city = name,
        isCurrentLocation = true,
        isFavorite = false,
        calculationMethod = null,
        asrCalculation = null,
        highLatitudeRule = null,
        fajrAngle = null,
        ishaAngle = null,
    )
}
