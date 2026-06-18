# Help & About Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the wordy, hard-to-navigate Help screen with a search-first, topic→detail→guide UX whose content is data-driven (authored in `help.json`, seeded into Room at runtime) and localized to all 6 app languages; and rebuild the About screen to a cleaner layout.

**Architecture:** Help content lives in 4 read-only Room tables. A startup `HelpContentSeeder` parses a bundled `assets/help/help.json` (kotlinx.serialization) and fills the tables when empty or when a bundled content-version increments — so both fresh installs and existing users get content without rebuilding the 146 MB prepopulated DB asset. The 4 tables are created for everyone by `MIGRATION_13_14` (Room runs migrations even after `createFromAsset`). The UI is content-agnostic: `HelpRepository` resolves localized strings (selected language → English fallback) into domain models; `HelpViewModel` exposes state; three Compose screens render whatever exists. About is a static rebuild using existing design-system atoms.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room (KSP), Hilt, kotlinx.serialization, Coroutines/Flow, DataStore. Tests: JUnit4, MockK, Truth, Turbine, kotlinx-coroutines-test.

## Global Constraints

- **Package root:** `com.arshadshah.nimaz`. Follow existing clean-arch layout: entities `data/local/database/entity/`, DAOs `data/local/database/dao/`, repo impl `data/repository/`, repo interface `domain/repository/`, models `domain/model/`, use cases `domain/usecase/`, ViewModels `presentation/viewmodel/`, screens `presentation/screens/help/`, DI `core/di/`, nav `core/navigation/`.
- **DB:** `NimazDatabase`, name `nimaz_database`, current version **13** → bump to **14**. `exportSchema = true` (schemas in `app/schemas/`). DB opens via `createFromAsset("database/nimaz_prepopulated.db", PREPACKAGED_CALLBACK)` + `addMigrations(...)` in `core/di/DatabaseModule.kt`. **Do NOT modify** `nimaz-pro-data/` or the prepopulated asset.
- **Supported languages (codes):** `en, tr, id, ms, fr, de`. Selected language: `PreferencesDataStore.appLanguage: Flow<String>` (default `"en"`). English is the fallback for any missing string.
- **DI style:** repositories bound with `@Binds` in `abstract class RepositoryModule`; use cases provided with `@Provides` in `object UseCaseModule`; DAOs provided with `@Provides` in `object DatabaseModule`. All `@Singleton`, `@InstallIn(SingletonComponent::class)`.
- **Reads return `Flow`, mutations are `suspend`** (codebase convention).
- **Test conventions:** unit tests in `app/src/test/...`; JUnit4 (`org.junit.Test`), assertions via `com.google.common.truth.Truth.assertThat`, mocks via `io.mockk.mockk(relaxed = true)`, Flow assertions via `app.cash.turbine.test`, coroutines via `kotlinx.coroutines.test.runTest` + `StandardTestDispatcher` (+ `Dispatchers.setMain`). Repositories are tested with a **mocked DAO**; ViewModels with a **mocked repository/use-cases + PreferencesDataStore**.
- **Test command:** `./gradlew :app:testDebugUnitTest --tests "<FQN>"`. Build check: `./gradlew :app:assembleDebug`. Instrumented (migration) test: `./gradlew :app:connectedDebugAndroidTest --tests "<FQN>"` (needs a device/emulator).
- **Icons** in content are Material Symbols / `Icons.*` names resolved by a `helpIcon(key)` mapper; **colors** are theme keys resolved by a `helpColor(key)` mapper. Unknown keys fall back to a default — never crash on unknown content.
- **Commit** after each task with the message shown in its final step.

## File map

**Create:**
- `app/src/main/java/.../data/local/database/entity/HelpEntities.kt` — 4 `@Entity` classes
- `app/src/main/java/.../data/local/database/dao/HelpDao.kt` — DAO
- `app/src/main/java/.../domain/model/HelpModels.kt` — domain models
- `app/src/main/java/.../data/local/help/HelpJsonDto.kt` — `@Serializable` DTOs + `Json` config
- `app/src/main/assets/help/help.json` — authored content (English in phase 1)
- `app/src/main/java/.../data/local/help/HelpContentSeeder.kt` — runtime seeder
- `app/src/main/java/.../domain/repository/HelpRepository.kt` — interface
- `app/src/main/java/.../data/repository/HelpRepositoryImpl.kt` — impl
- `app/src/main/java/.../domain/usecase/HelpUseCases.kt` — use cases
- `app/src/main/java/.../presentation/viewmodel/HelpViewModel.kt`
- `app/src/main/java/.../presentation/screens/help/HelpScreen.kt` (replaces old `HelpSupportScreen.kt`)
- `app/src/main/java/.../presentation/screens/help/HelpTopicDetailScreen.kt`
- `app/src/main/java/.../presentation/screens/help/HelpGuideScreen.kt`
- `app/src/main/java/.../presentation/screens/help/HelpContentUi.kt` — `helpIcon`/`helpColor` mappers + shared item composables
- `app/src/main/java/.../core/navigation/HelpDeepLink.kt` — `helpDeepLinkRoute(key): Route?`
- Tests: `app/src/test/.../data/local/help/HelpJsonDtoTest.kt`, `HelpContentSeederTest.kt`, `data/repository/HelpRepositoryImplTest.kt`, `presentation/viewmodel/HelpViewModelTest.kt`, `core/navigation/HelpDeepLinkTest.kt`; instrumented `app/src/androidTest/.../MigrationTest.kt`

**Modify:**
- `app/src/main/java/.../data/local/database/NimazDatabase.kt` — add entities, `helpDao()`, version 14, `MIGRATION_13_14`
- `app/src/main/java/.../core/di/DatabaseModule.kt` — register migration + `provideHelpDao`
- `app/src/main/java/.../core/di/RepositoryModule.kt` — bind repo + provide use cases
- `app/src/main/java/.../core/navigation/Routes.kt` — `HelpTopicDetail`, `HelpGuide`
- `app/src/main/java/.../core/navigation/NavGraph.kt` — register 3 screens + deep-link wiring
- `app/src/main/java/.../presentation/screens/about/AboutScreen.kt` — rebuild
- `app/src/main/res/values/strings.xml` (+ `values-*/`) — Help/About labels
- Delete `app/src/main/java/.../presentation/screens/help/HelpSupportScreen.kt` (in Task 13)

---

## Phase 1 — Data foundation

### Task 1: Help entities + DB registration + migration

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/data/local/database/entity/HelpEntities.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/data/local/database/NimazDatabase.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/di/DatabaseModule.kt`
- Test: `app/src/androidTest/java/com/arshadshah/nimaz/MigrationTest.kt`

**Interfaces:**
- Produces: `HelpTopicEntity`, `HelpItemEntity`, `HelpStepEntity`, `HelpStringEntity`; `NimazDatabase.helpDao(): HelpDao` (DAO defined in Task 2 — add the abstract fun signature now, referencing a `HelpDao` you create as an empty interface in this task and flesh out next).

- [ ] **Step 1: Create the entities**

```kotlin
// HelpEntities.kt
package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "help_topic")
data class HelpTopicEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "color_key") val colorKey: String
)

@Entity(
    tableName = "help_item",
    indices = [Index(value = ["topic_id"])]
)
data class HelpItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "topic_id") val topicId: String,
    val type: String, // "QUESTION" | "GUIDE"
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "icon_key") val iconKey: String?,
    @ColumnInfo(name = "estimated_minutes") val estimatedMinutes: Int?
)

@Entity(
    tableName = "help_step",
    indices = [Index(value = ["item_id"])]
)
data class HelpStepEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "deeplink_route") val deeplinkRoute: String?,
    @ColumnInfo(name = "path_labels") val pathLabels: String? // JSON array string
)

@Entity(
    tableName = "help_string",
    primaryKeys = ["owner_type", "owner_id", "field_key", "lang_code"],
    indices = [
        Index(value = ["owner_type", "owner_id", "lang_code"]),
        Index(value = ["lang_code"])
    ]
)
data class HelpStringEntity(
    @ColumnInfo(name = "owner_type") val ownerType: String, // "TOPIC" | "ITEM" | "STEP"
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "field_key") val fieldKey: String,   // "title","subtitle","question","answer","body"
    @ColumnInfo(name = "lang_code") val langCode: String,
    val value: String
)
```

- [ ] **Step 2: Add an empty `HelpDao` placeholder** so the database compiles

```kotlin
// data/local/database/dao/HelpDao.kt  (fleshed out in Task 2)
package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao

@Dao
interface HelpDao
```

- [ ] **Step 3: Register entities, DAO getter, version bump, and migration in `NimazDatabase.kt`**

In the `@Database(entities = [...])` list add: `HelpTopicEntity::class, HelpItemEntity::class, HelpStepEntity::class, HelpStringEntity::class`. Change `version = 13` to `version = 14`. Add the abstract getter and the migration:

```kotlin
abstract fun helpDao(): com.arshadshah.nimaz.data.local.database.dao.HelpDao
```

In the companion object, change `const val SCHEMA_VERSION = 13` to `14`, and add (next to the other migrations):

```kotlin
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `help_topic` (
                `id` TEXT NOT NULL,
                `display_order` INTEGER NOT NULL,
                `icon_key` TEXT NOT NULL,
                `color_key` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `help_item` (
                `id` TEXT NOT NULL,
                `topic_id` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `display_order` INTEGER NOT NULL,
                `icon_key` TEXT,
                `estimated_minutes` INTEGER,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_help_item_topic_id` ON `help_item` (`topic_id`)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `help_step` (
                `id` TEXT NOT NULL,
                `item_id` TEXT NOT NULL,
                `display_order` INTEGER NOT NULL,
                `deeplink_route` TEXT,
                `path_labels` TEXT,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_help_step_item_id` ON `help_step` (`item_id`)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `help_string` (
                `owner_type` TEXT NOT NULL,
                `owner_id` TEXT NOT NULL,
                `field_key` TEXT NOT NULL,
                `lang_code` TEXT NOT NULL,
                `value` TEXT NOT NULL,
                PRIMARY KEY(`owner_type`, `owner_id`, `field_key`, `lang_code`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_help_string_owner_type_owner_id_lang_code` ON `help_string` (`owner_type`, `owner_id`, `lang_code`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_help_string_lang_code` ON `help_string` (`lang_code`)")
    }
}
```

- [ ] **Step 4: Register the migration + DAO provider in `DatabaseModule.kt`**

Add `NimazDatabase.MIGRATION_13_14` to the `.addMigrations(...)` call, and add:

```kotlin
@Provides
@Singleton
fun provideHelpDao(database: NimazDatabase): com.arshadshah.nimaz.data.local.database.dao.HelpDao =
    database.helpDao()
```

- [ ] **Step 5: Write the migration test** (instrumented; validates the generated schema matches the entities)

```kotlin
// app/src/androidTest/java/com/arshadshah/nimaz/MigrationTest.kt
package com.arshadshah.nimaz

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NimazDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate13To14_createsHelpTables() {
        helper.createDatabase(dbName, 13).close()
        val db = helper.runMigrationsAndValidate(
            dbName, 14, true, NimazDatabase.MIGRATION_13_14
        )
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN " +
                "('help_topic','help_item','help_step','help_string')"
        )
        assertThat(cursor.count).isEqualTo(4)
        cursor.close()
    }
}
```

- [ ] **Step 6: Verify schema export + build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL; `app/schemas/com.arshadshah.nimaz.data.local.database.NimazDatabase/14.json` is generated. Confirm `13.json` already exists (it must, for the migration test).

- [ ] **Step 7: Run the migration test** (requires emulator/device)

Run: `./gradlew :app:connectedDebugAndroidTest --tests "com.arshadshah.nimaz.MigrationTest"`
Expected: PASS. If no device is available, note it and rely on Step 6 + the runtime smoke test in Task 13.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/data/local/database/ \
        app/src/main/java/com/arshadshah/nimaz/core/di/DatabaseModule.kt \
        app/src/androidTest/java/com/arshadshah/nimaz/MigrationTest.kt \
        app/schemas
git commit -m "feat(help): add help content tables + migration 13->14"
```

---

### Task 2: HelpDao

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/data/local/database/dao/HelpDao.kt`

**Interfaces:**
- Consumes: entities from Task 1.
- Produces: `HelpDao` with the methods below (used by seeder + repository).

- [ ] **Step 1: Replace the placeholder with the full DAO**

```kotlin
package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.database.entity.HelpItemEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStepEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpTopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HelpDao {

    // ---- reads (reactive) ----
    @Query("SELECT * FROM help_topic ORDER BY display_order ASC")
    fun getTopics(): Flow<List<HelpTopicEntity>>

    @Query("SELECT * FROM help_item WHERE topic_id = :topicId ORDER BY display_order ASC")
    fun getItemsForTopic(topicId: String): Flow<List<HelpItemEntity>>

    @Query("SELECT * FROM help_item WHERE id = :itemId")
    fun getItem(itemId: String): Flow<HelpItemEntity?>

    @Query("SELECT * FROM help_step WHERE item_id = :itemId ORDER BY display_order ASC")
    fun getStepsForItem(itemId: String): Flow<List<HelpStepEntity>>

    @Query("SELECT * FROM help_string WHERE owner_type = :ownerType AND owner_id IN (:ownerIds)")
    fun getStringsFor(ownerType: String, ownerIds: List<String>): Flow<List<HelpStringEntity>>

    // Search: match localized text in the requested language; caller adds EN fallback rows.
    @Query(
        "SELECT * FROM help_string WHERE lang_code = :lang AND value LIKE '%' || :query || '%'"
    )
    fun searchStrings(lang: String, query: String): Flow<List<HelpStringEntity>>

    @Query("SELECT COUNT(*) FROM help_topic")
    suspend fun topicCount(): Int

    // ---- seeding (suspend, used by HelpContentSeeder) ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<HelpTopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<HelpItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<HelpStepEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrings(strings: List<HelpStringEntity>)

    @Query("DELETE FROM help_topic")
    suspend fun clearTopics()

    @Query("DELETE FROM help_item")
    suspend fun clearItems()

    @Query("DELETE FROM help_step")
    suspend fun clearSteps()

    @Query("DELETE FROM help_string")
    suspend fun clearStrings()
}
```

- [ ] **Step 2: Build to verify Room generates the DAO**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Room KSP compiles all queries).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/data/local/database/dao/HelpDao.kt
git commit -m "feat(help): add HelpDao with read + seeding queries"
```

---

### Task 3: Domain models + JSON DTOs

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/model/HelpModels.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/data/local/help/HelpJsonDto.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/data/local/help/HelpJsonDtoTest.kt`

**Interfaces:**
- Produces (domain): `HelpTopic`, `HelpItem` (sealed: `HelpQuestion`, `HelpGuide`), `HelpStep`, `HelpSearchResult`, `HelpTopicDetail`.
- Produces (dto): `HelpJsonRoot`, `HelpTopicDto`, `HelpItemDto`, `HelpStepDto`, and `helpJson: Json`.

- [ ] **Step 1: Create domain models**

```kotlin
// HelpModels.kt
package com.arshadshah.nimaz.domain.model

data class HelpTopic(
    val id: String,
    val iconKey: String,
    val colorKey: String,
    val title: String,
    val subtitle: String,
    val order: Int,
    val itemCount: Int
)

sealed interface HelpItem {
    val id: String
    val order: Int

    data class HelpQuestion(
        override val id: String,
        override val order: Int,
        val question: String,
        val answer: String
    ) : HelpItem

    data class HelpGuide(
        override val id: String,
        override val order: Int,
        val iconKey: String?,
        val title: String,
        val estimatedMinutes: Int?,
        val stepCount: Int
    ) : HelpItem
}

data class HelpStep(
    val id: String,
    val order: Int,
    val title: String,
    val body: String,
    val deeplinkRoute: String?,
    val pathLabels: List<String>
)

data class HelpTopicDetail(
    val topic: HelpTopic,
    val questions: List<HelpItem.HelpQuestion>,
    val guides: List<HelpItem.HelpGuide>
)

data class HelpGuideDetail(
    val id: String,
    val title: String,
    val estimatedMinutes: Int?,
    val steps: List<HelpStep>
)

data class HelpSearchResult(
    val topicId: String,
    val itemId: String?,   // null when the hit is the topic itself
    val isGuide: Boolean,
    val title: String,
    val snippet: String
)
```

- [ ] **Step 2: Create the JSON DTOs + `Json` config**

```kotlin
// HelpJsonDto.kt
package com.arshadshah.nimaz.data.local.help

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val helpJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
data class HelpJsonRoot(
    val contentVersion: Int = 1,
    val topics: List<HelpTopicDto> = emptyList()
)

@Serializable
data class HelpTopicDto(
    val id: String,
    val order: Int,
    val icon: String,
    val color: String,
    val title: Map<String, String> = emptyMap(),
    val subtitle: Map<String, String> = emptyMap(),
    val items: List<HelpItemDto> = emptyList()
)

@Serializable
data class HelpItemDto(
    val id: String,
    val type: String,                 // "question" | "guide"
    val order: Int,
    val icon: String? = null,
    val estimatedMinutes: Int? = null,
    val question: Map<String, String> = emptyMap(),
    val answer: Map<String, String> = emptyMap(),
    val title: Map<String, String> = emptyMap(),
    val steps: List<HelpStepDto> = emptyList()
)

@Serializable
data class HelpStepDto(
    val id: String,
    val order: Int,
    val deeplink: String? = null,
    val pathLabels: List<String> = emptyList(),
    val title: Map<String, String> = emptyMap(),
    val body: Map<String, String> = emptyMap()
)
```

- [ ] **Step 3: Write the failing DTO parse test**

```kotlin
// HelpJsonDtoTest.kt
package com.arshadshah.nimaz.data.local.help

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HelpJsonDtoTest {

    private val sample = """
        { "contentVersion": 3, "topics": [
          { "id":"prayer_times","order":1,"icon":"schedule","color":"indigo",
            "title":{"en":"Prayer Times","fr":"Horaires"},
            "subtitle":{"en":"Calculation"},
            "items":[
              {"id":"pt_q1","type":"question","order":1,
               "question":{"en":"Why different?"},"answer":{"en":"Because location."}},
              {"id":"pt_g1","type":"guide","order":2,"icon":"tune","estimatedMinutes":1,
               "title":{"en":"Change method"},
               "steps":[{"id":"pt_g1_s1","order":1,"deeplink":"prayer_settings",
                         "pathLabels":["More","Prayer Settings"],
                         "title":{"en":"Open settings"},"body":{"en":"Go to More."}}]}
            ] }
        ] }
    """.trimIndent()

    @Test
    fun parsesContentVersionTopicsItemsAndSteps() {
        val root = helpJson.decodeFromString(HelpJsonRoot.serializer(), sample)
        assertThat(root.contentVersion).isEqualTo(3)
        assertThat(root.topics).hasSize(1)
        val topic = root.topics.first()
        assertThat(topic.title["fr"]).isEqualTo("Horaires")
        assertThat(topic.items).hasSize(2)
        val guide = topic.items.first { it.type == "guide" }
        assertThat(guide.steps.first().deeplink).isEqualTo("prayer_settings")
        assertThat(guide.steps.first().pathLabels).containsExactly("More", "Prayer Settings")
    }

    @Test
    fun ignoresUnknownKeys() {
        val withExtra = """{ "contentVersion":1, "future":"x", "topics":[] }"""
        val root = helpJson.decodeFromString(HelpJsonRoot.serializer(), withExtra)
        assertThat(root.topics).isEmpty()
    }
}
```

- [ ] **Step 4: Run the test to verify it passes** (DTOs already written)

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.data.local.help.HelpJsonDtoTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/model/HelpModels.kt \
        app/src/main/java/com/arshadshah/nimaz/data/local/help/HelpJsonDto.kt \
        app/src/test/java/com/arshadshah/nimaz/data/local/help/HelpJsonDtoTest.kt
git commit -m "feat(help): add domain models and help.json DTOs"
```

---

### Task 4: `help.json` asset (English content)

**Files:**
- Create: `app/src/main/assets/help/help.json`

**Interfaces:**
- Consumes: the DTO shape from Task 3.
- Produces: the bundled content the seeder reads. `contentVersion` starts at `1`.

This is a **content task**. It condenses the existing 23 FAQ / 10 guides / 8 troubleshooting items (extracted verbatim from `HelpSupportScreen.kt`) into 6 topics with short (1–2 sentence) answers, converting step-based instructions into guides. Every condensed string traces back to the original copy.

- [ ] **Step 1: Author the file.** Use exactly this shape (excerpt shows one full topic + the skeleton for the rest; fill all 6 topics the same way). Map source → topics: `prayer_times`, `notifications_adhan`, `location_qibla`, `quran_audio`, `tracking_tools`, `troubleshooting`.

```json
{
  "contentVersion": 1,
  "topics": [
    {
      "id": "prayer_times", "order": 1, "icon": "schedule", "color": "indigo",
      "title": { "en": "Prayer Times" },
      "subtitle": { "en": "Calculation, Asr, adjustments" },
      "items": [
        { "id": "pt_q_calc", "type": "question", "order": 1,
          "question": { "en": "How are prayer times calculated?" },
          "answer": { "en": "From your saved location plus the calculation method, Asr method and high-latitude rule you pick. Times refresh automatically each day." } },
        { "id": "pt_q_mosque", "type": "question", "order": 2,
          "question": { "en": "Why don't they match my mosque?" },
          "answer": { "en": "Mosques use a specific method or fixed minute offsets. Match the method, then fine-tune each prayer under Manual Adjustments." } },
        { "id": "pt_q_asr", "type": "question", "order": 3,
          "question": { "en": "Why are there two Asr times?" },
          "answer": { "en": "Standard (Shafi'i/Maliki/Hanbali) uses shadow = object length; Hanafi uses twice that, making Asr later. Pick the school you follow." } },
        { "id": "pt_g_method", "type": "guide", "order": 4, "icon": "tune", "estimatedMinutes": 1,
          "title": { "en": "Change calculation method" },
          "steps": [
            { "id": "pt_g_method_s1", "order": 1, "deeplink": "prayer_settings",
              "pathLabels": ["More", "Prayer Settings"],
              "title": { "en": "Open Prayer Settings" },
              "body": { "en": "From the bottom bar, go to More → Prayer Settings." } },
            { "id": "pt_g_method_s2", "order": 2,
              "title": { "en": "Tap Calculation Method" },
              "body": { "en": "The method currently in use is highlighted." } },
            { "id": "pt_g_method_s3", "order": 3,
              "title": { "en": "Pick your mosque's authority" },
              "body": { "en": "Choose e.g. Muslim World League or Umm al-Qura — times update instantly." } }
          ] },
        { "id": "pt_g_adjust", "type": "guide", "order": 5, "icon": "more_time", "estimatedMinutes": 1,
          "title": { "en": "Fine-tune a prayer time" },
          "steps": [
            { "id": "pt_g_adjust_s1", "order": 1, "deeplink": "prayer_settings",
              "pathLabels": ["More", "Prayer Settings"],
              "title": { "en": "Open Prayer Settings" },
              "body": { "en": "Go to More → Prayer Settings and scroll to Manual Adjustments (Minutes)." } },
            { "id": "pt_g_adjust_s2", "order": 2,
              "title": { "en": "Shift the prayer" },
              "body": { "en": "Move Fajr, Dhuhr, Asr, Maghrib or Isha forward or back to match your local timetable." } }
          ] }
      ]
    }
  ]
}
```

Remaining topics to author the same way (concise answers, guides where steps exist):
- **notifications_adhan** (`icon: notifications_active`, `color: gold`): not receiving notifications (guide → `notifications`), choosing the adhan sound (guide → `notifications`), pre-adhan reminder, adhan during DND.
- **location_qibla** (`icon: explore`, `color: teal`): set location (guide → `location`), background-location/privacy (question), Qibla compass + calibration (question).
- **quran_audio** (`icon: menu_book`, `color: green`): display options (guide → `quran_settings`), recitation/reciter (guide → `quran_settings`).
- **tracking_tools** (`icon: task_alt`, `color: violet`): track prayers, Qada, Tasbih, fasting, Zakat (questions; deep-link guides where a confirmed route exists — see Task 8).
- **troubleshooting** (`icon: build`, `color: orange`): the 8 troubleshooting items as questions with condensed symptom→fix answers (times off by an hour, no notifications, adhan silent, location won't update, Qibla wrong, background-kill, Hijri off by a day, audio won't download).

- [ ] **Step 2: Validate the JSON parses** by extending the Task 3 test to load the real asset is not possible in a unit test (assets aren't on the unit classpath); instead validate locally:

Run: `python3 -c "import json,sys; json.load(open('app/src/main/assets/help/help.json')); print('valid')"`
Expected: `valid`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/help/help.json
git commit -m "content(help): author English help.json (6 topics)"
```

---

### Task 5: HelpContentSeeder

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/data/local/help/HelpContentSeeder.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/data/local/datastore/PreferencesDataStore.kt` (add a content-version key)
- Test: `app/src/test/java/com/arshadshah/nimaz/data/local/help/HelpContentSeederTest.kt`

**Interfaces:**
- Consumes: `HelpDao` (Task 2), DTOs + `helpJson` (Task 3), an asset-reading lambda, and a stored content-version (get/set suspend functions).
- Produces: `HelpContentSeeder.seedIfNeeded()` — idempotent; parses JSON, clears + inserts all tables, records the new content version. Flattens every `{lang: value}` map into `HelpStringEntity` rows; serializes `pathLabels` to a JSON string.

- [ ] **Step 1: Add a content-version preference** to `PreferencesDataStore.kt` (mirror the existing `app_language` pattern):

```kotlin
// inside PreferencesKeys
val HELP_CONTENT_VERSION = androidx.datastore.preferences.core.intPreferencesKey("help_content_version")

// public API
val helpContentVersion: Flow<Int> = dataStore.data.map { it[PreferencesKeys.HELP_CONTENT_VERSION] ?: 0 }
suspend fun setHelpContentVersion(version: Int) {
    dataStore.edit { it[PreferencesKeys.HELP_CONTENT_VERSION] = version }
}
```

- [ ] **Step 2: Create the seeder** (asset access injected as a lambda so it is unit-testable without Android)

```kotlin
// HelpContentSeeder.kt
package com.arshadshah.nimaz.data.local.help

import android.content.Context
import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.entity.HelpItemEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStepEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpTopicEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HelpContentSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: HelpDao,
    private val versionStore: HelpContentVersionStore,
    private val readAsset: (String) -> String = { path ->
        context.assets.open(path).bufferedReader().use { it.readText() }
    }
) {
    private val mutex = Mutex()

    suspend fun seedIfNeeded() = mutex.withLock {
        val root = helpJson.decodeFromString(
            HelpJsonRoot.serializer(), readAsset("help/help.json")
        )
        val stored = versionStore.get()
        val populated = dao.topicCount() > 0
        if (populated && stored >= root.contentVersion) return@withLock
        seed(root)
        versionStore.set(root.contentVersion)
    }

    private suspend fun seed(root: HelpJsonRoot) {
        val topics = mutableListOf<HelpTopicEntity>()
        val items = mutableListOf<HelpItemEntity>()
        val steps = mutableListOf<HelpStepEntity>()
        val strings = mutableListOf<HelpStringEntity>()

        fun addStrings(ownerType: String, ownerId: String, field: String, map: Map<String, String>) {
            map.forEach { (lang, value) ->
                strings += HelpStringEntity(ownerType, ownerId, field, lang, value)
            }
        }

        root.topics.forEach { t ->
            topics += HelpTopicEntity(t.id, t.order, t.icon, t.color)
            addStrings("TOPIC", t.id, "title", t.title)
            addStrings("TOPIC", t.id, "subtitle", t.subtitle)
            t.items.forEach { item ->
                val type = if (item.type.equals("guide", true)) "GUIDE" else "QUESTION"
                items += HelpItemEntity(item.id, t.id, type, item.order, item.icon, item.estimatedMinutes)
                addStrings("ITEM", item.id, "question", item.question)
                addStrings("ITEM", item.id, "answer", item.answer)
                addStrings("ITEM", item.id, "title", item.title)
                item.steps.forEach { s ->
                    val pathLabels = if (s.pathLabels.isEmpty()) null
                        else helpJson.encodeToString(s.pathLabels)
                    steps += HelpStepEntity(s.id, item.id, s.order, s.deeplink, pathLabels)
                    addStrings("STEP", s.id, "title", s.title)
                    addStrings("STEP", s.id, "body", s.body)
                }
            }
        }

        dao.clearStrings(); dao.clearSteps(); dao.clearItems(); dao.clearTopics()
        dao.insertTopics(topics); dao.insertItems(items)
        dao.insertSteps(steps); dao.insertStrings(strings)
    }
}

/** Thin abstraction over the DataStore version key so the seeder is unit-testable. */
interface HelpContentVersionStore {
    suspend fun get(): Int
    suspend fun set(version: Int)
}
```

Add the `kotlinx.serialization.encodeToString` import (`import kotlinx.serialization.encodeToString`).

- [ ] **Step 3: Provide a DataStore-backed `HelpContentVersionStore`** (in the same file or `HelpContentSeeder.kt`):

```kotlin
@Singleton
class DataStoreHelpContentVersionStore @Inject constructor(
    private val prefs: com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
) : HelpContentVersionStore {
    override suspend fun get(): Int =
        kotlinx.coroutines.flow.first(prefs.helpContentVersion)
    override suspend fun set(version: Int) = prefs.setHelpContentVersion(version)
}
```

Use `kotlinx.coroutines.flow.first` (import `kotlinx.coroutines.flow.first`).

- [ ] **Step 4: Bind the version store** in `RepositoryModule` (abstract module):

```kotlin
@Binds
@Singleton
abstract fun bindHelpContentVersionStore(
    impl: com.arshadshah.nimaz.data.local.help.DataStoreHelpContentVersionStore
): com.arshadshah.nimaz.data.local.help.HelpContentVersionStore
```

- [ ] **Step 5: Write the failing seeder test** (mocked DAO + fake version store + injected asset string)

```kotlin
// HelpContentSeederTest.kt
package com.arshadshah.nimaz.data.local.help

import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpTopicEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HelpContentSeederTest {

    private val json = """
        { "contentVersion": 2, "topics": [
          { "id":"t1","order":1,"icon":"schedule","color":"indigo",
            "title":{"en":"Prayer Times","fr":"Horaires"},"subtitle":{"en":"Sub"},
            "items":[
              {"id":"i1","type":"question","order":1,"question":{"en":"Q?"},"answer":{"en":"A."}},
              {"id":"i2","type":"guide","order":2,"icon":"tune","estimatedMinutes":1,
               "title":{"en":"Guide"},
               "steps":[{"id":"s1","order":1,"deeplink":"prayer_settings",
                         "pathLabels":["More","Prayer Settings"],
                         "title":{"en":"Step"},"body":{"en":"Body"}}]}
            ] }
        ] }
    """.trimIndent()

    private fun seeder(dao: HelpDao, storedVersion: Int): HelpContentSeeder {
        val store = object : HelpContentVersionStore {
            var v = storedVersion
            override suspend fun get() = v
            override suspend fun set(version: Int) { v = version }
        }
        return HelpContentSeeder(
            context = mockk(relaxed = true),
            dao = dao,
            versionStore = store,
            readAsset = { json }
        )
    }

    @Test
    fun seedsWhenEmpty_flattensLocaleMapsAndPathLabels() = runTest {
        val dao = mockk<HelpDao>(relaxed = true)
        coEvery { dao.topicCount() } returns 0
        val topics = slot<List<HelpTopicEntity>>()
        val strings = slot<List<HelpStringEntity>>()
        coEvery { dao.insertTopics(capture(topics)) } returns Unit
        coEvery { dao.insertStrings(capture(strings)) } returns Unit

        seeder(dao, storedVersion = 0).seedIfNeeded()

        assertThat(topics.captured.map { it.id }).containsExactly("t1")
        // both locales for the topic title are flattened
        assertThat(strings.captured.filter { it.ownerId == "t1" && it.fieldKey == "title" }
            .map { it.langCode }).containsExactly("en", "fr")
        coVerify { dao.clearStrings(); dao.clearTopics() }
    }

    @Test
    fun skipsWhenPopulatedAndVersionCurrent() = runTest {
        val dao = mockk<HelpDao>(relaxed = true)
        coEvery { dao.topicCount() } returns 6
        seeder(dao, storedVersion = 2).seedIfNeeded()
        coVerify(exactly = 0) { dao.insertTopics(any()) }
    }

    @Test
    fun reseedsWhenContentVersionIncremented() = runTest {
        val dao = mockk<HelpDao>(relaxed = true)
        coEvery { dao.topicCount() } returns 6
        seeder(dao, storedVersion = 1).seedIfNeeded() // bundled is 2 > stored 1
        coVerify(exactly = 1) { dao.insertTopics(any()) }
    }
}
```

- [ ] **Step 6: Run the tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.data.local.help.HelpContentSeederTest"`
Expected: PASS (3 tests).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/data/local/help/ \
        app/src/main/java/com/arshadshah/nimaz/data/local/datastore/PreferencesDataStore.kt \
        app/src/main/java/com/arshadshah/nimaz/core/di/RepositoryModule.kt \
        app/src/test/java/com/arshadshah/nimaz/data/local/help/HelpContentSeederTest.kt
git commit -m "feat(help): runtime help.json seeder with content-version guard"
```

---

### Task 6: HelpRepository (localization resolution)

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/repository/HelpRepository.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/data/repository/HelpRepositoryImpl.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/data/repository/HelpRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `HelpDao`, `HelpContentSeeder`, entities, domain models.
- Produces:
  - `interface HelpRepository`:
    - `fun getTopics(lang: String): Flow<List<HelpTopic>>`
    - `fun getTopicDetail(topicId: String, lang: String): Flow<HelpTopicDetail?>`
    - `fun getGuide(guideId: String, lang: String): Flow<HelpGuideDetail?>`
    - `fun search(query: String, lang: String): Flow<List<HelpSearchResult>>`
  - String resolution rule: prefer `lang`, else `"en"`, else empty string.

- [ ] **Step 1: Create the interface**

```kotlin
// HelpRepository.kt
package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail
import kotlinx.coroutines.flow.Flow

interface HelpRepository {
    fun getTopics(lang: String): Flow<List<HelpTopic>>
    fun getTopicDetail(topicId: String, lang: String): Flow<HelpTopicDetail?>
    fun getGuide(guideId: String, lang: String): Flow<HelpGuideDetail?>
    fun search(query: String, lang: String): Flow<List<HelpSearchResult>>
}
```

- [ ] **Step 2: Create the implementation**

```kotlin
// HelpRepositoryImpl.kt
package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.help.HelpContentSeeder
import com.arshadshah.nimaz.data.local.help.helpJson
import com.arshadshah.nimaz.domain.model.*
import com.arshadshah.nimaz.domain.repository.HelpRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Inject
import javax.inject.Singleton

private const val EN = "en"

@Singleton
class HelpRepositoryImpl @Inject constructor(
    private val dao: HelpDao,
    private val seeder: HelpContentSeeder
) : HelpRepository {

    /** Resolves a field for owner in `lang`, falling back to English, then "". */
    private fun List<HelpStringEntity>.resolve(
        ownerId: String, field: String, lang: String
    ): String {
        val forOwner = filter { it.ownerId == ownerId && it.fieldKey == field }
        return forOwner.firstOrNull { it.langCode == lang }?.value
            ?: forOwner.firstOrNull { it.langCode == EN }?.value
            ?: ""
    }

    private fun parsePathLabels(raw: String?): List<String> =
        if (raw.isNullOrBlank()) emptyList()
        else runCatching {
            helpJson.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())

    // Seed once, then emit DB-backed flows.
    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> =
        flow { seeder.seedIfNeeded(); emitAll(block()) }

    override fun getTopics(lang: String): Flow<List<HelpTopic>> = seededFlow {
        dao.getTopics().flatMapLatest { topics ->
            if (topics.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val ids = topics.map { it.id }
            combine(
                dao.getStringsFor("TOPIC", ids),
                dao.getItemsForTopic("__count__").let { _ -> dao.getTopics() } // placeholder to keep arity; replaced below
            ) { _, _ -> emptyList<HelpTopic>() }
        }
    }
    // NOTE: getTopics is finalized in Step 3 (item counts need a dedicated query) — see below.

    override fun getTopicDetail(topicId: String, lang: String): Flow<HelpTopicDetail?> = seededFlow {
        combine(
            dao.getTopics(),
            dao.getItemsForTopic(topicId),
            dao.getStringsFor("TOPIC", listOf(topicId))
        ) { topics, items, topicStrings -> Triple(topics, items, topicStrings) }
            .flatMapLatest { (topics, items, topicStrings) ->
                val topicEntity = topics.firstOrNull { it.id == topicId }
                    ?: return@flatMapLatest flowOf(null)
                val itemIds = items.map { it.id }
                dao.getStringsFor("ITEM", itemIds).map { itemStrings ->
                    val topic = HelpTopic(
                        id = topicEntity.id, iconKey = topicEntity.iconKey,
                        colorKey = topicEntity.colorKey,
                        title = topicStrings.resolve(topicId, "title", lang),
                        subtitle = topicStrings.resolve(topicId, "subtitle", lang),
                        order = topicEntity.displayOrder, itemCount = items.size
                    )
                    val questions = items.filter { it.type == "QUESTION" }.map {
                        HelpItem.HelpQuestion(
                            id = it.id, order = it.displayOrder,
                            question = itemStrings.resolve(it.id, "question", lang),
                            answer = itemStrings.resolve(it.id, "answer", lang)
                        )
                    }
                    val guides = items.filter { it.type == "GUIDE" }.map {
                        HelpItem.HelpGuide(
                            id = it.id, order = it.displayOrder, iconKey = it.iconKey,
                            title = itemStrings.resolve(it.id, "title", lang),
                            estimatedMinutes = it.estimatedMinutes, stepCount = 0
                        )
                    }
                    HelpTopicDetail(topic, questions, guides)
                }
            }
    }

    override fun getGuide(guideId: String, lang: String): Flow<HelpGuideDetail?> = seededFlow {
        combine(
            dao.getItem(guideId),
            dao.getStepsForItem(guideId),
            dao.getStringsFor("ITEM", listOf(guideId))
        ) { item, steps, itemStrings -> Triple(item, steps, itemStrings) }
            .flatMapLatest { (item, steps, itemStrings) ->
                if (item == null) return@flatMapLatest flowOf(null)
                val stepIds = steps.map { it.id }
                dao.getStringsFor("STEP", stepIds).map { stepStrings ->
                    HelpGuideDetail(
                        id = item.id,
                        title = itemStrings.resolve(item.id, "title", lang),
                        estimatedMinutes = item.estimatedMinutes,
                        steps = steps.map { s ->
                            HelpStep(
                                id = s.id, order = s.displayOrder,
                                title = stepStrings.resolve(s.id, "title", lang),
                                body = stepStrings.resolve(s.id, "body", lang),
                                deeplinkRoute = s.deeplinkRoute,
                                pathLabels = parsePathLabels(s.pathLabels)
                            )
                        }
                    )
                }
            }
    }

    override fun search(query: String, lang: String): Flow<List<HelpSearchResult>> = seededFlow {
        if (query.isBlank()) return@seededFlow flowOf(emptyList())
        combine(
            dao.searchStrings(lang, query),
            dao.searchStrings(EN, query),
            dao.getTopics(),
            dao.getItemsForTopic("") // returns nothing; real item lookup via getAllItems below
        ) { _, _, _, _ -> emptyList<HelpSearchResult>() }
    }
    // NOTE: search is finalized in Step 3 (needs an "all items" query) — see below.
}
```

- [ ] **Step 3: Finalize `getTopics`, `search`, and add the missing DAO queries.** The skeleton above has two methods marked for finalization because they need queries the DAO doesn't have yet. Add to `HelpDao`:

```kotlin
@Query("SELECT * FROM help_item ORDER BY display_order ASC")
fun getAllItems(): Flow<List<HelpItemEntity>>

@Query("SELECT COUNT(*) FROM help_item WHERE topic_id = :topicId")
fun itemCountForTopic(topicId: String): Flow<Int>
```

Replace `getTopics` with:

```kotlin
override fun getTopics(lang: String): Flow<List<HelpTopic>> = seededFlow {
    combine(
        dao.getTopics(),
        dao.getAllItems()
    ) { topics, allItems -> topics to allItems }
        .flatMapLatest { (topics, allItems) ->
            if (topics.isEmpty()) return@flatMapLatest flowOf(emptyList())
            dao.getStringsFor("TOPIC", topics.map { it.id }).map { strings ->
                topics.map { t ->
                    HelpTopic(
                        id = t.id, iconKey = t.iconKey, colorKey = t.colorKey,
                        title = strings.resolve(t.id, "title", lang),
                        subtitle = strings.resolve(t.id, "subtitle", lang),
                        order = t.displayOrder,
                        itemCount = allItems.count { it.topicId == t.id }
                    )
                }
            }
        }
}
```

Replace `search` with (resolve each matched owner to a result, dedupe by ownerId, EN rows fill gaps):

```kotlin
override fun search(query: String, lang: String): Flow<List<HelpSearchResult>> = seededFlow {
    if (query.isBlank()) return@seededFlow flowOf(emptyList())
    combine(
        dao.searchStrings(lang, query),
        dao.searchStrings(EN, query),
        dao.getAllItems()
    ) { localized, english, items ->
        val itemById = items.associateBy { it.id }
        val topicByItem = items.associate { it.id to it.topicId }
        (localized + english)
            .filter { it.ownerType == "ITEM" && it.fieldKey in setOf("question", "title") }
            .distinctBy { it.ownerId }
            .mapNotNull { s ->
                val item = itemById[s.ownerId] ?: return@mapNotNull null
                HelpSearchResult(
                    topicId = topicByItem[s.ownerId] ?: return@mapNotNull null,
                    itemId = s.ownerId,
                    isGuide = item.type == "GUIDE",
                    title = s.value,
                    snippet = ""
                )
            }
    }
}
```

Also fix `getTopicDetail`'s guide `stepCount`: after building `guides`, the count is acceptable as `0` for the list screen (the detail screen shows `N steps` from the guide screen). To show real counts, add `dao.getStepsForItem` per guide is overkill — instead the detail screen reads `estimatedMinutes`; leave `stepCount = 0` and render `"N steps"` only on the guide screen. (Documented intentional simplification.)

- [ ] **Step 4: Write the failing repository test** (mocked DAO)

```kotlin
// HelpRepositoryImplTest.kt
package com.arshadshah.nimaz.data.repository

import app.cash.turbine.test
import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.entity.HelpItemEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStepEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpTopicEntity
import com.arshadshah.nimaz.data.local.help.HelpContentSeeder
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HelpRepositoryImplTest {

    private lateinit var dao: HelpDao
    private lateinit var seeder: HelpContentSeeder
    private lateinit var repo: HelpRepositoryImpl

    @Before fun setUp() {
        dao = mockk(relaxed = true)
        seeder = mockk(relaxed = true)
        coEvery { seeder.seedIfNeeded() } returns Unit
        repo = HelpRepositoryImpl(dao, seeder)
    }

    @Test fun topicDetail_fallsBackToEnglishWhenLangMissing() = runTest {
        every { dao.getTopics() } returns flowOf(
            listOf(HelpTopicEntity("t1", 1, "schedule", "indigo"))
        )
        every { dao.getItemsForTopic("t1") } returns flowOf(
            listOf(HelpItemEntity("i1", "t1", "QUESTION", 1, null, null))
        )
        every { dao.getStringsFor("TOPIC", listOf("t1")) } returns flowOf(
            listOf(HelpStringEntity("TOPIC", "t1", "title", "en", "Prayer Times"))
        )
        every { dao.getStringsFor("ITEM", listOf("i1")) } returns flowOf(
            listOf(
                HelpStringEntity("ITEM", "i1", "question", "en", "Why?"),
                HelpStringEntity("ITEM", "i1", "answer", "en", "Because.")
            )
        )

        repo.getTopicDetail("t1", lang = "fr").test {
            val detail = awaitItem()!!
            assertThat(detail.topic.title).isEqualTo("Prayer Times") // EN fallback
            assertThat(detail.questions.single().answer).isEqualTo("Because.")
            awaitComplete()
        }
    }

    @Test fun topicDetail_prefersRequestedLanguage() = runTest {
        every { dao.getTopics() } returns flowOf(listOf(HelpTopicEntity("t1", 1, "schedule", "indigo")))
        every { dao.getItemsForTopic("t1") } returns flowOf(emptyList())
        every { dao.getStringsFor("TOPIC", listOf("t1")) } returns flowOf(
            listOf(
                HelpStringEntity("TOPIC", "t1", "title", "en", "Prayer Times"),
                HelpStringEntity("TOPIC", "t1", "title", "fr", "Horaires")
            )
        )
        every { dao.getStringsFor("ITEM", emptyList()) } returns flowOf(emptyList())

        repo.getTopicDetail("t1", lang = "fr").test {
            assertThat(awaitItem()!!.topic.title).isEqualTo("Horaires")
            awaitComplete()
        }
    }

    @Test fun guide_parsesPathLabelsJson() = runTest {
        every { dao.getItem("g1") } returns flowOf(HelpItemEntity("g1", "t1", "GUIDE", 1, "tune", 1))
        every { dao.getStepsForItem("g1") } returns flowOf(
            listOf(HelpStepEntity("s1", "g1", 1, "prayer_settings", """["More","Prayer Settings"]"""))
        )
        every { dao.getStringsFor("ITEM", listOf("g1")) } returns flowOf(
            listOf(HelpStringEntity("ITEM", "g1", "title", "en", "Change method"))
        )
        every { dao.getStringsFor("STEP", listOf("s1")) } returns flowOf(
            listOf(
                HelpStringEntity("STEP", "s1", "title", "en", "Open"),
                HelpStringEntity("STEP", "s1", "body", "en", "Go to More.")
            )
        )

        repo.getGuide("g1", lang = "en").test {
            val g = awaitItem()!!
            assertThat(g.steps.single().pathLabels).containsExactly("More", "Prayer Settings")
            assertThat(g.steps.single().deeplinkRoute).isEqualTo("prayer_settings")
            awaitComplete()
        }
    }
}
```

- [ ] **Step 5: Run the tests; fix until green**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.data.repository.HelpRepositoryImplTest"`
Expected: PASS (3 tests). (`getItemsForTopic` for an empty list won't be called when items are empty; ensure `getStringsFor("ITEM", emptyList())` is stubbed as above.)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/repository/HelpRepository.kt \
        app/src/main/java/com/arshadshah/nimaz/data/repository/HelpRepositoryImpl.kt \
        app/src/main/java/com/arshadshah/nimaz/data/local/database/dao/HelpDao.kt \
        app/src/test/java/com/arshadshah/nimaz/data/repository/HelpRepositoryImplTest.kt
git commit -m "feat(help): repository with language resolution + EN fallback"
```

---

### Task 7: HelpUseCases + DI wiring

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/usecase/HelpUseCases.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: `HelpRepository`.
- Produces: `HelpUseCases(getTopics, getTopicDetail, getGuide, search)` where each is an invokable class.

- [ ] **Step 1: Create the use cases**

```kotlin
// HelpUseCases.kt
package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail
import com.arshadshah.nimaz.domain.repository.HelpRepository
import kotlinx.coroutines.flow.Flow

data class HelpUseCases(
    val getTopics: GetHelpTopicsUseCase,
    val getTopicDetail: GetHelpTopicDetailUseCase,
    val getGuide: GetHelpGuideUseCase,
    val search: SearchHelpUseCase
)

class GetHelpTopicsUseCase(private val repo: HelpRepository) {
    operator fun invoke(lang: String): Flow<List<HelpTopic>> = repo.getTopics(lang)
}
class GetHelpTopicDetailUseCase(private val repo: HelpRepository) {
    operator fun invoke(topicId: String, lang: String): Flow<HelpTopicDetail?> =
        repo.getTopicDetail(topicId, lang)
}
class GetHelpGuideUseCase(private val repo: HelpRepository) {
    operator fun invoke(guideId: String, lang: String): Flow<HelpGuideDetail?> =
        repo.getGuide(guideId, lang)
}
class SearchHelpUseCase(private val repo: HelpRepository) {
    operator fun invoke(query: String, lang: String): Flow<List<HelpSearchResult>> =
        repo.search(query, lang)
}
```

- [ ] **Step 2: Bind repo + provide use cases** in `RepositoryModule.kt`. In `abstract class RepositoryModule` add:

```kotlin
@Binds
@Singleton
abstract fun bindHelpRepository(impl: HelpRepositoryImpl): HelpRepository
```

In `object UseCaseModule` add:

```kotlin
@Provides
@Singleton
fun provideHelpUseCases(repository: HelpRepository): HelpUseCases = HelpUseCases(
    getTopics = GetHelpTopicsUseCase(repository),
    getTopicDetail = GetHelpTopicDetailUseCase(repository),
    getGuide = GetHelpGuideUseCase(repository),
    search = SearchHelpUseCase(repository)
)
```

(Add the necessary imports for `HelpRepository`, `HelpRepositoryImpl`, and the use-case classes.)

- [ ] **Step 3: Build to verify Hilt graph compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Hilt resolves `HelpUseCases`, `HelpRepository`, `HelpDao`, `HelpContentSeeder`, `HelpContentVersionStore`).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/usecase/HelpUseCases.kt \
        app/src/main/java/com/arshadshah/nimaz/core/di/RepositoryModule.kt
git commit -m "feat(help): use cases + DI wiring"
```

---

## Phase 2 — Presentation

### Task 8: Routes + deep-link resolver

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/Routes.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/core/navigation/HelpDeepLink.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/core/navigation/HelpDeepLinkTest.kt`

**Interfaces:**
- Produces: `Route.HelpTopicDetail(topicId: String)`, `Route.HelpGuide(guideId: String)`, and `fun helpDeepLinkRoute(key: String?): Route?`.

- [ ] **Step 1: Add routes** to `Routes.kt` (next to `SettingsHelp`):

```kotlin
@Serializable
data class HelpTopicDetail(val topicId: String) : Route

@Serializable
data class HelpGuide(val guideId: String) : Route
```

- [ ] **Step 2: Write the failing resolver test**

```kotlin
// HelpDeepLinkTest.kt
package com.arshadshah.nimaz.core.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HelpDeepLinkTest {
    @Test fun mapsKnownKeys() {
        assertThat(helpDeepLinkRoute("prayer_settings")).isEqualTo(Route.SettingsPrayerCalculation)
        assertThat(helpDeepLinkRoute("notifications")).isEqualTo(Route.SettingsNotifications)
        assertThat(helpDeepLinkRoute("location")).isEqualTo(Route.SettingsLocation)
        assertThat(helpDeepLinkRoute("qibla")).isEqualTo(Route.Qibla)
        assertThat(helpDeepLinkRoute("quran_settings")).isEqualTo(Route.SettingsQuran)
    }
    @Test fun unknownOrNullKeyReturnsNull() {
        assertThat(helpDeepLinkRoute("nope")).isNull()
        assertThat(helpDeepLinkRoute(null)).isNull()
    }
}
```

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.core.navigation.HelpDeepLinkTest"`
Expected: FAIL (unresolved `helpDeepLinkRoute`).

- [ ] **Step 4: Implement the resolver** (only confirmed routes; unknown → null, safe no-op)

```kotlin
// HelpDeepLink.kt
package com.arshadshah.nimaz.core.navigation

/** Maps a help.json step `deeplink` key to an in-app Route, or null if unknown. */
fun helpDeepLinkRoute(key: String?): Route? = when (key) {
    "prayer_settings" -> Route.SettingsPrayerCalculation
    "notifications" -> Route.SettingsNotifications
    "location" -> Route.SettingsLocation
    "qibla" -> Route.Qibla
    "quran_settings" -> Route.SettingsQuran
    "language" -> Route.SettingsLanguage
    "appearance" -> Route.SettingsAppearance
    "calendar" -> Route.IslamicCalendar
    "settings" -> Route.Settings
    "home" -> Route.Home
    else -> null
}
```

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.core.navigation.HelpDeepLinkTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/core/navigation/Routes.kt \
        app/src/main/java/com/arshadshah/nimaz/core/navigation/HelpDeepLink.kt \
        app/src/test/java/com/arshadshah/nimaz/core/navigation/HelpDeepLinkTest.kt
git commit -m "feat(help): topic/guide routes + deep-link resolver"
```

---

### Task 9: HelpViewModel

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/HelpViewModel.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/HelpViewModelTest.kt`

**Interfaces:**
- Consumes: `HelpUseCases`, `PreferencesDataStore.appLanguage`.
- Produces: `homeState: StateFlow<HelpHomeUiState>`, `topicState: StateFlow<HelpTopicUiState>`, `guideState: StateFlow<HelpGuideUiState>`; `onEvent(HelpEvent)`.

- [ ] **Step 1: Create the ViewModel**

```kotlin
// HelpViewModel.kt
package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.*
import com.arshadshah.nimaz.domain.usecase.HelpUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HelpHomeUiState(
    val topics: List<HelpTopic> = emptyList(),
    val query: String = "",
    val results: List<HelpSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)
data class HelpTopicUiState(
    val detail: HelpTopicDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
data class HelpGuideUiState(
    val guide: HelpGuideDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface HelpEvent {
    data class Search(val query: String) : HelpEvent
    data class LoadTopic(val topicId: String) : HelpEvent
    data class LoadGuide(val guideId: String) : HelpEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HelpViewModel @Inject constructor(
    private val useCases: HelpUseCases,
    preferences: PreferencesDataStore
) : ViewModel() {

    private val language: StateFlow<String> = preferences.appLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    private val query = MutableStateFlow("")

    private val _homeState = MutableStateFlow(HelpHomeUiState())
    val homeState: StateFlow<HelpHomeUiState> = _homeState.asStateFlow()

    private val _topicState = MutableStateFlow(HelpTopicUiState())
    val topicState: StateFlow<HelpTopicUiState> = _topicState.asStateFlow()

    private val _guideState = MutableStateFlow(HelpGuideUiState())
    val guideState: StateFlow<HelpGuideUiState> = _guideState.asStateFlow()

    init {
        // topics re-resolve when language changes
        viewModelScope.launch {
            language.flatMapLatest { lang -> useCases.getTopics(lang) }
                .catch { e -> _homeState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { topics -> _homeState.update { it.copy(topics = topics, isLoading = false) } }
        }
        // search results
        viewModelScope.launch {
            combine(query.debounce(200), language) { q, lang -> q to lang }
                .flatMapLatest { (q, lang) ->
                    if (q.isBlank()) flowOf(emptyList())
                    else useCases.search(q, lang)
                }
                .catch { /* keep last */ }
                .collect { results ->
                    _homeState.update { it.copy(results = results, isSearching = query.value.isNotBlank()) }
                }
        }
    }

    fun onEvent(event: HelpEvent) = when (event) {
        is HelpEvent.Search -> {
            query.value = event.query
            _homeState.update { it.copy(query = event.query) }
        }
        is HelpEvent.LoadTopic -> loadTopic(event.topicId)
        is HelpEvent.LoadGuide -> loadGuide(event.guideId)
    }

    private fun loadTopic(topicId: String) {
        _topicState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            language.flatMapLatest { lang -> useCases.getTopicDetail(topicId, lang) }
                .catch { e -> _topicState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { detail -> _topicState.update { it.copy(detail = detail, isLoading = false) } }
        }
    }

    private fun loadGuide(guideId: String) {
        _guideState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            language.flatMapLatest { lang -> useCases.getGuide(guideId, lang) }
                .catch { e -> _guideState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { guide -> _guideState.update { it.copy(guide = guide, isLoading = false) } }
        }
    }
}
```

(`debounce` requires `@OptIn(FlowPreview::class)` — add it to the class, or import `kotlinx.coroutines.FlowPreview` and annotate.)

- [ ] **Step 2: Write the failing ViewModel test**

```kotlin
// HelpViewModelTest.kt
package com.arshadshah.nimaz.presentation.viewmodel

import app.cash.turbine.test
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.usecase.*
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HelpViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var useCases: HelpUseCases
    private lateinit var prefs: PreferencesDataStore

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        prefs = mockk(relaxed = true)
        every { prefs.appLanguage } returns flowOf("en")
        useCases = mockk(relaxed = true)
        every { useCases.getTopics } returns mockk {
            every { this@mockk.invoke("en") } returns flowOf(
                listOf(HelpTopic("t1", "schedule", "indigo", "Prayer Times", "sub", 1, 3))
            )
        }
    }
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun loadsTopicsOnInit() = runTest {
        val vm = HelpViewModel(useCases, prefs)
        advanceUntilIdle()
        vm.homeState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.topics.single().title).isEqualTo("Prayer Times")
        }
    }
}
```

- [ ] **Step 3: Run the test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.presentation.viewmodel.HelpViewModelTest"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/HelpViewModel.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/HelpViewModelTest.kt
git commit -m "feat(help): HelpViewModel with language-reactive state"
```

---

### Task 10: Help content UI helpers (icon/color mappers + shared composables)

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/help/HelpContentUi.kt`

**Interfaces:**
- Produces: `helpIcon(key: String): ImageVector`, `helpColor(key: String): Color` (Composable), `HelpTopicTile`, `HelpQuestionRow`, `HelpGuideRow`, `HelpStepTimeline` composables consumed by Tasks 11–12.

- [ ] **Step 1: Create the mappers + shared composables.** Map content keys to existing `Icons.*` and theme colors; unknown keys fall back safely.

```kotlin
// HelpContentUi.kt  (abridged: provide the mappers fully; composables follow the design-system patterns)
package com.arshadshah.nimaz.presentation.screens.help

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun helpIcon(key: String): ImageVector = when (key) {
    "schedule" -> Icons.Filled.Schedule
    "notifications_active" -> Icons.Filled.NotificationsActive
    "explore" -> Icons.Filled.Explore
    "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
    "task_alt" -> Icons.Filled.TaskAlt
    "build" -> Icons.Filled.Build
    "tune" -> Icons.Filled.Tune
    "more_time" -> Icons.Filled.MoreTime
    else -> Icons.Filled.HelpOutline
}

/** Tinted accent color for a topic; uses theme-ish hues consistent with the home redesign. */
@Composable
fun helpColor(key: String): Color = when (key) {
    "indigo" -> Color(0xFF6366F1)
    "gold" -> Color(0xFFEAB308)
    "teal" -> Color(0xFF14B8A6)
    "green" -> Color(0xFF22C55E)
    "violet" -> Color(0xFF8B5CF6)
    "orange" -> Color(0xFFF97316)
    else -> Color(0xFF14B8A6)
}
```

Add to the same file the composables `HelpTopicTile`, `HelpQuestionRow` (expandable), `HelpGuideRow`, and `HelpStepTimeline` (numbered nodes + connecting rail, mirroring `TodaysProgressCard`'s node/track drawing but vertical). Build them from `NimazCard(style = OUTLINED)`, tinted icon boxes (icon @ ~12% alpha background), `MaterialTheme.typography`, and spacing of 16.dp. Each is a stateless composable taking the domain model + lambdas. (Render-only; no business logic.)

- [ ] **Step 2: Add a `@Preview`** for each composable using fake data, and build.

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/help/HelpContentUi.kt
git commit -m "feat(help): icon/color mappers + reusable help composables"
```

---

### Task 11: HelpScreen + HelpTopicDetailScreen

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/help/HelpScreen.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/help/HelpTopicDetailScreen.kt`

**Interfaces:**
- Consumes: `HelpViewModel`, `HelpContentUi` composables, `NimazBackTopAppBar`, `NimazSearchBar`, `NimazCard`, `NimazSectionTitle`.
- Produces:
  - `HelpScreen(onNavigateBack: () -> Unit, onNavigateToTopic: (String) -> Unit, onContact: () -> Unit, viewModel: HelpViewModel = hiltViewModel())`
  - `HelpTopicDetailScreen(topicId: String, onNavigateBack: () -> Unit, onOpenGuide: (String) -> Unit, viewModel: HelpViewModel = hiltViewModel())`

- [ ] **Step 1: Build `HelpScreen`** — `Scaffold` + `NimazBackTopAppBar(title = "Help")`; body = `LazyColumn` with: `NimazSearchBar(query, onQueryChange = { viewModel.onEvent(HelpEvent.Search(it)) })`; if `query` blank → "Browse topics" `NimazSectionTitle` + a 2-column grid of `HelpTopicTile`s (click → `onNavigateToTopic(topic.id)`) + a teal contact `NimazCard` (click → `onContact()`); if `query` non-blank → render `homeState.results` as tappable rows (click → `onNavigateToTopic(result.topicId)`; if `itemId != null && isGuide` you may route to topic for now). Collect with `val state by viewModel.homeState.collectAsState()`.

- [ ] **Step 2: Build `HelpTopicDetailScreen`** — `LaunchedEffect(topicId) { viewModel.onEvent(HelpEvent.LoadTopic(topicId)) }`; collect `topicState`; show hero (`helpIcon`/`helpColor` of `detail.topic`), a "Common questions" `NimazCard` of `HelpQuestionRow`s (locally expandable), and a "Step-by-step" section of `HelpGuideRow`s (click → `onOpenGuide(guide.id)`). Handle `isLoading` (spinner) and `detail == null` (empty message).

- [ ] **Step 3: Add `@Preview`s with fake state and build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/help/HelpScreen.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/help/HelpTopicDetailScreen.kt
git commit -m "feat(help): Help home + topic detail screens"
```

---

### Task 12: HelpGuideScreen

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/help/HelpGuideScreen.kt`

**Interfaces:**
- Produces: `HelpGuideScreen(guideId: String, onNavigateBack: () -> Unit, onDeepLink: (String) -> Unit, viewModel: HelpViewModel = hiltViewModel())`.

- [ ] **Step 1: Build the screen** — `LaunchedEffect(guideId) { viewModel.onEvent(HelpEvent.LoadGuide(guideId)) }`; collect `guideState`; show a centered hero (icon, `guide.title`, a `N steps · about M min` chip), then `HelpStepTimeline(steps = guide.steps, onPathChipClick = { step -> step.deeplinkRoute?.let(onDeepLink) })`, then a green "That's it!" `NimazCard`. The path chip is shown only when `step.pathLabels.isNotEmpty()`, and is clickable only when `step.deeplinkRoute != null`.

- [ ] **Step 2: Add `@Preview` and build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/help/HelpGuideScreen.kt
git commit -m "feat(help): step-by-step guide screen with deep-linking path chips"
```

---

### Task 13: NavGraph wiring + delete old screen

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt`
- Delete: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/help/HelpSupportScreen.kt`

**Interfaces:**
- Consumes: the 3 screens, `helpDeepLinkRoute`.

- [ ] **Step 1: Replace the `Route.SettingsHelp` registration** and add the two new ones:

```kotlin
composable<Route.SettingsHelp> {
    com.arshadshah.nimaz.presentation.screens.help.HelpScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToTopic = { topicId -> navController.navigate(Route.HelpTopicDetail(topicId)) },
        onContact = {
            val email = context.getString(com.arshadshah.nimaz.R.string.support_email)
            val subject = context.getString(com.arshadshah.nimaz.R.string.nimaz_support_request)
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:$email")
                putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            }
            context.startActivity(android.content.Intent.createChooser(intent, email))
        }
    )
}
composable<Route.HelpTopicDetail> { backStackEntry ->
    val args = backStackEntry.toRoute<Route.HelpTopicDetail>()
    com.arshadshah.nimaz.presentation.screens.help.HelpTopicDetailScreen(
        topicId = args.topicId,
        onNavigateBack = { navController.popBackStack() },
        onOpenGuide = { guideId -> navController.navigate(Route.HelpGuide(guideId)) }
    )
}
composable<Route.HelpGuide> { backStackEntry ->
    val args = backStackEntry.toRoute<Route.HelpGuide>()
    com.arshadshah.nimaz.presentation.screens.help.HelpGuideScreen(
        guideId = args.guideId,
        onNavigateBack = { navController.popBackStack() },
        onDeepLink = { key ->
            com.arshadshah.nimaz.core.navigation.helpDeepLinkRoute(key)?.let { route ->
                navController.navigate(route)
            }
        }
    )
}
```

Ensure a `val context = LocalContext.current` is in scope where `SettingsHelp` is registered (the About block already does this; add it if needed), and that `androidx.navigation.toRoute` is imported.

- [ ] **Step 2: Delete the old screen file**

```bash
git rm app/src/main/java/com/arshadshah/nimaz/presentation/screens/help/HelpSupportScreen.kt
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (no remaining references to `HelpSupportScreen`). If the tablet `AdaptiveMoreScreen`/`MoreDetailPane.HELP` path references `HelpSupportScreen`, repoint it to `HelpScreen` with the same callbacks.

- [ ] **Step 4: Smoke test on a device/emulator.** Install, open More → Help: verify topics load (seeder ran), search filters, a topic opens, a guide opens, and a path chip navigates to Prayer Settings. Switch app language and reopen Help to confirm strings change (English fallback where translations are absent).

Run: `./gradlew :app:installDebug`
Expected: app launches; Help flow works as described.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt
git commit -m "feat(help): wire Help screens into navigation; remove old HelpSupportScreen"
```

---

## Phase 3 — About

### Task 14: About screen rebuild

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/about/AboutScreen.kt`
- Modify: `app/src/main/res/values/strings.xml` (+ `values-*/strings.xml` for the new tagline/labels)

**Interfaces:**
- Keep the existing `AboutScreen(...)` signature and callbacks (`onNavigateBack`, `onNavigateToPrivacyPolicy`, `onNavigateToTerms`, `onNavigateToLicenses`, `onRateApp`, `onShareApp`, `onContactUs`) so `NavGraph` needs no change. Keep `LocalInAppUpdateManager` + `UpdateState` handling and `BuildConfig.VERSION_NAME/CODE`.

- [ ] **Step 1: Add strings** to `res/values/strings.xml`:

```xml
<string name="about_tagline">Your daily Islamic companion — accurate prayer times, Quran, qibla and more.</string>
<string name="about_links">Links</string>
<string name="about_developer">Developer</string>
<string name="about_built_with">Built with data from</string>
<string name="about_rate">Rate</string>
<string name="about_share">Share</string>
<string name="about_updates">Updates</string>
```

- [ ] **Step 2: Rebuild the composable** to the approved layout, top→bottom: branded hero (app logo, "Nimaz", version chip via `version_detail_format`, `about_tagline`); quick-actions row (Rate→`onRateApp`, Share→`onShareApp`, Updates→drives `updateManager` exactly as today); `NimazSectionTitle(about_links)` + links `NimazCard` (Website opens `https://nimaz.arshadshah.com`, Privacy→`onNavigateToPrivacyPolicy`, Terms→`onNavigateToTerms`, Licenses→`onNavigateToLicenses`, Check-for-updates row shows the `updateSubtitle` state as an inline badge); compact developer `NimazCard` (avatar "AS", `developer_name`, `developer_role`, github/linkedin icon buttons opening the existing URLs via `LocalUriHandler`); `NimazSectionTitle(about_built_with)` + the 6 credits as a 2-column grid of small `NimazCard`s (reuse the existing `credits` list); footer ("Made with ♥ for the Ummah", `copyright_format`). Preserve all existing URLs and resource names extracted from the current screen.

- [ ] **Step 3: Translate the new strings** in each `values-<lang>/strings.xml` (tr, id, ms, fr, de). If a translation is unavailable at implementation time, omit the override so Android falls back to the English value (acceptable; matches app behavior).

- [ ] **Step 4: Add `@Preview` + build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/about/AboutScreen.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-*/strings.xml
git commit -m "feat(about): redesign About screen (hero, quick actions, credits grid)"
```

---

## Phase 4 — Localization content (follow-up)

### Task 15: Translate help.json into tr/id/ms/fr/de

**Files:**
- Modify: `app/src/main/assets/help/help.json`

**Interfaces:** content only; bump `contentVersion` so the seeder re-imports for existing users.

- [ ] **Step 1:** For every `{ "en": "…" }` map in `help.json`, add `tr`, `id`, `ms`, `fr`, `de` entries (translation task). Missing languages safely fall back to English at runtime.
- [ ] **Step 2:** Increment `contentVersion` (e.g. `1` → `2`).
- [ ] **Step 3:** Validate JSON: `python3 -c "import json; json.load(open('app/src/main/assets/help/help.json')); print('valid')"`.
- [ ] **Step 4:** Install on device, switch each language, open Help, confirm localized content. Commit:

```bash
git add app/src/main/assets/help/help.json
git commit -m "content(help): localize help.json to all supported languages"
```

---

## Self-review notes

- **Spec coverage:** searchable topic-grid Help (Tasks 9–13), short answers + guides with deep-linking steps (Tasks 6, 8, 12), localized content with EN fallback (Tasks 5–6, 15), content-agnostic data-driven rendering (Tasks 3–6), seeding for all users via migration + runtime seeder (Tasks 1, 5), About redesign (Task 14) — all mapped.
- **Seeding decision:** runtime JSON seeder chosen; the prepopulated asset and `nimaz-pro-data/` are intentionally untouched. Tables are created for everyone by `MIGRATION_13_14`.
- **Known intentional simplifications (documented inline):** `HelpItem.HelpGuide.stepCount` is `0` in the topic-list projection (real step count is shown on the guide screen); search snippets are empty in v1 (title-only results). Deep-link resolver covers only confirmed routes; unknown keys are safe no-ops — extend as more routes are confirmed.
- **Type consistency:** `HelpRepository` signatures, `HelpUseCases` invokers, and `HelpViewModel` usage all use `(… , lang: String)` and the same model names throughout.
