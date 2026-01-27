# NIMAZ PRO
## Android Technical Foundation Document
### Complete Offline-First Islamic Companion App

**Version 1.0 | January 2026**

---

# Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Technology Stack & Dependencies](#2-technology-stack--dependencies)
3. [Project Structure & Architecture](#3-project-structure--architecture)
4. [Database Schema (Complete)](#4-database-schema-complete)
5. [Design System Implementation](#5-design-system-implementation)
6. [Component Inventory](#6-component-inventory)
7. [Claude Code Task List](#7-claude-code-task-list)
8. [Screen Data Requirements](#8-screen-data-requirements)
9. [Testing Strategy](#9-testing-strategy)
10. [Git Workflow & Changelog](#10-git-workflow--changelog)

---

# 1. Executive Summary

Nimaz Pro is a **100% offline-capable** Islamic companion app built with the latest Android technologies. This document provides the complete technical foundation for building the app, including database schemas, architecture patterns, and a comprehensive task list for Claude Code to execute.

## 1.1 Core Principles

- **Offline-First**: All data stored locally in Room database, no internet required for core features
- **Clean Architecture**: Domain, Data, and Presentation layers with clear separation
- **MVVM + UDF**: Unidirectional data flow with ViewModels exposing StateFlow
- **Compose-First**: Modern declarative UI with Material 3
- **Free & Open**: No premium features, no subscriptions, everything unlocked

## 1.2 Feature Set

- **Prayer Times**: Accurate calculation for any location worldwide
- **Quran**: Complete text with translations, audio, bookmarks
- **Hadith**: Kutub al-Sittah with search and bookmarks
- **Duas & Adhkar**: Daily supplications with audio
- **Tasbih Counter**: Digital counter with presets
- **Prayer Tracker**: Track daily prayers with statistics
- **Fasting Tracker**: Ramadan and voluntary fasts
- **Zakat Calculator**: Calculate obligations
- **Qibla Compass**: Find direction to Mecca
- **Islamic Calendar**: Hijri date conversion and events
- **Widgets**: Home screen widgets for prayer times

---

# 2. Technology Stack & Dependencies

## 2.1 Core Android

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.0.0+ | Primary language |
| Jetpack Compose | 1.7.0+ | Declarative UI |
| Compose Material 3 | 1.3.0+ | Material Design 3 components |
| Compose Navigation | 2.8.0+ | Type-safe navigation |
| Min SDK | 26 | Android 8.0 Oreo |
| Target SDK | 35 | Android 15 |

## 2.2 Architecture & DI

| Library | Version | Purpose |
|---------|---------|---------|
| Hilt | 2.57.1 | Dependency injection |
| Hilt Navigation Compose | 1.3.0 | ViewModel injection in Compose |
| ViewModel | 2.8.0+ | UI state management |
| Lifecycle | 2.8.0+ | Lifecycle-aware components |

## 2.3 Data & Storage

| Library | Version | Purpose |
|---------|---------|---------|
| Room | 2.8.4 | SQLite abstraction (with KSP) |
| DataStore | 1.1.0 | Preferences storage |
| KSP | 2.0.0-1.0.24 | Kotlin Symbol Processing |

## 2.4 Async & Reactive

| Library | Version | Purpose |
|---------|---------|---------|
| Coroutines | 1.8.0+ | Async programming |
| Flow | Built-in | Reactive streams |

## 2.5 Islamic Libraries

| Library | Source | Purpose |
|---------|--------|---------|
| Adhan Kotlin | com.batoulapps.adhan:adhan2 | Prayer time calculations |
| Hijri Calendar | Custom implementation | Umm al-Qura calendar |

## 2.6 Other Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Glance | 1.1.0 | App widgets |
| WorkManager | 2.9.0 | Background tasks |
| Location Services | 21.2.0 | GPS location |
| Media3 ExoPlayer | 1.3.0 | Audio playback |
| Accompanist | 0.34.0 | Compose utilities |

## 2.7 build.gradle.kts (App Module)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("androidx.room")
}

android {
    namespace = "com.nimazpro.app"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.nimazpro.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildFeatures {
        compose = true
    }
    
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-android-compiler:2.57.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    
    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    
    // Prayer Times
    implementation("com.batoulapps.adhan:adhan2:0.0.6")
    
    // Widgets
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    
    // Media
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    
    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
```

---

# 3. Project Structure & Architecture

## 3.1 Clean Architecture Layers

The app follows Clean Architecture with three main layers:

- **Presentation Layer**: Compose UI, ViewModels, UI State
- **Domain Layer**: Use Cases, Domain Models, Repository Interfaces
- **Data Layer**: Repository Implementations, Room DAOs, DataStore

## 3.2 Package Structure

```
com.nimazpro.app/
├── NimazProApp.kt                    # @HiltAndroidApp
├── MainActivity.kt                   # @AndroidEntryPoint, single activity
│
├── core/                             # Shared utilities
│   ├── di/                           # Hilt modules
│   │   ├── DatabaseModule.kt
│   │   ├── RepositoryModule.kt
│   │   └── UseCaseModule.kt
│   ├── util/
│   │   ├── DateTimeUtil.kt
│   │   ├── HijriDateConverter.kt
│   │   └── Extensions.kt
│   └── navigation/
│       ├── NavGraph.kt
│       └── Routes.kt
│
├── data/                             # Data layer
│   ├── local/
│   │   ├── database/
│   │   │   ├── NimazDatabase.kt
│   │   │   ├── dao/
│   │   │   │   ├── QuranDao.kt
│   │   │   │   ├── HadithDao.kt
│   │   │   │   ├── DuaDao.kt
│   │   │   │   ├── PrayerDao.kt
│   │   │   │   ├── FastDao.kt
│   │   │   │   └── BookmarkDao.kt
│   │   │   └── entity/
│   │   │       ├── SurahEntity.kt
│   │   │       ├── AyahEntity.kt
│   │   │       ├── HadithEntity.kt
│   │   │       ├── DuaEntity.kt
│   │   │       ├── PrayerRecordEntity.kt
│   │   │       ├── FastRecordEntity.kt
│   │   │       └── BookmarkEntity.kt
│   │   └── datastore/
│   │       └── PreferencesDataStore.kt
│   └── repository/
│       ├── QuranRepositoryImpl.kt
│       ├── HadithRepositoryImpl.kt
│       ├── PrayerRepositoryImpl.kt
│       └── SettingsRepositoryImpl.kt
│
├── domain/                           # Domain layer
│   ├── model/
│   │   ├── Surah.kt
│   │   ├── Ayah.kt
│   │   ├── Hadith.kt
│   │   ├── Dua.kt
│   │   ├── PrayerTime.kt
│   │   ├── PrayerRecord.kt
│   │   └── FastRecord.kt
│   ├── repository/
│   │   ├── QuranRepository.kt
│   │   ├── HadithRepository.kt
│   │   ├── PrayerRepository.kt
│   │   └── SettingsRepository.kt
│   └── usecase/
│       ├── quran/
│       │   ├── GetSurahListUseCase.kt
│       │   ├── GetAyahsUseCase.kt
│       │   └── SearchQuranUseCase.kt
│       ├── hadith/
│       │   ├── GetHadithCollectionsUseCase.kt
│       │   └── SearchHadithUseCase.kt
│       ├── prayer/
│       │   ├── CalculatePrayerTimesUseCase.kt
│       │   ├── GetPrayerRecordsUseCase.kt
│       │   └── LogPrayerUseCase.kt
│       └── settings/
│           └── GetSettingsUseCase.kt
│
├── presentation/                     # Presentation layer
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Shape.kt
│   ├── components/                   # Reusable UI components
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
│
└── widget/                           # App Widgets
    ├── PrayerTimesWidget.kt
    └── PrayerTimesWidgetReceiver.kt
```

---

# 4. Database Schema (Complete)

All data is stored locally in a Room database. Pre-populated data (Quran, Hadith, Duas) is shipped with the app as a pre-built database file.

## 4.1 Entity Relationship Diagram

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     SURAH       │     │      AYAH       │     │   TRANSLATION   │
├─────────────────┤     ├─────────────────┤     ├─────────────────┤
│ id (PK)         │←───┐│ id (PK)         │←───┐│ id (PK)         │
│ number          │    ││ surah_id (FK)   │────┘│ ayah_id (FK)    │────┐
│ name_arabic     │    ││ number_in_surah │     │ translator_id   │    │
│ name_english    │    ││ text_arabic     │     │ text            │    │
│ name_transliter │    ││ juz             │     └─────────────────┘    │
│ revelation_type │    ││ page            │                            │
│ verses_count    │    │└─────────────────┘                            │
│ order_revealed  │    │                                               │
└─────────────────┘    │                                               │
                       │     ┌─────────────────┐                       │
┌─────────────────┐    │     │    BOOKMARK     │                       │
│  HADITH_BOOK    │    │     ├─────────────────┤                       │
├─────────────────┤    │     │ id (PK)         │                       │
│ id (PK)         │←──┐│     │ type (enum)     │  QURAN|HADITH|DUA    │
│ name_english    │   ││     │ reference_id    │───────────────────────┘
│ name_arabic     │   ││     │ created_at      │
│ author          │   ││     │ note            │
│ hadith_count    │   ││     └─────────────────┘
└─────────────────┘   ││
                      ││     ┌─────────────────┐
┌─────────────────┐   ││     │  PRAYER_RECORD  │
│     HADITH      │   ││     ├─────────────────┤
├─────────────────┤   ││     │ id (PK)         │
│ id (PK)         │←─┐││     │ date            │
│ book_id (FK)    │──┘││     │ prayer_type     │  FAJR|DHUHR|ASR|...
│ chapter         │   ││     │ prayed          │  BOOLEAN
│ number          │   ││     │ on_time         │  BOOLEAN
│ text_arabic     │   ││     │ in_jamaat       │  BOOLEAN
│ text_english    │   ││     └─────────────────┘
│ narrator        │   ││
│ grade           │   ││     ┌─────────────────┐
│ reference       │   ││     │   FAST_RECORD   │
└─────────────────┘   ││     ├─────────────────┤
                      ││     │ id (PK)         │
┌─────────────────┐   ││     │ hijri_date      │
│  DUA_CATEGORY   │   ││     │ gregorian_date  │
├─────────────────┤   ││     │ type            │  RAMADAN|VOLUNTARY|MAKEUP
│ id (PK)         │←─┐││     │ status          │  FASTED|MISSED|EXEMPT
│ name_english    │  │││     │ reason          │  (for exempt/missed)
│ name_arabic     │  │││     └─────────────────┘
│ icon            │  │││
│ order           │  │││     ┌─────────────────┐
└─────────────────┘  │││     │  TASBIH_PRESET  │
                     │││     ├─────────────────┤
┌─────────────────┐  │││     │ id (PK)         │
│       DUA       │  │││     │ name            │
├─────────────────┤  │││     │ arabic          │
│ id (PK)         │←┐│││     │ transliteration │
│ category_id(FK) │─┘│││     │ translation     │
│ title           │  │││     │ target_count    │
│ text_arabic     │  │││     │ is_custom       │
│ transliteration │  │││     └─────────────────┘
│ translation     │  │││
│ source          │  │││     ┌─────────────────┐
│ audio_file      │  │││     │ TASBIH_SESSION  │
│ repeat_count    │  │││     ├─────────────────┤
│ virtue          │  │││     │ id (PK)         │
└─────────────────┘  │││     │ preset_id (FK)  │
                     │││     │ count           │
┌─────────────────┐  │││     │ completed_at    │
│ ISLAMIC_EVENT   │  │││     └─────────────────┘
├─────────────────┤  │││
│ id (PK)         │  │││     ┌─────────────────┐
│ name            │  │││     │    LOCATION     │
│ hijri_month     │  │││     ├─────────────────┤
│ hijri_day       │  │││     │ id (PK)         │
│ type            │  │││     │ name            │
│ description     │  │││     │ country         │
│ is_holiday      │  │││     │ latitude        │
└─────────────────┘  │││     │ longitude       │
                     │││     │ timezone        │
                     │││     │ is_current      │
                     │││     └─────────────────┘
```

## 4.2 Room Entity Definitions

### 4.2.1 Quran Entities

```kotlin
// SurahEntity.kt
@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "number") val number: Int,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    @ColumnInfo(name = "name_transliteration") val nameTransliteration: String,
    @ColumnInfo(name = "revelation_type") val revelationType: String, // MECCAN or MEDINAN
    @ColumnInfo(name = "verses_count") val versesCount: Int,
    @ColumnInfo(name = "order_revealed") val orderRevealed: Int,
    @ColumnInfo(name = "start_page") val startPage: Int
)

// AyahEntity.kt
@Entity(
    tableName = "ayahs",
    foreignKeys = [ForeignKey(
        entity = SurahEntity::class,
        parentColumns = ["id"],
        childColumns = ["surah_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("surah_id"), Index("juz"), Index("page")]
)
data class AyahEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "surah_id") val surahId: Int,
    @ColumnInfo(name = "number_in_surah") val numberInSurah: Int,
    @ColumnInfo(name = "number_global") val numberGlobal: Int,
    @ColumnInfo(name = "text_arabic") val textArabic: String,
    @ColumnInfo(name = "text_uthmani") val textUthmani: String,
    @ColumnInfo(name = "juz") val juz: Int,
    @ColumnInfo(name = "hizb") val hizb: Int,
    @ColumnInfo(name = "page") val page: Int,
    @ColumnInfo(name = "sajda") val sajda: Boolean = false,
    @ColumnInfo(name = "sajda_type") val sajdaType: String? = null
)

// TranslationEntity.kt
@Entity(
    tableName = "translations",
    foreignKeys = [ForeignKey(
        entity = AyahEntity::class,
        parentColumns = ["id"],
        childColumns = ["ayah_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("ayah_id"), Index("translator_id")]
)
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "ayah_id") val ayahId: Int,
    @ColumnInfo(name = "translator_id") val translatorId: String,
    @ColumnInfo(name = "text") val text: String
)

// QuranBookmarkEntity.kt
@Entity(
    tableName = "quran_bookmarks",
    indices = [Index("surah_id"), Index("ayah_number")]
)
data class QuranBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "surah_id") val surahId: Int,
    @ColumnInfo(name = "ayah_number") val ayahNumber: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "note") val note: String? = null
)

// ReadingProgressEntity.kt
@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "last_surah_id") val lastSurahId: Int,
    @ColumnInfo(name = "last_ayah_number") val lastAyahNumber: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
```

### 4.2.2 Hadith Entities

```kotlin
// HadithBookEntity.kt
@Entity(tableName = "hadith_books")
data class HadithBookEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "author") val author: String,
    @ColumnInfo(name = "hadith_count") val hadithCount: Int,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "icon") val icon: String
)

// HadithChapterEntity.kt
@Entity(
    tableName = "hadith_chapters",
    foreignKeys = [ForeignKey(
        entity = HadithBookEntity::class,
        parentColumns = ["id"],
        childColumns = ["book_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("book_id")]
)
data class HadithChapterEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "book_id") val bookId: Int,
    @ColumnInfo(name = "number") val number: Int,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "hadith_count") val hadithCount: Int
)

// HadithEntity.kt
@Entity(
    tableName = "hadiths",
    foreignKeys = [ForeignKey(
        entity = HadithChapterEntity::class,
        parentColumns = ["id"],
        childColumns = ["chapter_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("chapter_id"), Index("book_id"), Index("grade")]
)
data class HadithEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "book_id") val bookId: Int,
    @ColumnInfo(name = "chapter_id") val chapterId: Int,
    @ColumnInfo(name = "number_in_book") val numberInBook: Int,
    @ColumnInfo(name = "number_in_chapter") val numberInChapter: Int,
    @ColumnInfo(name = "text_arabic") val textArabic: String,
    @ColumnInfo(name = "text_english") val textEnglish: String,
    @ColumnInfo(name = "narrator") val narrator: String,
    @ColumnInfo(name = "grade") val grade: String,
    @ColumnInfo(name = "reference") val reference: String
)

// HadithBookmarkEntity.kt
@Entity(
    tableName = "hadith_bookmarks",
    indices = [Index("hadith_id")]
)
data class HadithBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "hadith_id") val hadithId: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "note") val note: String? = null
)
```

### 4.2.3 Dua Entities

```kotlin
// DuaCategoryEntity.kt
@Entity(tableName = "dua_categories")
data class DuaCategoryEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "icon") val icon: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "dua_count") val duaCount: Int
)

// DuaEntity.kt
@Entity(
    tableName = "duas",
    foreignKeys = [ForeignKey(
        entity = DuaCategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("category_id")]
)
data class DuaEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "category_id") val categoryId: Int,
    @ColumnInfo(name = "title_english") val titleEnglish: String,
    @ColumnInfo(name = "title_arabic") val titleArabic: String,
    @ColumnInfo(name = "text_arabic") val textArabic: String,
    @ColumnInfo(name = "transliteration") val transliteration: String,
    @ColumnInfo(name = "translation") val translation: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "virtue") val virtue: String?,
    @ColumnInfo(name = "repeat_count") val repeatCount: Int = 1,
    @ColumnInfo(name = "audio_file") val audioFile: String?,
    @ColumnInfo(name = "display_order") val displayOrder: Int
)
```

### 4.2.4 Prayer & Fasting Entities

```kotlin
// PrayerRecordEntity.kt
@Entity(
    tableName = "prayer_records",
    indices = [Index(value = ["date", "prayer_type"], unique = true)]
)
data class PrayerRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "date") val date: String, // YYYY-MM-DD
    @ColumnInfo(name = "prayer_type") val prayerType: String,
    @ColumnInfo(name = "prayed") val prayed: Boolean = false,
    @ColumnInfo(name = "on_time") val onTime: Boolean = false,
    @ColumnInfo(name = "in_jamaah") val inJamaah: Boolean = false,
    @ColumnInfo(name = "logged_at") val loggedAt: Long = System.currentTimeMillis()
)

// FastRecordEntity.kt
@Entity(
    tableName = "fast_records",
    indices = [Index(value = ["gregorian_date"], unique = true)]
)
data class FastRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "hijri_date") val hijriDate: String,
    @ColumnInfo(name = "gregorian_date") val gregorianDate: String,
    @ColumnInfo(name = "fast_type") val fastType: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "reason") val reason: String? = null,
    @ColumnInfo(name = "makeup_for_date") val makeupForDate: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// MakeupFastEntity.kt
@Entity(tableName = "makeup_fasts")
data class MakeupFastEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "original_date") val originalDate: String,
    @ColumnInfo(name = "ramadan_year") val ramadanYear: Int,
    @ColumnInfo(name = "reason") val reason: String,
    @ColumnInfo(name = "completed") val completed: Boolean = false,
    @ColumnInfo(name = "completed_date") val completedDate: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

### 4.2.5 Tasbih & Settings Entities

```kotlin
// TasbihPresetEntity.kt
@Entity(tableName = "tasbih_presets")
data class TasbihPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "arabic") val arabic: String,
    @ColumnInfo(name = "transliteration") val transliteration: String,
    @ColumnInfo(name = "translation") val translation: String,
    @ColumnInfo(name = "target_count") val targetCount: Int = 33,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean = false,
    @ColumnInfo(name = "display_order") val displayOrder: Int = 0
)

// TasbihSessionEntity.kt
@Entity(
    tableName = "tasbih_sessions",
    foreignKeys = [ForeignKey(
        entity = TasbihPresetEntity::class,
        parentColumns = ["id"],
        childColumns = ["preset_id"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("preset_id")]
)
data class TasbihSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "preset_id") val presetId: Int?,
    @ColumnInfo(name = "count") val count: Int,
    @ColumnInfo(name = "completed_at") val completedAt: Long = System.currentTimeMillis()
)

// LocationEntity.kt
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "country") val country: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "timezone") val timezone: String,
    @ColumnInfo(name = "is_current") val isCurrent: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// IslamicEventEntity.kt
@Entity(tableName = "islamic_events")
data class IslamicEventEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "hijri_month") val hijriMonth: Int,
    @ColumnInfo(name = "hijri_day") val hijriDay: Int,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "is_holiday") val isHoliday: Boolean = false
)
```

---

# 5. Design System Implementation

## 5.1 Color Palette

```kotlin
// Color.kt
object NimazColors {
    // Primary - Teal
    val Primary50 = Color(0xFFF0FDFA)
    val Primary100 = Color(0xFFCCFBF1)
    val Primary200 = Color(0xFF99F6E4)
    val Primary300 = Color(0xFF5EEAD4)
    val Primary400 = Color(0xFF2DD4BF)
    val Primary500 = Color(0xFF14B8A6) // Main
    val Primary600 = Color(0xFF0D9488)
    val Primary700 = Color(0xFF0F766E)
    val Primary800 = Color(0xFF115E59)
    val Primary900 = Color(0xFF134E4A)
    
    // Accent - Gold
    val Gold50 = Color(0xFFFEFCE8)
    val Gold100 = Color(0xFFFEF9C3)
    val Gold300 = Color(0xFFFDE047)
    val Gold500 = Color(0xFFEAB308) // Main
    val Gold700 = Color(0xFFA16207)
    
    // Prayer Colors
    val Fajr = Color(0xFF6366F1)     // Indigo
    val Dhuhr = Color(0xFFEAB308)    // Gold
    val Asr = Color(0xFFF97316)      // Orange
    val Maghrib = Color(0xFFEF4444)  // Red
    val Isha = Color(0xFF8B5CF6)     // Purple
    
    // Neutrals (Warm Stone)
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

## 5.2 Typography

```kotlin
// Type.kt
val NimazTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.outfit_bold)),
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 56.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.outfit_semibold)),
        fontSize = 36.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.outfit_bold)),
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.outfit_semibold)),
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.plus_jakarta_sans_regular)),
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.plus_jakarta_sans_regular)),
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.plus_jakarta_sans_medium)),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily(Font(R.font.plus_jakarta_sans_medium)),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
)

// Arabic Typography
val ArabicFontFamily = FontFamily(Font(R.font.amiri_regular))
val ArabicBoldFontFamily = FontFamily(Font(R.font.amiri_bold))
```

## 5.3 Theme

```kotlin
// Theme.kt
private val DarkColorScheme = darkColorScheme(
    primary = NimazColors.Primary500,
    onPrimary = Color.White,
    primaryContainer = NimazColors.Primary800,
    onPrimaryContainer = NimazColors.Primary100,
    secondary = NimazColors.Gold500,
    onSecondary = Color.Black,
    background = NimazColors.Neutral950,
    onBackground = Color.White,
    surface = NimazColors.Neutral900,
    onSurface = Color.White,
    surfaceVariant = NimazColors.Neutral800,
    onSurfaceVariant = NimazColors.Neutral300,
    outline = NimazColors.Neutral700,
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
    background = NimazColors.Neutral50,
    onBackground = NimazColors.Neutral900,
    surface = Color.White,
    onSurface = NimazColors.Neutral900,
    surfaceVariant = NimazColors.Neutral100,
    onSurfaceVariant = NimazColors.Neutral600,
    outline = NimazColors.Neutral300,
    error = NimazColors.Error,
    onError = Color.White
)

@Composable
fun NimazProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NimazTypography,
        shapes = NimazShapes,
        content = content
    )
}
```

---

# 6. Component Inventory

Components follow Atomic Design principles: Atoms → Molecules → Organisms → Screens

## 6.1 Atoms (Basic Building Blocks)

| Component | File | Description |
|-----------|------|-------------|
| NimazButton | atoms/NimazButton.kt | Primary, Secondary, Outlined, Text variants |
| NimazIconButton | atoms/NimazIconButton.kt | Circular icon buttons |
| NimazCard | atoms/NimazCard.kt | Elevated, Filled, Outlined cards |
| NimazText | atoms/NimazText.kt | Styled text with Arabic support |
| ArabicText | atoms/ArabicText.kt | RTL Arabic text with Amiri font |
| NimazTextField | atoms/NimazTextField.kt | Text input with validation |
| NimazSwitch | atoms/NimazSwitch.kt | Toggle switch |
| NimazSlider | atoms/NimazSlider.kt | Value slider |
| NimazChip | atoms/NimazChip.kt | Filter and selection chips |
| NimazBadge | atoms/NimazBadge.kt | Status badges (Sahih, PRO, etc.) |
| NimazDivider | atoms/NimazDivider.kt | Horizontal/vertical dividers |
| NimazProgressBar | atoms/NimazProgressBar.kt | Linear progress indicator |
| NimazCircularProgress | atoms/NimazCircularProgress.kt | Circular progress/loading |
| PrayerColorIndicator | atoms/PrayerColorIndicator.kt | Colored bar for prayer type |

## 6.2 Molecules (Composed Components)

| Component | File | Description |
|-----------|------|-------------|
| PrayerTimeCard | molecules/PrayerTimeCard.kt | Single prayer with time, color, status |
| PrayerTimesRow | molecules/PrayerTimesRow.kt | Horizontal list of all 5 prayers |
| CountdownTimer | molecules/CountdownTimer.kt | Next prayer countdown display |
| SurahListItem | molecules/SurahListItem.kt | Surah row with number, name, verses |
| AyahCard | molecules/AyahCard.kt | Verse with Arabic, translation, actions |
| HadithListItem | molecules/HadithListItem.kt | Hadith preview in list |
| HadithCard | molecules/HadithCard.kt | Full hadith display |
| DuaListItem | molecules/DuaListItem.kt | Dua in category list |
| DuaCard | molecules/DuaCard.kt | Full dua with counter |
| TasbihCounter | molecules/TasbihCounter.kt | Animated counter ring |
| TasbihPresetCard | molecules/TasbihPresetCard.kt | Preset dhikr selection |
| BookmarkItem | molecules/BookmarkItem.kt | Bookmark list item |
| SearchResultItem | molecules/SearchResultItem.kt | Search result with highlighting |
| CalendarDay | molecules/CalendarDay.kt | Single day in calendar grid |
| IslamicEventCard | molecules/IslamicEventCard.kt | Upcoming event display |
| StatCard | molecules/StatCard.kt | Statistics display card |
| SettingsItem | molecules/SettingsItem.kt | Settings row with icon, label, action |
| SettingsSection | molecules/SettingsSection.kt | Grouped settings with header |
| LocationItem | molecules/LocationItem.kt | Location in list |
| ReciterItem | molecules/ReciterItem.kt | Audio reciter selection |

## 6.3 Organisms (Complex Components)

| Component | File | Description |
|-----------|------|-------------|
| PrayerTimesSection | organisms/PrayerTimesSection.kt | Complete prayer times with countdown |
| QuranReader | organisms/QuranReader.kt | Scrollable ayah list with audio controls |
| HadithReader | organisms/HadithReader.kt | Full hadith with isnad, related |
| DuaReader | organisms/DuaReader.kt | Dua with audio, counter, navigation |
| CalendarGrid | organisms/CalendarGrid.kt | Monthly calendar view |
| PrayerStatsChart | organisms/PrayerStatsChart.kt | Weekly/monthly prayer charts |
| FastingCalendar | organisms/FastingCalendar.kt | Ramadan tracker calendar |
| QiblaCompass | organisms/QiblaCompass.kt | Animated compass with direction |
| ZakatForm | organisms/ZakatForm.kt | Asset input form |
| BottomNavBar | organisms/BottomNavBar.kt | Main navigation bar (5 items) |
| TopAppBar | organisms/TopAppBar.kt | Screen header with back, title, actions |
| SearchBar | organisms/SearchBar.kt | Expandable search input |
| AudioPlayer | organisms/AudioPlayer.kt | Mini audio player for Quran/Dua |
| OnboardingSlide | organisms/OnboardingSlide.kt | Single onboarding page |

## 6.4 Screen Summary (29 Screens)

| # | Screen | Route | ViewModel |
|---|--------|-------|-----------|
| 1 | Home | home | HomeViewModel |
| 2 | Quran Home | quran | QuranViewModel |
| 3 | Quran Reader | quran/{surahId} | QuranViewModel |
| 4 | Surah Info | quran/{surahId}/info | QuranViewModel |
| 5 | Hadith Collection | hadith | HadithViewModel |
| 6 | Hadith Reader | hadith/{bookId}/{hadithId} | HadithViewModel |
| 7 | Duas Collection | duas | DuaViewModel |
| 8 | Dua Reader | duas/{categoryId}/{duaId} | DuaViewModel |
| 9 | Tasbih | tasbih | TasbihViewModel |
| 10 | Prayer Tracker | prayer-tracker | PrayerViewModel |
| 11 | Prayer Statistics | prayer-stats | PrayerViewModel |
| 12 | Fast Tracker | fasting | FastViewModel |
| 13 | Makeup Fasts | fasting/makeup | FastViewModel |
| 14 | Zakat Calculator | zakat | ZakatViewModel |
| 15 | Qibla Compass | qibla | QiblaViewModel |
| 16 | Islamic Calendar | calendar | CalendarViewModel |
| 17 | Bookmarks | bookmarks | BookmarksViewModel |
| 18 | Search Results | search?query={q} | SearchViewModel |
| 19 | More/Menu | more | SettingsViewModel |
| 20 | Settings | settings | SettingsViewModel |
| 21 | Prayer Settings | settings/prayer | SettingsViewModel |
| 22 | Notification Settings | settings/notifications | SettingsViewModel |
| 23 | Quran Settings | settings/quran | SettingsViewModel |
| 24 | Appearance Settings | settings/appearance | SettingsViewModel |
| 25 | Location | settings/location | SettingsViewModel |
| 26 | Language | settings/language | SettingsViewModel |
| 27 | Widgets Preview | settings/widgets | SettingsViewModel |
| 28 | About | about | — |
| 29 | Onboarding | onboarding | OnboardingViewModel |

---

# 7. Claude Code Task List

The following tasks should be executed in order. Each task creates a complete, tested piece of the foundation. After all tasks, the app will have 100% data functionality with minimal placeholder UI.

## Phase 1: Project Setup (Tasks 1-5)

### Task 1: Create Android Project

Create new Android Studio project with Empty Compose Activity. Package: `com.nimazpro.app`. Min SDK 26, Target SDK 35.

- Create project structure as defined in Section 3.2
- Add all dependencies from Section 2.7 to build.gradle.kts
- Configure KSP for Room and Hilt
- Create NimazProApp.kt with @HiltAndroidApp annotation
- Create MainActivity.kt with @AndroidEntryPoint annotation

### Task 2: Setup Theme & Design System

- Create Color.kt with NimazColors object (from Section 5.1)
- Create Type.kt with NimazTypography (from Section 5.2)
- Create Shape.kt with NimazShapes (corner radii: 8dp, 12dp, 16dp, 20dp)
- Create Theme.kt with light/dark color schemes (from Section 5.3)
- Add font files to res/font/ (Outfit, Plus Jakarta Sans, Amiri)

### Task 3: Setup Room Database

- Create all Entity classes (from Section 4.2)
- Create NimazDatabase.kt with @Database annotation listing all entities
- Create DatabaseModule.kt Hilt module providing database instance
- Configure Room schema export directory
- Set database version to 1

### Task 4: Setup DataStore Preferences

- Create PreferencesDataStore.kt for app settings
- Define preference keys: calculationMethod, madhab, theme, language, notificationEnabled, etc.
- Create PreferencesRepository interface and implementation
- Add to Hilt module

### Task 5: Setup Navigation

- Create Routes.kt sealed class with all route definitions
- Create NavGraph.kt with NavHost and all screen composables (placeholder)
- Setup type-safe navigation with arguments
- Create BottomNavBar organism for main navigation

## Phase 2: Quran Module (Tasks 6-9)

### Task 6: Quran DAOs

Create QuranDao.kt with queries:
- `getAllSurahs(): Flow<List<SurahEntity>>`
- `getSurahById(id: Int): Flow<SurahEntity>`
- `getAyahsBySurah(surahId: Int): Flow<List<AyahEntity>>`
- `getAyahsByJuz(juz: Int): Flow<List<AyahWithSurah>>`
- `getAyahsByPage(page: Int): Flow<List<AyahWithSurah>>`
- `searchAyahs(query: String): Flow<List<AyahWithSurah>>`
- `getTranslation(ayahId: Int, translatorId: String): Flow<TranslationEntity>`

Create QuranBookmarkDao for bookmark operations.
Create ReadingProgressDao for continue reading.

### Task 7: Quran Repository & Use Cases

- Create QuranRepository interface in domain/repository/
- Create QuranRepositoryImpl implementing the interface
- Create use cases:
  - GetSurahListUseCase
  - GetSurahWithAyahsUseCase
  - GetAyahsByJuzUseCase
  - SearchQuranUseCase
  - ToggleBookmarkUseCase
  - GetReadingProgressUseCase
  - UpdateReadingProgressUseCase
- Add to RepositoryModule and UseCaseModule

### Task 8: Quran ViewModel

Create QuranUiState data class with:
- surahList, currentSurah, ayahs, isLoading, error, bookmarks, readingProgress

Create QuranViewModel with @HiltViewModel:
- Inject all Quran use cases
- Expose StateFlow<QuranUiState>
- Handle events: loadSurah, toggleBookmark, updateProgress, search

### Task 9: Quran Pre-populated Database

- Create JSON files for Quran data: surahs.json, ayahs.json, translations.json
- Create DatabaseCallback to pre-populate on first launch
- Place database file in assets/ or use JSON parsing
- Include Sahih International translation

## Phase 3: Hadith Module (Tasks 10-12)

### Task 10: Hadith DAOs

- Create HadithDao.kt with queries for books, chapters, hadiths
- Create HadithBookmarkDao.kt
- Implement full-text search with @Fts4

### Task 11: Hadith Repository & Use Cases

- Create HadithRepository interface and implementation
- Create use cases: GetBooks, GetChapters, GetHadiths, SearchHadith, ToggleBookmark

### Task 12: Hadith ViewModel & Pre-populated Data

- Create HadithViewModel with UI state
- Create JSON files for Kutub al-Sittah (sample subset)
- Pre-populate database

## Phase 4: Dua Module (Tasks 13-15)

### Task 13: Dua DAOs

- Create DuaDao.kt for categories, duas, progress
- Create DuaBookmarkDao.kt

### Task 14: Dua Repository & Use Cases

- Create DuaRepository interface and implementation
- Create use cases: GetCategories, GetDuas, ToggleBookmark, LogProgress

### Task 15: Dua ViewModel & Pre-populated Data

- Create DuaViewModel with UI state
- Create JSON files for Morning/Evening Adhkar, situational duas
- Pre-populate database

## Phase 5: Prayer Module (Tasks 16-19)

### Task 16: Prayer Time Calculation

- Integrate Adhan Kotlin library
- Create PrayerTimeCalculator wrapper class
- Support all calculation methods (MWL, ISNA, Egypt, Makkah, Karachi, etc.)
- Support Hanafi/Shafi Asr calculation
- Create CalculatePrayerTimesUseCase

### Task 17: Prayer Tracking DAOs

Create PrayerDao.kt with queries:
- `getPrayersByDate(date: String): Flow<List<PrayerRecordEntity>>`
- `getPrayersByDateRange(start: String, end: String): Flow<List<PrayerRecordEntity>>`
- `getStreak(): Flow<Int>`
- `getCompletionRate(start: String, end: String): Flow<Float>`
- `upsertPrayer(prayer: PrayerRecordEntity)`

### Task 18: Prayer Repository & Use Cases

- Create PrayerRepository interface and implementation
- Create use cases: GetTodayPrayers, LogPrayer, GetStats, GetStreak

### Task 19: Prayer ViewModels

- Create PrayerViewModel for tracker screen
- Create HomeViewModel for main screen with prayer times
- Include countdown timer logic

## Phase 6: Fasting Module (Tasks 20-22)

### Task 20: Fasting DAOs

- Create FastDao.kt with queries for records and makeup fasts

### Task 21: Fasting Repository & Use Cases

- Create FastRepository interface and implementation
- Create use cases: GetRamadanProgress, LogFast, GetMakeupFasts, CompleteMakeup

### Task 22: Fasting ViewModel

Create FastViewModel with:
- Ramadan calendar state
- Voluntary fasts tracking
- Makeup fasts management

## Phase 7: Additional Features (Tasks 23-27)

### Task 23: Tasbih Module

- Create TasbihDao for presets and sessions
- Create TasbihRepository and use cases
- Create TasbihViewModel with counter state
- Pre-populate default dhikr presets

### Task 24: Zakat Calculator

- Create ZakatCalculator utility class
- Support gold, silver, cash, stocks, property, debts
- Create ZakatViewModel

### Task 25: Qibla Module

- Create QiblaCalculator using device sensors
- Create LocationRepository for GPS
- Create QiblaViewModel with compass state

### Task 26: Islamic Calendar

- Create HijriDateConverter utility
- Implement Umm al-Qura calendar algorithm
- Create IslamicEventDao and pre-populate events
- Create CalendarViewModel

### Task 27: Bookmarks & Search

- Create unified BookmarkRepository
- Create SearchRepository with full-text search across Quran, Hadith, Duas
- Create BookmarksViewModel and SearchViewModel

## Phase 8: System Integration (Tasks 28-32)

### Task 28: Notifications System

- Create NotificationManager for prayer time alerts
- Create AlarmScheduler using WorkManager
- Create NotificationSettingsRepository

### Task 29: Widget Implementation

- Create PrayerTimesWidget using Glance
- Support 2x2, 4x2, 4x4 sizes
- Create WidgetUpdateWorker

### Task 30: Location Services

- Create LocationRepository with FusedLocationProvider
- Handle permissions gracefully
- Create LocationViewModel

### Task 31: Settings Complete

- Create SettingsViewModel with all preferences
- Implement theme switching
- Implement language switching (future preparation)

### Task 32: Onboarding Flow

- Create OnboardingViewModel
- Handle first-launch detection
- Request necessary permissions
- Set initial location and calculation method

## Phase 9: Testing & Polish (Tasks 33-35)

### Task 33: Unit Tests

- Test all use cases
- Test ViewModels with fake repositories
- Test prayer time calculations
- Test Hijri date conversion

### Task 34: Integration Tests

- Test Room DAOs with in-memory database
- Test repository implementations

### Task 35: Create Minimal UI Screens

- Create minimal placeholder screens for each route
- Display data from ViewModels with basic Text/LazyColumn
- Verify all data flows work end-to-end
- These will be replaced with full UI later

---

# 8. Screen Data Requirements

Each screen's data requirements and ViewModel outputs:

## 8.1 Home Screen

```kotlin
data class HomeUiState(
    val currentLocation: Location? = null,
    val prayerTimes: PrayerTimes? = null,
    val nextPrayer: Prayer? = null,
    val countdownSeconds: Long = 0,
    val hijriDate: HijriDate? = null,
    val todayPrayers: List<PrayerRecord> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class HomeEvent {
    data class LogPrayer(val prayerType: PrayerType) : HomeEvent()
    object RefreshPrayerTimes : HomeEvent()
}
```

## 8.2 Quran Screens

```kotlin
data class QuranHomeUiState(
    val surahs: List<Surah> = emptyList(),
    val juzList: List<Juz> = emptyList(),
    val readingProgress: ReadingProgress? = null,
    val bookmarks: List<QuranBookmark> = emptyList(),
    val khatams: List<Khatam> = emptyList(),
    val isLoading: Boolean = true
)

data class QuranReaderUiState(
    val surah: Surah? = null,
    val ayahs: List<AyahWithTranslation> = emptyList(),
    val currentAyah: Int = 1,
    val isBookmarked: Boolean = false,
    val isPlaying: Boolean = false,
    val selectedReciter: Reciter? = null,
    val fontSize: Int = 24,
    val showTranslation: Boolean = true,
    val showTransliteration: Boolean = false
)
```

## 8.3 Prayer Tracker Screen

```kotlin
data class PrayerTrackerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val monthPrayers: Map<LocalDate, List<PrayerRecord>> = emptyMap(),
    val todayPrayers: List<PrayerRecord> = emptyList(),
    val currentStreak: Int = 0,
    val monthCompletion: Float = 0f,
    val perfectDays: Int = 0
)

sealed class PrayerTrackerEvent {
    data class SelectDate(val date: LocalDate) : PrayerTrackerEvent()
    data class TogglePrayer(val prayer: PrayerRecord) : PrayerTrackerEvent()
    data class ChangeMonth(val month: YearMonth) : PrayerTrackerEvent()
}
```

---

# 9. Testing Strategy

## 9.1 Unit Tests

- Test all Use Cases with mocked repositories
- Test ViewModels with fake data sources
- Test utility classes (HijriDateConverter, PrayerTimeCalculator)
- Test Room type converters

## 9.2 Integration Tests

- Test Room DAOs with in-memory database
- Test Repository implementations
- Test DataStore operations

## 9.3 UI Tests

- Test navigation flows
- Test screen state rendering
- Test user interactions

## 9.4 Test Dependencies

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
testImplementation("io.mockk:mockk:1.13.9")
testImplementation("app.cash.turbine:turbine:1.0.0") // Flow testing
testImplementation("com.google.truth:truth:1.4.0")

androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("androidx.room:room-testing:2.8.4")
```

---

# 10. Git Workflow & Changelog

## 10.1 Branch Strategy

- **main**: Production-ready code only
- **develop**: Integration branch for features
- **feature/***: Individual feature branches
- **fix/***: Bug fix branches

## 10.2 Commit Convention

```
feat: Add prayer time calculation module
fix: Correct Hijri date conversion for leap years
docs: Update README with setup instructions
style: Format code according to ktlint
refactor: Extract PrayerTimeCalculator to separate class
test: Add unit tests for QuranRepository
chore: Update Gradle dependencies
```

## 10.3 Changelog Format

```markdown
# Changelog

## [1.0.0] - 2026-XX-XX

### Added
- Prayer times calculation with multiple methods
- Complete Quran with Sahih International translation
- Hadith collection (Bukhari, Muslim, Abu Dawud, Tirmidhi, Nasai, Ibn Majah)
- Duas and Adhkar with audio
- Tasbih counter with presets
- Prayer tracker with statistics
- Fasting tracker for Ramadan and voluntary fasts
- Zakat calculator
- Qibla compass
- Islamic calendar with events
- Home screen widgets
- Notification system for prayer times
- Offline-first architecture

### Technical
- Clean Architecture with MVVM
- Jetpack Compose UI
- Room database with pre-populated data
- Hilt dependency injection
- Material 3 design system
```

## 10.4 Issue Template

```markdown
## Task: [Task Name]

### Description
Brief description of what needs to be implemented.

### Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

### Technical Notes
Any implementation details or considerations.

### Dependencies
List any tasks this depends on.

### Estimated Effort
[ ] Small (< 2 hours)
[ ] Medium (2-4 hours)
[ ] Large (4-8 hours)
[ ] XL (> 8 hours)
```

---

# Summary

This document provides the complete technical foundation for building Nimaz Pro. Following the 35 tasks in order will result in a **100% functional offline-first app** with all data layers complete. The UI can then be built on top of this foundation, replacing the minimal placeholder screens with the designs from the HTML prototypes.

## Key Deliverables

- Complete Room database with 20+ entities
- Pre-populated Islamic data (Quran, Hadith, Duas, Events)
- Clean Architecture with domain/data/presentation layers
- Hilt dependency injection throughout
- ViewModels exposing StateFlow for all screens
- Repository pattern for data access
- Use cases for business logic
- Notification and widget systems
- Comprehensive test suite

## Next Steps After Foundation

1. Build Atom components from design system
2. Build Molecule components composing atoms
3. Build Organism components
4. Replace placeholder screens with full UI
5. Add animations and polish
6. Performance optimization
7. Accessibility improvements
8. Localization (Arabic, Urdu, etc.)

---

<div align="center">

**بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ**

*In the name of Allah, the Most Gracious, the Most Merciful*

</div>
