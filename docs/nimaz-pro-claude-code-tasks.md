# Nimaz Pro - Claude Code Task Instructions

> **Important**: Execute these tasks in order. Each task builds on the previous. Do not skip tasks.

---

## Pre-requisites

Before starting, ensure you have:
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17+
- Kotlin 2.0.0+

---

## Phase 1: Project Setup (Tasks 1-5)

### Task 1: Create Android Project & Dependencies

**Objective**: Set up the Android project with all required dependencies.

```
1. Create new Android Studio project:
   - Template: Empty Compose Activity
   - Package name: com.nimazpro.app
   - Minimum SDK: 26 (Android 8.0)
   - Build configuration: Kotlin DSL

2. Replace settings.gradle.kts with:
```

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "NimazPro"
include(":app")
```

```
3. Replace build.gradle.kts (Project level) with:
```

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("androidx.room") version "2.8.4" apply false
}
```

```
4. Replace build.gradle.kts (app module) with the full configuration from the technical document Section 2.7

5. Sync project and verify no errors
```

**Verification**: Project builds successfully with `./gradlew assembleDebug`

---

### Task 2: Create Package Structure

**Objective**: Set up the Clean Architecture folder structure.

```
Create the following package structure under app/src/main/java/com/nimazpro/app/:

├── NimazProApp.kt
├── MainActivity.kt
├── core/
│   ├── di/
│   ├── util/
│   └── navigation/
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── dao/
│   │   │   └── entity/
│   │   └── datastore/
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── theme/
│   ├── components/
│   │   ├── atoms/
│   │   ├── molecules/
│   │   └── organisms/
│   └── screens/
│       ├── home/
│       ├── quran/
│       ├── hadith/
│       ├── dua/
│       ├── tasbih/
│       ├── prayer/
│       ├── fasting/
│       ├── zakat/
│       ├── qibla/
│       ├── calendar/
│       ├── bookmarks/
│       ├── search/
│       ├── settings/
│       └── onboarding/
└── widget/
```

**Create NimazProApp.kt**:
```kotlin
package com.nimazpro.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NimazProApp : Application()
```

**Create MainActivity.kt**:
```kotlin
package com.nimazpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nimazpro.app.presentation.theme.NimazProTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NimazProTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // NavGraph will go here
                }
            }
        }
    }
}
```

**Update AndroidManifest.xml**:
```xml
<application
    android:name=".NimazProApp"
    ...>
```

**Verification**: App compiles and runs showing empty screen

---

### Task 3: Setup Theme & Design System

**Objective**: Implement the complete Material 3 theme.

**Create presentation/theme/Color.kt**:
```kotlin
package com.nimazpro.app.presentation.theme

import androidx.compose.ui.graphics.Color

object NimazColors {
    // Primary - Teal
    val Primary50 = Color(0xFFF0FDFA)
    val Primary100 = Color(0xFFCCFBF1)
    val Primary200 = Color(0xFF99F6E4)
    val Primary300 = Color(0xFF5EEAD4)
    val Primary400 = Color(0xFF2DD4BF)
    val Primary500 = Color(0xFF14B8A6)
    val Primary600 = Color(0xFF0D9488)
    val Primary700 = Color(0xFF0F766E)
    val Primary800 = Color(0xFF115E59)
    val Primary900 = Color(0xFF134E4A)
    
    // Accent - Gold
    val Gold50 = Color(0xFFFEFCE8)
    val Gold100 = Color(0xFFFEF9C3)
    val Gold300 = Color(0xFFFDE047)
    val Gold500 = Color(0xFFEAB308)
    val Gold700 = Color(0xFFA16207)
    
    // Prayer Colors
    val Fajr = Color(0xFF6366F1)
    val Dhuhr = Color(0xFFEAB308)
    val Asr = Color(0xFFF97316)
    val Maghrib = Color(0xFFEF4444)
    val Isha = Color(0xFF8B5CF6)
    
    // Neutrals
    val Neutral50 = Color(0xFFFAFAF9)
    val Neutral100 = Color(0xFFF5F5F4)
    val Neutral200 = Color(0xFFE7E5E4)
    val Neutral300 = Color(0xFFD6D3D1)
    val Neutral400 = Color(0xFFA8A29E)
    val Neutral500 = Color(0xFF78716C)
    val Neutral600 = Color(0xFF57534E)
    val Neutral700 = Color(0xFF44403C)
    val Neutral800 = Color(0xFF292524)
    val Neutral900 = Color(0xFF1C1917)
    val Neutral950 = Color(0xFF0C0A09)
    
    // Semantic
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
}
```

**Create presentation/theme/Type.kt**:
```kotlin
package com.nimazpro.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nimazpro.app.R

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold)
)

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)

val NimazTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp
    ),
    displayMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    displaySmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

**Create presentation/theme/Shape.kt**:
```kotlin
package com.nimazpro.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val NimazShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)
```

**Create presentation/theme/Theme.kt**:
```kotlin
package com.nimazpro.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NimazColors.Primary500,
    onPrimary = Color.White,
    primaryContainer = NimazColors.Primary800,
    onPrimaryContainer = NimazColors.Primary100,
    secondary = NimazColors.Gold500,
    onSecondary = Color.Black,
    secondaryContainer = NimazColors.Gold700,
    onSecondaryContainer = NimazColors.Gold100,
    tertiary = NimazColors.Primary400,
    background = NimazColors.Neutral950,
    onBackground = Color.White,
    surface = NimazColors.Neutral900,
    onSurface = Color.White,
    surfaceVariant = NimazColors.Neutral800,
    onSurfaceVariant = NimazColors.Neutral300,
    outline = NimazColors.Neutral700,
    outlineVariant = NimazColors.Neutral800,
    error = NimazColors.Error,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = NimazColors.Primary600,
    onPrimary = Color.White,
    primaryContainer = NimazColors.Primary100,
    onPrimaryContainer = NimazColors.Primary900,
    secondary = NimazColors.Gold500,
    onSecondary = Color.Black,
    secondaryContainer = NimazColors.Gold100,
    onSecondaryContainer = NimazColors.Gold700,
    tertiary = NimazColors.Primary400,
    background = NimazColors.Neutral50,
    onBackground = NimazColors.Neutral900,
    surface = Color.White,
    onSurface = NimazColors.Neutral900,
    surfaceVariant = NimazColors.Neutral100,
    onSurfaceVariant = NimazColors.Neutral600,
    outline = NimazColors.Neutral300,
    outlineVariant = NimazColors.Neutral200,
    error = NimazColors.Error,
    onError = Color.White
)

@Composable
fun NimazProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NimazTypography,
        shapes = NimazShapes,
        content = content
    )
}
```

**Download fonts and place in res/font/**:
- outfit_regular.ttf, outfit_medium.ttf, outfit_semibold.ttf, outfit_bold.ttf
- plus_jakarta_sans_regular.ttf, plus_jakarta_sans_medium.ttf, plus_jakarta_sans_semibold.ttf, plus_jakarta_sans_bold.ttf
- amiri_regular.ttf, amiri_bold.ttf

**Verification**: App builds and displays with custom theme colors

---

### Task 4: Setup Room Database Structure

**Objective**: Create the complete Room database with all entities.

**Create data/local/database/entity/SurahEntity.kt**:
```kotlin
package com.nimazpro.app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "number") val number: Int,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    @ColumnInfo(name = "name_transliteration") val nameTransliteration: String,
    @ColumnInfo(name = "revelation_type") val revelationType: String,
    @ColumnInfo(name = "verses_count") val versesCount: Int,
    @ColumnInfo(name = "order_revealed") val orderRevealed: Int,
    @ColumnInfo(name = "start_page") val startPage: Int
)
```

**Create remaining entities as specified in the technical document Section 4.2**:
- AyahEntity.kt
- TranslationEntity.kt
- QuranBookmarkEntity.kt
- ReadingProgressEntity.kt
- HadithBookEntity.kt
- HadithChapterEntity.kt
- HadithEntity.kt
- HadithBookmarkEntity.kt
- DuaCategoryEntity.kt
- DuaEntity.kt
- DuaBookmarkEntity.kt
- DuaProgressEntity.kt
- PrayerRecordEntity.kt
- FastRecordEntity.kt
- MakeupFastEntity.kt
- TasbihPresetEntity.kt
- TasbihSessionEntity.kt
- LocationEntity.kt
- IslamicEventEntity.kt

**Create data/local/database/NimazDatabase.kt**:
```kotlin
package com.nimazpro.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nimazpro.app.data.local.database.dao.*
import com.nimazpro.app.data.local.database.entity.*

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        TranslationEntity::class,
        QuranBookmarkEntity::class,
        ReadingProgressEntity::class,
        HadithBookEntity::class,
        HadithChapterEntity::class,
        HadithEntity::class,
        HadithBookmarkEntity::class,
        DuaCategoryEntity::class,
        DuaEntity::class,
        DuaBookmarkEntity::class,
        DuaProgressEntity::class,
        PrayerRecordEntity::class,
        FastRecordEntity::class,
        MakeupFastEntity::class,
        TasbihPresetEntity::class,
        TasbihSessionEntity::class,
        LocationEntity::class,
        IslamicEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NimazDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun hadithDao(): HadithDao
    abstract fun duaDao(): DuaDao
    abstract fun prayerDao(): PrayerDao
    abstract fun fastDao(): FastDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun locationDao(): LocationDao
    abstract fun calendarDao(): CalendarDao
}
```

**Create core/di/DatabaseModule.kt**:
```kotlin
package com.nimazpro.app.core.di

import android.content.Context
import androidx.room.Room
import com.nimazpro.app.data.local.database.NimazDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NimazDatabase {
        return Room.databaseBuilder(
            context,
            NimazDatabase::class.java,
            "nimaz_database"
        )
        .createFromAsset("database/nimaz_prepopulated.db")
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    fun provideQuranDao(database: NimazDatabase) = database.quranDao()
    
    @Provides
    fun provideHadithDao(database: NimazDatabase) = database.hadithDao()
    
    @Provides
    fun provideDuaDao(database: NimazDatabase) = database.duaDao()
    
    @Provides
    fun providePrayerDao(database: NimazDatabase) = database.prayerDao()
    
    @Provides
    fun provideFastDao(database: NimazDatabase) = database.fastDao()
    
    @Provides
    fun provideTasbihDao(database: NimazDatabase) = database.tasbihDao()
    
    @Provides
    fun provideBookmarkDao(database: NimazDatabase) = database.bookmarkDao()
    
    @Provides
    fun provideLocationDao(database: NimazDatabase) = database.locationDao()
    
    @Provides
    fun provideCalendarDao(database: NimazDatabase) = database.calendarDao()
}
```

**Verification**: Database compiles without errors

---

### Task 5: Setup Navigation

**Objective**: Create type-safe navigation with all routes.

**Create core/navigation/Routes.kt**:
```kotlin
package com.nimazpro.app.core.navigation

import kotlinx.serialization.Serializable

sealed interface Routes {
    @Serializable data object Home : Routes
    @Serializable data object Quran : Routes
    @Serializable data class QuranReader(val surahId: Int) : Routes
    @Serializable data class SurahInfo(val surahId: Int) : Routes
    @Serializable data object Hadith : Routes
    @Serializable data class HadithReader(val bookId: Int, val hadithId: Int) : Routes
    @Serializable data object Duas : Routes
    @Serializable data class DuaReader(val categoryId: Int, val duaId: Int) : Routes
    @Serializable data object Tasbih : Routes
    @Serializable data object PrayerTracker : Routes
    @Serializable data object PrayerStats : Routes
    @Serializable data object Fasting : Routes
    @Serializable data object MakeupFasts : Routes
    @Serializable data object Zakat : Routes
    @Serializable data object Qibla : Routes
    @Serializable data object Calendar : Routes
    @Serializable data object Bookmarks : Routes
    @Serializable data class Search(val query: String = "") : Routes
    @Serializable data object More : Routes
    @Serializable data object Settings : Routes
    @Serializable data object PrayerSettings : Routes
    @Serializable data object NotificationSettings : Routes
    @Serializable data object QuranSettings : Routes
    @Serializable data object AppearanceSettings : Routes
    @Serializable data object Location : Routes
    @Serializable data object Language : Routes
    @Serializable data object Widgets : Routes
    @Serializable data object About : Routes
    @Serializable data object Onboarding : Routes
}
```

**Create core/navigation/NavGraph.kt**:
```kotlin
package com.nimazpro.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nimazpro.app.presentation.screens.home.HomeScreen
// Import all other screens...

@Composable
fun NimazNavGraph(
    navController: NavHostController,
    startDestination: Routes = Routes.Home
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Routes.Home> {
            HomeScreen(
                onNavigateToQuran = { navController.navigate(Routes.Quran) },
                onNavigateToHadith = { navController.navigate(Routes.Hadith) },
                onNavigateToDuas = { navController.navigate(Routes.Duas) },
                onNavigateToTasbih = { navController.navigate(Routes.Tasbih) },
                onNavigateToMore = { navController.navigate(Routes.More) }
            )
        }
        
        composable<Routes.Quran> {
            QuranHomeScreen(
                onNavigateToReader = { surahId -> 
                    navController.navigate(Routes.QuranReader(surahId)) 
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Routes.QuranReader> { backStackEntry ->
            val route = backStackEntry.toRoute<Routes.QuranReader>()
            QuranReaderScreen(
                surahId = route.surahId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInfo = { 
                    navController.navigate(Routes.SurahInfo(route.surahId)) 
                }
            )
        }
        
        // Add all other composable routes...
    }
}
```

**Update MainActivity.kt**:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NimazProTheme {
                val navController = rememberNavController()
                NimazNavGraph(navController = navController)
            }
        }
    }
}
```

**Verification**: App compiles with navigation setup (will show empty screens)

---

## Phase 2: Quran Module (Tasks 6-9)

### Task 6: Create Quran DAOs

**Create data/local/database/dao/QuranDao.kt**:
```kotlin
package com.nimazpro.app.data.local.database.dao

import androidx.room.*
import com.nimazpro.app.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    
    // Surahs
    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>
    
    @Query("SELECT * FROM surahs WHERE id = :id")
    fun getSurahById(id: Int): Flow<SurahEntity?>
    
    @Query("SELECT * FROM surahs WHERE name_english LIKE '%' || :query || '%' OR name_transliteration LIKE '%' || :query || '%'")
    fun searchSurahs(query: String): Flow<List<SurahEntity>>
    
    // Ayahs
    @Query("SELECT * FROM ayahs WHERE surah_id = :surahId ORDER BY number_in_surah ASC")
    fun getAyahsBySurah(surahId: Int): Flow<List<AyahEntity>>
    
    @Query("SELECT * FROM ayahs WHERE juz = :juz ORDER BY number_global ASC")
    fun getAyahsByJuz(juz: Int): Flow<List<AyahEntity>>
    
    @Query("SELECT * FROM ayahs WHERE page = :page ORDER BY number_global ASC")
    fun getAyahsByPage(page: Int): Flow<List<AyahEntity>>
    
    @Query("SELECT * FROM ayahs WHERE id = :id")
    fun getAyahById(id: Int): Flow<AyahEntity?>
    
    @Query("""
        SELECT a.*, s.name_english as surah_name, s.name_arabic as surah_name_arabic
        FROM ayahs a
        INNER JOIN surahs s ON a.surah_id = s.id
        WHERE a.text_arabic LIKE '%' || :query || '%'
        ORDER BY a.number_global ASC
        LIMIT 100
    """)
    fun searchAyahs(query: String): Flow<List<AyahWithSurah>>
    
    // Translations
    @Query("SELECT * FROM translations WHERE ayah_id = :ayahId AND translator_id = :translatorId")
    fun getTranslation(ayahId: Int, translatorId: String): Flow<TranslationEntity?>
    
    @Query("SELECT * FROM translations WHERE ayah_id IN (:ayahIds) AND translator_id = :translatorId")
    fun getTranslationsForAyahs(ayahIds: List<Int>, translatorId: String): Flow<List<TranslationEntity>>
    
    // Bookmarks
    @Query("SELECT * FROM quran_bookmarks ORDER BY created_at DESC")
    fun getAllBookmarks(): Flow<List<QuranBookmarkEntity>>
    
    @Query("SELECT * FROM quran_bookmarks WHERE surah_id = :surahId AND ayah_number = :ayahNumber")
    fun getBookmark(surahId: Int, ayahNumber: Int): Flow<QuranBookmarkEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: QuranBookmarkEntity)
    
    @Query("DELETE FROM quran_bookmarks WHERE surah_id = :surahId AND ayah_number = :ayahNumber")
    suspend fun deleteBookmark(surahId: Int, ayahNumber: Int)
    
    // Reading Progress
    @Query("SELECT * FROM reading_progress WHERE id = 1")
    fun getReadingProgress(): Flow<ReadingProgressEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateReadingProgress(progress: ReadingProgressEntity)
}

data class AyahWithSurah(
    @Embedded val ayah: AyahEntity,
    @ColumnInfo(name = "surah_name") val surahName: String,
    @ColumnInfo(name = "surah_name_arabic") val surahNameArabic: String
)
```

**Verification**: DAO compiles without errors

---

### Task 7: Create Quran Domain Layer

**Create domain/model/Surah.kt**:
```kotlin
package com.nimazpro.app.domain.model

data class Surah(
    val id: Int,
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameTransliteration: String,
    val revelationType: RevelationType,
    val versesCount: Int,
    val orderRevealed: Int,
    val startPage: Int
)

enum class RevelationType {
    MECCAN, MEDINAN
}

data class Ayah(
    val id: Int,
    val surahId: Int,
    val numberInSurah: Int,
    val numberGlobal: Int,
    val textArabic: String,
    val textUthmani: String,
    val juz: Int,
    val hizb: Int,
    val page: Int,
    val sajda: Boolean,
    val sajdaType: SajdaType?,
    val translation: String? = null
)

enum class SajdaType {
    RECOMMENDED, OBLIGATORY
}

data class ReadingProgress(
    val surahId: Int,
    val ayahNumber: Int,
    val updatedAt: Long
)
```

**Create domain/repository/QuranRepository.kt**:
```kotlin
package com.nimazpro.app.domain.repository

import com.nimazpro.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    fun getAllSurahs(): Flow<List<Surah>>
    fun getSurahById(id: Int): Flow<Surah?>
    fun getAyahsBySurah(surahId: Int, translatorId: String): Flow<List<Ayah>>
    fun getAyahsByJuz(juz: Int): Flow<List<Ayah>>
    fun searchQuran(query: String): Flow<List<SearchResult>>
    fun getBookmarks(): Flow<List<QuranBookmark>>
    fun isBookmarked(surahId: Int, ayahNumber: Int): Flow<Boolean>
    suspend fun toggleBookmark(surahId: Int, ayahNumber: Int, note: String?)
    fun getReadingProgress(): Flow<ReadingProgress?>
    suspend fun updateReadingProgress(surahId: Int, ayahNumber: Int)
}

data class QuranBookmark(
    val id: Int,
    val surahId: Int,
    val ayahNumber: Int,
    val surahName: String,
    val createdAt: Long,
    val note: String?
)

data class SearchResult(
    val type: SearchResultType,
    val title: String,
    val subtitle: String,
    val textArabic: String,
    val textTranslation: String,
    val reference: String
)

enum class SearchResultType {
    QURAN, HADITH, DUA
}
```

**Create data/repository/QuranRepositoryImpl.kt**:
```kotlin
package com.nimazpro.app.data.repository

import com.nimazpro.app.data.local.database.dao.QuranDao
import com.nimazpro.app.data.local.database.entity.QuranBookmarkEntity
import com.nimazpro.app.data.local.database.entity.ReadingProgressEntity
import com.nimazpro.app.domain.model.*
import com.nimazpro.app.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuranRepositoryImpl @Inject constructor(
    private val quranDao: QuranDao
) : QuranRepository {
    
    override fun getAllSurahs(): Flow<List<Surah>> {
        return quranDao.getAllSurahs().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getSurahById(id: Int): Flow<Surah?> {
        return quranDao.getSurahById(id).map { it?.toDomain() }
    }
    
    override fun getAyahsBySurah(surahId: Int, translatorId: String): Flow<List<Ayah>> {
        return combine(
            quranDao.getAyahsBySurah(surahId),
            quranDao.getTranslationsForAyahs(
                // This needs the ayah IDs - simplified for now
                emptyList(), translatorId
            )
        ) { ayahs, translations ->
            val translationMap = translations.associateBy { it.ayahId }
            ayahs.map { ayah ->
                ayah.toDomain(translationMap[ayah.id]?.text)
            }
        }
    }
    
    override fun getAyahsByJuz(juz: Int): Flow<List<Ayah>> {
        return quranDao.getAyahsByJuz(juz).map { entities ->
            entities.map { it.toDomain(null) }
        }
    }
    
    override fun searchQuran(query: String): Flow<List<SearchResult>> {
        return quranDao.searchAyahs(query).map { results ->
            results.map { 
                SearchResult(
                    type = SearchResultType.QURAN,
                    title = it.surahName,
                    subtitle = "Ayah ${it.ayah.numberInSurah}",
                    textArabic = it.ayah.textArabic,
                    textTranslation = "", // Would need translation joined
                    reference = "${it.surahName} ${it.ayah.surahId}:${it.ayah.numberInSurah}"
                )
            }
        }
    }
    
    override fun getBookmarks(): Flow<List<QuranBookmark>> {
        return quranDao.getAllBookmarks().map { bookmarks ->
            bookmarks.map { 
                QuranBookmark(
                    id = it.id,
                    surahId = it.surahId,
                    ayahNumber = it.ayahNumber,
                    surahName = "", // Would need join
                    createdAt = it.createdAt,
                    note = it.note
                )
            }
        }
    }
    
    override fun isBookmarked(surahId: Int, ayahNumber: Int): Flow<Boolean> {
        return quranDao.getBookmark(surahId, ayahNumber).map { it != null }
    }
    
    override suspend fun toggleBookmark(surahId: Int, ayahNumber: Int, note: String?) {
        val existing = quranDao.getBookmark(surahId, ayahNumber)
        // Toggle logic here
    }
    
    override fun getReadingProgress(): Flow<ReadingProgress?> {
        return quranDao.getReadingProgress().map { it?.toDomain() }
    }
    
    override suspend fun updateReadingProgress(surahId: Int, ayahNumber: Int) {
        quranDao.updateReadingProgress(
            ReadingProgressEntity(
                id = 1,
                lastSurahId = surahId,
                lastAyahNumber = ayahNumber
            )
        )
    }
    
    // Extension functions to convert entities to domain models
    private fun SurahEntity.toDomain() = Surah(
        id = id,
        number = number,
        nameArabic = nameArabic,
        nameEnglish = nameEnglish,
        nameTransliteration = nameTransliteration,
        revelationType = RevelationType.valueOf(revelationType),
        versesCount = versesCount,
        orderRevealed = orderRevealed,
        startPage = startPage
    )
    
    private fun AyahEntity.toDomain(translation: String?) = Ayah(
        id = id,
        surahId = surahId,
        numberInSurah = numberInSurah,
        numberGlobal = numberGlobal,
        textArabic = textArabic,
        textUthmani = textUthmani,
        juz = juz,
        hizb = hizb,
        page = page,
        sajda = sajda,
        sajdaType = sajdaType?.let { SajdaType.valueOf(it) },
        translation = translation
    )
    
    private fun ReadingProgressEntity.toDomain() = ReadingProgress(
        surahId = lastSurahId,
        ayahNumber = lastAyahNumber,
        updatedAt = updatedAt
    )
}
```

**Create use cases** in domain/usecase/quran/:
- GetSurahListUseCase.kt
- GetSurahWithAyahsUseCase.kt
- SearchQuranUseCase.kt
- ToggleQuranBookmarkUseCase.kt
- GetReadingProgressUseCase.kt
- UpdateReadingProgressUseCase.kt

**Verification**: Repository compiles and can be injected

---

### Task 8: Create Quran ViewModel

**Create presentation/screens/quran/QuranViewModel.kt**:
```kotlin
package com.nimazpro.app.presentation.screens.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimazpro.app.domain.model.*
import com.nimazpro.app.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuranHomeUiState(
    val surahs: List<Surah> = emptyList(),
    val readingProgress: ReadingProgress? = null,
    val bookmarks: List<QuranBookmark> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class QuranReaderUiState(
    val surah: Surah? = null,
    val ayahs: List<Ayah> = emptyList(),
    val currentAyah: Int = 1,
    val isBookmarked: Boolean = false,
    val isPlaying: Boolean = false,
    val fontSize: Int = 24,
    val showTranslation: Boolean = true,
    val showTransliteration: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class QuranEvent {
    data class LoadSurah(val surahId: Int) : QuranEvent()
    data class ToggleBookmark(val surahId: Int, val ayahNumber: Int) : QuranEvent()
    data class UpdateProgress(val surahId: Int, val ayahNumber: Int) : QuranEvent()
    data class SetFontSize(val size: Int) : QuranEvent()
    data object ToggleTranslation : QuranEvent()
    data object ToggleTransliteration : QuranEvent()
    data class Search(val query: String) : QuranEvent()
}

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranRepository: QuranRepository
) : ViewModel() {
    
    private val _homeState = MutableStateFlow(QuranHomeUiState())
    val homeState: StateFlow<QuranHomeUiState> = _homeState.asStateFlow()
    
    private val _readerState = MutableStateFlow(QuranReaderUiState())
    val readerState: StateFlow<QuranReaderUiState> = _readerState.asStateFlow()
    
    init {
        loadSurahs()
        loadReadingProgress()
        loadBookmarks()
    }
    
    fun onEvent(event: QuranEvent) {
        when (event) {
            is QuranEvent.LoadSurah -> loadSurah(event.surahId)
            is QuranEvent.ToggleBookmark -> toggleBookmark(event.surahId, event.ayahNumber)
            is QuranEvent.UpdateProgress -> updateProgress(event.surahId, event.ayahNumber)
            is QuranEvent.SetFontSize -> setFontSize(event.size)
            is QuranEvent.ToggleTranslation -> toggleTranslation()
            is QuranEvent.ToggleTransliteration -> toggleTransliteration()
            is QuranEvent.Search -> search(event.query)
        }
    }
    
    private fun loadSurahs() {
        viewModelScope.launch {
            quranRepository.getAllSurahs()
                .catch { e -> _homeState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { surahs ->
                    _homeState.update { it.copy(surahs = surahs, isLoading = false) }
                }
        }
    }
    
    private fun loadReadingProgress() {
        viewModelScope.launch {
            quranRepository.getReadingProgress().collect { progress ->
                _homeState.update { it.copy(readingProgress = progress) }
            }
        }
    }
    
    private fun loadBookmarks() {
        viewModelScope.launch {
            quranRepository.getBookmarks().collect { bookmarks ->
                _homeState.update { it.copy(bookmarks = bookmarks) }
            }
        }
    }
    
    private fun loadSurah(surahId: Int) {
        viewModelScope.launch {
            _readerState.update { it.copy(isLoading = true) }
            
            combine(
                quranRepository.getSurahById(surahId),
                quranRepository.getAyahsBySurah(surahId, "sahih_international")
            ) { surah, ayahs ->
                _readerState.update { 
                    it.copy(
                        surah = surah,
                        ayahs = ayahs,
                        isLoading = false
                    )
                }
            }.catch { e ->
                _readerState.update { it.copy(error = e.message, isLoading = false) }
            }.collect()
        }
    }
    
    private fun toggleBookmark(surahId: Int, ayahNumber: Int) {
        viewModelScope.launch {
            quranRepository.toggleBookmark(surahId, ayahNumber, null)
        }
    }
    
    private fun updateProgress(surahId: Int, ayahNumber: Int) {
        viewModelScope.launch {
            quranRepository.updateReadingProgress(surahId, ayahNumber)
        }
    }
    
    private fun setFontSize(size: Int) {
        _readerState.update { it.copy(fontSize = size.coerceIn(16, 40)) }
    }
    
    private fun toggleTranslation() {
        _readerState.update { it.copy(showTranslation = !it.showTranslation) }
    }
    
    private fun toggleTransliteration() {
        _readerState.update { it.copy(showTransliteration = !it.showTransliteration) }
    }
    
    private fun search(query: String) {
        // Implement search
    }
}
```

**Verification**: ViewModel compiles and can be injected into Compose screens

---

### Task 9: Create Pre-populated Database

**Objective**: Create the SQLite database file with Quran data to ship with the app.

Create a separate tool/script to generate the database, or create JSON files in `assets/` and parse on first launch.

**Option A - JSON Files** (Recommended for maintainability):

Create `assets/data/surahs.json`:
```json
[
  {
    "id": 1,
    "number": 1,
    "name_arabic": "الفاتحة",
    "name_english": "The Opening",
    "name_transliteration": "Al-Fatihah",
    "revelation_type": "MECCAN",
    "verses_count": 7,
    "order_revealed": 5,
    "start_page": 1
  },
  // ... all 114 surahs
]
```

Create `assets/data/ayahs.json` with all 6236 ayahs.

Create database population callback in `DatabaseModule.kt`.

**Option B - Pre-built SQLite file**:

Use DB Browser for SQLite to create `nimaz_prepopulated.db` and place in `assets/database/`.

**Verification**: App launches and displays list of surahs from database

---

## Continue with remaining phases...

The document continues with Tasks 10-35 covering:
- **Phase 3**: Hadith Module (Tasks 10-12)
- **Phase 4**: Dua Module (Tasks 13-15)
- **Phase 5**: Prayer Module (Tasks 16-19)
- **Phase 6**: Fasting Module (Tasks 20-22)
- **Phase 7**: Additional Features (Tasks 23-27)
- **Phase 8**: System Integration (Tasks 28-32)
- **Phase 9**: Testing & Polish (Tasks 33-35)

Each task follows the same pattern:
1. Create DAOs with queries
2. Create Repository interface and implementation
3. Create Use Cases
4. Create ViewModel with UI State
5. Pre-populate data if needed
6. Create minimal screen to verify data flow

---

## Final Checklist

Before completing the foundation, verify:

- [ ] All 20 Room entities created and compiling
- [ ] All 9 DAOs created with required queries
- [ ] All Repository interfaces and implementations
- [ ] All ViewModels exposing StateFlow
- [ ] Navigation working between all screens
- [ ] Pre-populated database loading correctly
- [ ] Prayer time calculation working
- [ ] Hijri date conversion working
- [ ] DataStore preferences saving/loading
- [ ] Unit tests passing for critical paths
- [ ] App builds in release mode without errors

---

## Data Sources

For pre-populating the database, use these sources:

1. **Quran Text**: https://tanzil.net/download
2. **Translations**: https://quran.com (Sahih International)
3. **Hadith**: https://sunnah.com API or their GitHub data
4. **Duas**: Compile from Fortress of the Muslim (Hisnul Muslim)
5. **Islamic Events**: Standard Hijri calendar events

---

**Document Version**: 1.0
**Last Updated**: January 2026
**Author**: Claude Code Assistant
