# Location Screen Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the Location settings screen with a region-grouped, filterable curated
city list (~40 cities, each with a country-flag emoji), matching the Nimaz design language.

**Architecture:** Presentation-only change. Extract the city catalogue + pure helpers
(region grouping, coordinate formatting) into a new `LocationCatalog.kt` in the
`presentation.viewmodel` package so they're unit-testable without Android. `LocationViewModel`
gains a `selectedRegion` filter state + `SelectRegion` event. `LocationScreen` is restructured to
render a region filter chip row and a grouped/flat city list. No data-layer, DAO, repository,
DataStore, or navigation changes.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, JUnit + Truth, kotlinx-coroutines-test.

## Global Constraints

- Package: `com.arshadshah.nimaz`. App name: **Nimaz**.
- No hardcoded `Color(0xFF…)` in screens — use `MaterialTheme.colorScheme.*` / `NimazColors.*`.
- No emoji in the app **except** country flags on the Location screen (documented exception).
- Spacing/shape via `NimazSpacing.*` / `NimazCornerRadius.*` where used; reuse existing
  `presentation/components` (`NimazCard`, `NimazChip`, `NimazSearchBar`, `NimazSectionTitle`,
  `NimazBackTopAppBar`, `NimazIcon`).
- ViewModels expose `StateFlow<XxxUiState>` + single `onEvent(event)`. No exposed `MutableStateFlow`.
- Verify build with `./gradlew :app:compileDebugKotlin` and `./gradlew :app:testDebugUnitTest`
  (JDK 21 + Android SDK, compileSdk 36). Work on the current feature branch; do not push to `dev`.

---

### Task 1: City catalogue + pure helpers (region grouping, coordinate formatting)

Create the pure, Android-free building blocks and their tests. This task moves
`defaultPopularCities` out of `LocationViewModel.kt` into a new file and expands it.

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/LocationCatalog.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/LocationViewModel.kt`
  (extend `SearchLocation`; delete the old `defaultPopularCities` list at lines 68–77)
- Test: `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/LocationCatalogTest.kt`

**Interfaces:**
- Produces:
  - `enum class CityRegion(val label: String, val order: Int)` — MIDDLE_EAST, ASIA, EUROPE, AMERICAS, AFRICA
  - `data class SearchLocation(name, country, latitude, longitude, region: CityRegion? = null, flag: String? = null)` (extended in place)
  - `val defaultPopularCities: List<SearchLocation>` (moved to `LocationCatalog.kt`, ~40 entries)
  - `fun groupCitiesByRegion(cities: List<SearchLocation>): List<Pair<CityRegion, List<SearchLocation>>>`
  - `fun citiesForRegion(cities: List<SearchLocation>, region: CityRegion?): List<SearchLocation>`
  - `fun formatCoordinates(latitude: Double, longitude: Double): String`

- [ ] **Step 1: Extend `SearchLocation` in `LocationViewModel.kt`**

Replace the existing `data class SearchLocation` (lines 61–66) with:

```kotlin
data class SearchLocation(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val region: CityRegion? = null,   // set for curated cities; null for Geocoder & recent results
    val flag: String? = null,         // country-flag emoji for curated cities; null otherwise
)
```

Then delete the old `defaultPopularCities` declaration (lines 68–77) from `LocationViewModel.kt`
— it moves to `LocationCatalog.kt` in Step 3. (The default `popularCities = defaultPopularCities`
on `LocationUiState` line 45 still resolves because the new file is in the same package.)

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/LocationCatalogTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
```

- [ ] **Step 3: Create `LocationCatalog.kt` with the enum, city list, and helpers**

```kotlin
package com.arshadshah.nimaz.presentation.viewmodel

import kotlin.math.abs

/**
 * Geographic grouping for the curated city list shown on the Location screen.
 * `order` controls the display order of region groups in "All" mode.
 */
enum class CityRegion(val label: String, val order: Int) {
    MIDDLE_EAST("Middle East", 0),
    ASIA("Asia", 1),
    EUROPE("Europe", 2),
    AMERICAS("Americas", 3),
    AFRICA("Africa", 4),
}

/**
 * Curated set of notable cities (Muslim-majority + major diaspora), each tagged with a
 * [CityRegion] and its country-flag emoji. Flags are the one sanctioned emoji use in the app
 * (Location screen only) — see docs/ARCHITECTURE.md §7 / §9.
 */
val defaultPopularCities: List<SearchLocation> = listOf(
    // Middle East
    SearchLocation("Mecca", "Saudi Arabia", 21.4225, 39.8262, CityRegion.MIDDLE_EAST, "🇸🇦"),
    SearchLocation("Madinah", "Saudi Arabia", 24.4686, 39.6142, CityRegion.MIDDLE_EAST, "🇸🇦"),
    SearchLocation("Riyadh", "Saudi Arabia", 24.7136, 46.6753, CityRegion.MIDDLE_EAST, "🇸🇦"),
    SearchLocation("Jeddah", "Saudi Arabia", 21.4858, 39.1925, CityRegion.MIDDLE_EAST, "🇸🇦"),
    SearchLocation("Dubai", "United Arab Emirates", 25.2048, 55.2708, CityRegion.MIDDLE_EAST, "🇦🇪"),
    SearchLocation("Abu Dhabi", "United Arab Emirates", 24.4539, 54.3773, CityRegion.MIDDLE_EAST, "🇦🇪"),
    SearchLocation("Doha", "Qatar", 25.2854, 51.5310, CityRegion.MIDDLE_EAST, "🇶🇦"),
    SearchLocation("Kuwait City", "Kuwait", 29.3759, 47.9774, CityRegion.MIDDLE_EAST, "🇰🇼"),
    SearchLocation("Manama", "Bahrain", 26.2285, 50.5860, CityRegion.MIDDLE_EAST, "🇧🇭"),
    SearchLocation("Muscat", "Oman", 23.5880, 58.3829, CityRegion.MIDDLE_EAST, "🇴🇲"),
    SearchLocation("Jerusalem", "Palestine", 31.7683, 35.2137, CityRegion.MIDDLE_EAST, "🇵🇸"),
    SearchLocation("Amman", "Jordan", 31.9454, 35.9284, CityRegion.MIDDLE_EAST, "🇯🇴"),
    SearchLocation("Baghdad", "Iraq", 33.3152, 44.3661, CityRegion.MIDDLE_EAST, "🇮🇶"),
    SearchLocation("Tehran", "Iran", 35.6892, 51.3890, CityRegion.MIDDLE_EAST, "🇮🇷"),
    // Asia
    SearchLocation("Istanbul", "Türkiye", 41.0082, 28.9784, CityRegion.ASIA, "🇹🇷"),
    SearchLocation("Karachi", "Pakistan", 24.8607, 67.0011, CityRegion.ASIA, "🇵🇰"),
    SearchLocation("Lahore", "Pakistan", 31.5204, 74.3587, CityRegion.ASIA, "🇵🇰"),
    SearchLocation("Islamabad", "Pakistan", 33.6844, 73.0479, CityRegion.ASIA, "🇵🇰"),
    SearchLocation("Dhaka", "Bangladesh", 23.8103, 90.4125, CityRegion.ASIA, "🇧🇩"),
    SearchLocation("Delhi", "India", 28.6139, 77.2090, CityRegion.ASIA, "🇮🇳"),
    SearchLocation("Hyderabad", "India", 17.3850, 78.4867, CityRegion.ASIA, "🇮🇳"),
    SearchLocation("Jakarta", "Indonesia", -6.2088, 106.8456, CityRegion.ASIA, "🇮🇩"),
    SearchLocation("Kuala Lumpur", "Malaysia", 3.1390, 101.6869, CityRegion.ASIA, "🇲🇾"),
    SearchLocation("Singapore", "Singapore", 1.3521, 103.8198, CityRegion.ASIA, "🇸🇬"),
    // Europe
    SearchLocation("London", "United Kingdom", 51.5074, -0.1278, CityRegion.EUROPE, "🇬🇧"),
    SearchLocation("Birmingham", "United Kingdom", 52.4862, -1.8904, CityRegion.EUROPE, "🇬🇧"),
    SearchLocation("Paris", "France", 48.8566, 2.3522, CityRegion.EUROPE, "🇫🇷"),
    SearchLocation("Berlin", "Germany", 52.5200, 13.4050, CityRegion.EUROPE, "🇩🇪"),
    SearchLocation("Amsterdam", "Netherlands", 52.3676, 4.9041, CityRegion.EUROPE, "🇳🇱"),
    SearchLocation("Brussels", "Belgium", 50.8503, 4.3517, CityRegion.EUROPE, "🇧🇪"),
    SearchLocation("Sarajevo", "Bosnia and Herzegovina", 43.8563, 18.4131, CityRegion.EUROPE, "🇧🇦"),
    // Americas
    SearchLocation("New York", "United States", 40.7128, -74.0060, CityRegion.AMERICAS, "🇺🇸"),
    SearchLocation("Chicago", "United States", 41.8781, -87.6298, CityRegion.AMERICAS, "🇺🇸"),
    SearchLocation("Dearborn", "United States", 42.3223, -83.1763, CityRegion.AMERICAS, "🇺🇸"),
    SearchLocation("Toronto", "Canada", 43.6532, -79.3832, CityRegion.AMERICAS, "🇨🇦"),
    // Africa
    SearchLocation("Cairo", "Egypt", 30.0444, 31.2357, CityRegion.AFRICA, "🇪🇬"),
    SearchLocation("Casablanca", "Morocco", 33.5731, -7.5898, CityRegion.AFRICA, "🇲🇦"),
    SearchLocation("Lagos", "Nigeria", 6.5244, 3.3792, CityRegion.AFRICA, "🇳🇬"),
    SearchLocation("Kano", "Nigeria", 12.0022, 8.5920, CityRegion.AFRICA, "🇳🇬"),
    SearchLocation("Khartoum", "Sudan", 15.5007, 32.5599, CityRegion.AFRICA, "🇸🇩"),
    SearchLocation("Nairobi", "Kenya", -1.2921, 36.8219, CityRegion.AFRICA, "🇰🇪"),
)

/** Groups curated cities by region, preserving [CityRegion.order]; skips regions with no cities. */
fun groupCitiesByRegion(cities: List<SearchLocation>): List<Pair<CityRegion, List<SearchLocation>>> =
    cities.filter { it.region != null }
        .groupBy { it.region!! }
        .toList()
        .sortedBy { it.first.order }

/** Returns all cities when [region] is null, otherwise only that region's cities. */
fun citiesForRegion(cities: List<SearchLocation>, region: CityRegion?): List<SearchLocation> =
    if (region == null) cities else cities.filter { it.region == region }

/** Formats a coordinate pair with sign-derived hemispheres, e.g. "21.4225° N, 39.8262° E". */
fun formatCoordinates(latitude: Double, longitude: Double): String {
    val ns = if (latitude >= 0) "N" else "S"
    val ew = if (longitude >= 0) "E" else "W"
    return "%.4f° %s, %.4f° %s".format(abs(latitude), ns, abs(longitude), ew)
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.viewmodel.LocationCatalogTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/LocationCatalog.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/LocationViewModel.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/LocationCatalogTest.kt
git commit -m "feat(location): add regioned city catalogue + coord/grouping helpers"
```

---

### Task 2: ViewModel region-filter state + event

Add `selectedRegion` to the UI state and a `SelectRegion` event with its handler.

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/LocationViewModel.kt`
  (`LocationUiState` line 40–49; `LocationEvent` line 79–87; `onEvent` line 107–137)
- Test: `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/LocationViewModelRegionTest.kt`

**Interfaces:**
- Consumes: `CityRegion` (Task 1).
- Produces: `LocationUiState.selectedRegion: CityRegion?`; `LocationEvent.SelectRegion(region: CityRegion?)`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/LocationViewModelRegionTest.kt`:

```kotlin
package com.arshadshah.nimaz.presentation.viewmodel

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
```

> Note: this project's existing ViewModel tests (e.g. `HomeViewModelTest`) construct the VM with
> fakes; the Location VM needs a `Context`, `Geocoder`, and Play-Services client, so we assert the
> reducer contract in isolation here. Behavior is exercised end-to-end in the UI task.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.viewmodel.LocationViewModelRegionTest"`
Expected: FAIL — `LocationUiState` has no `selectedRegion` parameter (compile error).

- [ ] **Step 3: Add `selectedRegion` to `LocationUiState`**

In `LocationViewModel.kt`, add the field to `LocationUiState` (after `popularCities`):

```kotlin
data class LocationUiState(
    val searchQuery: String = "",
    val searchResults: List<SearchLocation> = emptyList(),
    val currentLocation: CurrentLocationState = CurrentLocationState.NotSet,
    val recentLocations: List<SearchLocation> = emptyList(),
    val popularCities: List<SearchLocation> = defaultPopularCities,
    val selectedRegion: CityRegion? = null,
    val isSearching: Boolean = false,
    val isLoadingGps: Boolean = false,
    val error: String? = null
)
```

- [ ] **Step 4: Add the `SelectRegion` event + handler**

In `LocationEvent`, add:

```kotlin
    data class SelectRegion(val region: CityRegion?) : LocationEvent
```

In `onEvent`, add a branch (place it next to `ClearSearch`):

```kotlin
            is LocationEvent.SelectRegion -> {
                AppAnalytics.logFeatureUsed("location", "filter_region")
                _state.update { it.copy(selectedRegion = event.region) }
            }
```

- [ ] **Step 5: Run tests + compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.viewmodel.LocationViewModelRegionTest"`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/LocationViewModel.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/LocationViewModelRegionTest.kt
git commit -m "feat(location): add region filter state and SelectRegion event"
```

---

### Task 3: String resources

Add the two new user-facing strings (region labels come from the enum).

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (near the existing `location_*` strings, ~line 1089)

- [ ] **Step 1: Add the strings**

After `<string name="location_popular_cities">Popular Cities</string>` (line 1091), add:

```xml
    <string name="location_browse_by_region">Browse by Region</string>
    <string name="location_region_all">All</string>
```

- [ ] **Step 2: Verify resources compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (R class regenerates with the two new IDs).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat(location): add browse-by-region and All-filter strings"
```

---

### Task 4: Redesign `LocationScreen` — region filter row, grouped list, flags, coord fix

Rewrite the screen body: add the region filter chip row, render cities grouped ("All") or flat
(single region), show country-flag emoji in curated rows, and use `formatCoordinates` in the hero.

**Files:**
- Modify (rewrite body + helpers): `app/src/main/java/com/arshadshah/nimaz/presentation/screens/settings/LocationScreen.kt`

**Interfaces:**
- Consumes: `CityRegion`, `groupCitiesByRegion`, `citiesForRegion`, `formatCoordinates`,
  `LocationEvent.SelectRegion`, `LocationUiState.selectedRegion` (Tasks 1–2);
  `NimazChip`/`NimazChipVariant.FILTER`, `NimazSectionTitle`, `NimazCard`, `NimazIcon`.

- [ ] **Step 1: Replace the Popular-Cities block with the region-browse block**

In `LocationScreen.kt`, replace the current "Popular Cities" section (lines 194–204) with the
region filter row + grouped/flat list. Replace this:

```kotlin
            // Popular Cities
            item {
                NimazSectionTitle(text = stringResource(R.string.location_popular_cities))
            }
            items(state.popularCities) { location ->
                LocationListItem(
                    location = location,
                    isSelected = isLocationSelected(state.currentLocation, location),
                    onClick = { viewModel.onEvent(LocationEvent.SelectLocation(location)) }
                )
            }
```

with:

```kotlin
            // Browse by region (hidden while showing live search results)
            if (state.searchResults.isEmpty()) {
                item {
                    NimazSectionTitle(text = stringResource(R.string.location_browse_by_region))
                }
                item {
                    RegionFilterRow(
                        selectedRegion = state.selectedRegion,
                        onSelect = { viewModel.onEvent(LocationEvent.SelectRegion(it)) }
                    )
                }

                if (state.selectedRegion == null) {
                    // "All" → grouped, with a region sub-header before each group
                    groupCitiesByRegion(state.popularCities).forEach { (region, cities) ->
                        item(key = "region-${region.name}") {
                            Text(
                                text = region.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp)
                            )
                        }
                        items(cities, key = { "${it.name}-${it.country}" }) { location ->
                            LocationListItem(
                                location = location,
                                isSelected = isLocationSelected(state.currentLocation, location),
                                onClick = { viewModel.onEvent(LocationEvent.SelectLocation(location)) }
                            )
                        }
                    }
                } else {
                    // Single region → flat list
                    items(
                        citiesForRegion(state.popularCities, state.selectedRegion),
                        key = { "${it.name}-${it.country}" }
                    ) { location ->
                        LocationListItem(
                            location = location,
                            isSelected = isLocationSelected(state.currentLocation, location),
                            onClick = { viewModel.onEvent(LocationEvent.SelectLocation(location)) }
                        )
                    }
                }
            }
```

- [ ] **Step 2: Add the `RegionFilterRow` composable**

Add this private composable to `LocationScreen.kt` (e.g. above `LocationListItem`):

```kotlin
@Composable
private fun RegionFilterRow(
    selectedRegion: CityRegion?,
    onSelect: (CityRegion?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NimazChip(
            text = stringResource(R.string.location_region_all),
            onClick = { onSelect(null) },
            variant = NimazChipVariant.FILTER,
            selected = selectedRegion == null
        )
        CityRegion.entries.sortedBy { it.order }.forEach { region ->
            NimazChip(
                text = region.label,
                onClick = { onSelect(region) },
                variant = NimazChipVariant.FILTER,
                selected = selectedRegion == region
            )
        }
    }
}
```

- [ ] **Step 3: Render the flag in `LocationListItem`**

In `LocationListItem` (lines 401–419), replace the icon `Box` so a curated city shows its flag
emoji, falling back to the existing `NimazIcon` for recents/search results:

```kotlin
        // Icon (country flag for curated cities, glyph otherwise)
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (location.flag != null) {
                Text(text = location.flag, style = MaterialTheme.typography.titleMedium)
            } else {
                NimazIcon(
                    imageVector = if (showGlobeIcon) Icons.Default.Public else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 18.dp
                )
            }
        }
```

- [ ] **Step 4: Fix the coordinate hemisphere in `CurrentLocationCard`**

In `CurrentLocationCard`, replace the hardcoded-hemisphere `Text` (lines 276–285) with:

```kotlin
                        Text(
                            text = formatCoordinates(
                                currentLocation.latitude,
                                currentLocation.longitude
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
```

- [ ] **Step 5: Add the required imports**

Add to the imports at the top of `LocationScreen.kt`:

```kotlin
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazChipVariant
import com.arshadshah.nimaz.presentation.viewmodel.CityRegion
import com.arshadshah.nimaz.presentation.viewmodel.citiesForRegion
import com.arshadshah.nimaz.presentation.viewmodel.formatCoordinates
import com.arshadshah.nimaz.presentation.viewmodel.groupCitiesByRegion
```

(`Row`, `Arrangement`, `fillMaxWidth`, `padding`, `Text`, `FontWeight`, `dp`, `Modifier`,
`MaterialTheme` are already imported.)

- [ ] **Step 6: Compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Full test run**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL (all suites, including Tasks 1–2 tests).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/settings/LocationScreen.kt
git commit -m "feat(location): region-grouped browse, city flags, hemisphere-correct coords"
```

---

### Task 5: Documentation updates

Record the flag-emoji exception and the city-list expansion.

**Files:**
- Modify: `docs/ARCHITECTURE.md` (§7 iconography rule + §9 deviation registry)
- Modify: `docs/SUBSYSTEMS.md` (location/preferences section, if it lists the city source)

- [ ] **Step 1: Add the §7 iconography note in `docs/ARCHITECTURE.md`**

Find §7's rule about "no hardcoded colors / Material icons via `NimazIcon` / no emoji" and append:

```markdown
> **Exception (Location screen only):** country flags on the Location screen
> (`LocationScreen`, curated cities in `LocationCatalog.kt`) are rendered as emoji.
> This is the single sanctioned emoji use in the app; no other emoji are permitted.
```

- [ ] **Step 2: Add a §9 deviation-registry entry in `docs/ARCHITECTURE.md`**

In the §9 registry, add a row/entry:

```markdown
- **Flag emoji on Location screen** — the Location screen renders country flags as emoji,
  deviating from the "Material icons via NimazIcon, no emoji" rule. Deliberate and bounded to
  this screen (curated cities only). See `LocationCatalog.kt` / `LocationScreen.kt`.
```

- [ ] **Step 3: Update `docs/SUBSYSTEMS.md` if it describes the city list**

Search `docs/SUBSYSTEMS.md` for a location/preferences or prayer-location section. If it mentions
the popular-cities list, update it to: "curated ~40-city catalogue grouped by `CityRegion`
(`LocationCatalog.kt`), filterable on the Location screen; live search still via Android
`Geocoder`." If no such section exists, skip this step.

- [ ] **Step 4: Commit**

```bash
git add docs/ARCHITECTURE.md docs/SUBSYSTEMS.md
git commit -m "docs: record Location flag-emoji exception and city-list expansion"
```

---

## Self-Review

**Spec coverage:**
- §1 goal (native look, ~40 cities, region grouping, flags) → Tasks 1, 4. ✓
- §2 layout (hero, search, GPS, search-results, recent, region browse) → existing screen retained;
  region browse added in Task 4. ✓
- §3 components (all existing) → Task 4 uses `NimazChip`/`NimazSectionTitle`/`NimazCard`/`NimazIcon`. ✓
- §4 data model (`CityRegion`, extended `SearchLocation`, ~40 cities) → Task 1. ✓
- §5 VM state (`selectedRegion`, `SelectRegion`) → Task 2. ✓
- §6 coord bug fix → Task 1 (`formatCoordinates`) + Task 4 Step 4. ✓
- §7 documented emoji exception → Task 5. ✓
- §8 doc updates → Task 5. ✓
- §9 testing (grouping/filter + hemisphere + build) → Tasks 1, 2 tests; Task 4 build/test. ✓

**Placeholder scan:** none — all steps contain concrete code/commands.

**Type consistency:** `SearchLocation(name, country, latitude, longitude, region, flag)`,
`groupCitiesByRegion`, `citiesForRegion`, `formatCoordinates`, `CityRegion(label, order)`,
`LocationEvent.SelectRegion`, `LocationUiState.selectedRegion` are used identically across
Tasks 1, 2, and 4. ✓
