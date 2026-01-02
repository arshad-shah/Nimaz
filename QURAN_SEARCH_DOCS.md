# Quran Search Functionality Documentation

This document details the technical implementation of the Quran search feature in the Nimaz application, tracing the data flow from the UI down to the database.

## 1. Technical Stack

*   **UI**: Jetpack Compose (Declarative UI)
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **State Management**: Kotlin StateFlow
*   **Dependency Injection**: Hilt
*   **Database**: Room (SQLite abstraction)
*   **Asynchronous Operations**: Kotlin Coroutines

---

## 2. Architecture & Data Flow

The search functionality follows a unidirectional data flow pattern:

`UI (Screen)` -> `Event` -> `ViewModel` -> `Repository (DataStore)` -> `System` -> `DAO` -> `Database`

### A. User Interface Layer

The entry point is **`QuranScreen.kt`**, which hosts the `QuranSearchScreen` within a tabbed interface.

**File:** `ui/screens/quran/QuranScreen.kt`
```kotlin
@Composable
private fun SearchContent(viewModel: QuranViewModel, onAyaClick: (LocalAya) -> Unit) {
    // Collecting state from ViewModel
    val searchQuery = viewModel.searchQuery.collectAsState()
    val searchResults = viewModel.filteredSearchResults.collectAsState()
    val searchFilters = viewModel.searchFilters.collectAsState()
    
    // Rendering the search screen
    QuranSearchScreen(
        searchQuery = searchQuery.value,
        searchResults = searchResults.value,
        // ...
        onSearch = viewModel::searchAyas,
        onAdvancedSearch = viewModel::searchAyasAdvanced,
        onSearchInFavorites = viewModel::searchInFavorites,
        onSearchInBookmarks = viewModel::searchInBookmarks,
        // ...
    )
}
```

**File:** `ui/screens/quran/QuranSearchScreen.kt`
This screen handles the search input and displays results. It supports:
1.  **Global Search**: Across all text (Arabic, English, Urdu).
2.  **Scoped Search**: Via chips to search specifically in "Favorites" or "Bookmarks" (connecting to "My Quran" data).
3.  **Advanced Filters**: Filter by Surah, Juz, or attributes (Note, Bookmark, Favorite).

```kotlin
// Example: Search Bar triggering search
FilledIconButton(
    onClick = { onSearch(searchQuery) },
    // ...
) {
    Icon(Icons.AutoMirrored.Rounded.KeyboardReturn, ...)
}
```

### B. ViewModel Layer

**File:** `viewModel/QuranViewModel.kt`
The ViewModel acts as the state holder and mediator. It exposes `StateFlow`s for the UI to observe and methods to trigger searches.

**State Management:**
```kotlin
private val _searchQuery = MutableStateFlow("")
private val _searchResults = MutableStateFlow<List<LocalAya>>(emptyList())
private val _searchFilters = MutableStateFlow(SearchFilters())

// Combined flow for UI consumption
val filteredSearchResults: StateFlow<List<LocalAya>> = combine(...)
```

**Search Logic:**
The ViewModel decides which Repository method to call based on the user's context (e.g., specific language or scoped to Favorites).

```kotlin
fun searchAyas(query: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val results = when (_searchLanguage.value) {
            "Arabic" -> dataStore.searchAyasInArabic(query)
            "English" -> dataStore.searchAyasInEnglish(query)
            // ...
            else -> dataStore.searchAyas(query)
        }
        _searchResults.value = results
    }
}

fun searchInFavorites(query: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val results = dataStore.searchFavoriteAyas(query)
        _searchResults.value = results
    }
}
```

### C. Repository Layer

The application uses a `DataStore` class as a central repository, which delegates domain-specific logic to systems (in this case, `QuranSystem`).

**File:** `data/local/DataStore.kt`
```kotlin
suspend fun searchAyas(query: String) = quranSystem.searchAyas(query)
suspend fun searchAyasAdvanced(...) = quranSystem.searchAyasAdvanced(...)
```

**File:** `data/local/systems/QuranSystem.kt`
This layer connects the repository to the DAO.
```kotlin
class QuranSystem @Inject constructor(private val ayaDao: AyaDao, ...) {
    suspend fun searchAyas(query: String) = ayaDao.searchAyas(query)
    // ...
}
```

### D. Database Layer (DAO)

**File:** `data/local/dao/AyaDao.kt`
This contains the raw SQL queries executed by Room.

**1. General Text Search**
Uses SQLite `LIKE` operator to search across multiple columns (Arabic text and translations).
```kotlin
@Query("""
    SELECT * FROM Aya 
    WHERE LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%' 
    OR LOWER(translationEnglish) LIKE '%' || LOWER(:query) || '%'
    OR LOWER(translationUrdu) LIKE '%' || LOWER(:query) || '%'
    ORDER BY suraNumber ASC, ayaNumberInSurah ASC
""")
suspend fun searchAyas(query: String): List<LocalAya>
```

**2. Scoped Search (My Quran Data)**
Searches only within Ayahs marked as Favorite or Bookmarked.
```kotlin
@Query("""
    SELECT * FROM Aya 
    WHERE favorite = 1
    AND (
        LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%' 
        OR LOWER(translationEnglish) LIKE '%' || LOWER(:query) || '%'
        OR LOWER(translationUrdu) LIKE '%' || LOWER(:query) || '%'
    )
    ORDER BY suraNumber ASC, ayaNumberInSurah ASC
""")
suspend fun searchFavoriteAyas(query: String): List<LocalAya>
```

**3. Advanced Search**
Uses dynamic filtering where `NULL` parameters are ignored, allowing for flexible query combinations.
```kotlin
@Query("""
    SELECT * FROM Aya 
    WHERE (:surahNumber IS NULL OR suraNumber = :surahNumber)
    AND (:juzNumber IS NULL OR juzNumber = :juzNumber)
    AND (:isFavorite IS NULL OR favorite = :isFavorite)
    AND (:isBookmarked IS NULL OR bookmark = :isBookmarked)
    AND (:hasNote IS NULL OR (CASE WHEN :hasNote = 1 THEN note != '' ELSE note = '' END))
    AND (
        LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%' 
        OR ...
    )
    ORDER BY suraNumber ASC, ayaNumberInSurah ASC
""")
suspend fun searchAyasAdvanced(...): List<LocalAya>
```

## 3. Data Model

**File:** `data/local/models/LocalAya.kt`
The entity representing a single Verse (Aya).

```kotlin
@Entity(tableName = "Aya")
data class LocalAya(
    @PrimaryKey
    val ayaNumberInQuran: Int,   // Unique absolute ID
    var ayaArabic: String,       // Arabic text
    val translationEnglish: String,
    val translationUrdu: String,
    val suraNumber: Int,
    val ayaNumberInSurah: Int,
    var bookmark: Boolean,       // User state (My Quran)
    var favorite: Boolean,       // User state (My Quran)
    var note: String,            // User note
    var audioFileLocation: String,
    val sajda: Boolean,
    val sajdaType: String,
    val ruku: Int,
    val juzNumber: Int,
)
```

## 4. Connection to "My Quran"

The "My Quran" screen (`MyQuranScreen.kt`) allows users to curate their personal collection of Ayahs (Bookmarks, Favorites, Notes). 
*   **Visualizing**: "My Quran" displays these lists.
*   **Searching**: The `QuranSearchScreen` utilizes the same boolean flags (`favorite`, `bookmark`) in the database to filter search results, allowing users to find specific text *within* their saved collections using `searchInFavorites` and `searchInBookmarks` methods.
