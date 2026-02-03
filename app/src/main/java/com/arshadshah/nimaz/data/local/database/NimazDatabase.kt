package com.arshadshah.nimaz.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiEntity
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.FastRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithBookEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithEntity
import com.arshadshah.nimaz.data.local.database.entity.IslamicEventEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamAyahEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamDailyLogEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import com.arshadshah.nimaz.data.local.database.entity.LocationEntity
import com.arshadshah.nimaz.data.local.database.entity.MakeupFastEntity
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.ProphetBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.ProphetEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranFavoriteEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahInfoEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerTextEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity

@Database(
    entities = [
        // Quran
        SurahEntity::class,
        AyahEntity::class,
        TranslationEntity::class,
        QuranBookmarkEntity::class,
        QuranFavoriteEntity::class,
        ReadingProgressEntity::class,
        SurahInfoEntity::class,
        // Hadith
        HadithBookEntity::class,
        HadithEntity::class,
        HadithBookmarkEntity::class,
        // Dua
        DuaCategoryEntity::class,
        DuaEntity::class,
        DuaBookmarkEntity::class,
        DuaProgressEntity::class,
        // Prayer & Fasting
        PrayerRecordEntity::class,
        FastRecordEntity::class,
        MakeupFastEntity::class,
        // Tasbih
        TasbihPresetEntity::class,
        TasbihSessionEntity::class,
        // Zakat
        ZakatHistoryEntity::class,
        // Tafseer
        TafseerTextEntity::class,
        TafseerHighlightEntity::class,
        TafseerNoteEntity::class,
        // Khatam
        KhatamEntity::class,
        KhatamAyahEntity::class,
        KhatamDailyLogEntity::class,
        // Asma ul Husna
        AsmaUlHusnaEntity::class,
        AsmaUlHusnaBookmarkEntity::class,
        // Asma un Nabi
        AsmaUnNabiEntity::class,
        AsmaUnNabiBookmarkEntity::class,
        // Prophets
        ProphetEntity::class,
        ProphetBookmarkEntity::class,
        // Other
        LocationEntity::class,
        IslamicEventEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class NimazDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun hadithDao(): HadithDao
    abstract fun duaDao(): DuaDao
    abstract fun prayerDao(): PrayerDao
    abstract fun fastingDao(): FastingDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun locationDao(): LocationDao
    abstract fun islamicEventDao(): IslamicEventDao
    abstract fun zakatDao(): ZakatDao
    abstract fun tafseerDao(): TafseerDao
    abstract fun khatamDao(): KhatamDao
    abstract fun asmaUlHusnaDao(): AsmaUlHusnaDao
    abstract fun asmaUnNabiDao(): AsmaUnNabiDao
    abstract fun prophetDao(): ProphetDao

    companion object {
        const val DATABASE_NAME = "nimaz_database"

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create asma_ul_husna table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `asma_ul_husna` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `number` INTEGER NOT NULL,
                        `name_arabic` TEXT NOT NULL,
                        `name_transliteration` TEXT NOT NULL,
                        `name_english` TEXT NOT NULL,
                        `meaning` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `benefits` TEXT NOT NULL,
                        `quran_references` TEXT NOT NULL,
                        `usage_in_dua` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create asma_ul_husna_bookmarks table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `asma_ul_husna_bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name_id` INTEGER NOT NULL,
                        `is_favorite` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_asma_ul_husna_bookmarks_name_id` ON `asma_ul_husna_bookmarks` (`name_id`)")

                // Create asma_un_nabi table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `asma_un_nabi` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `number` INTEGER NOT NULL,
                        `name_arabic` TEXT NOT NULL,
                        `name_transliteration` TEXT NOT NULL,
                        `name_english` TEXT NOT NULL,
                        `meaning` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create asma_un_nabi_bookmarks table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `asma_un_nabi_bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name_id` INTEGER NOT NULL,
                        `is_favorite` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_asma_un_nabi_bookmarks_name_id` ON `asma_un_nabi_bookmarks` (`name_id`)")

                // Create prophets table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `prophets` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `number` INTEGER NOT NULL,
                        `name_arabic` TEXT NOT NULL,
                        `name_english` TEXT NOT NULL,
                        `name_transliteration` TEXT NOT NULL,
                        `title_arabic` TEXT NOT NULL,
                        `title_english` TEXT NOT NULL,
                        `story_summary` TEXT NOT NULL,
                        `key_lessons` TEXT NOT NULL,
                        `quran_mentions` TEXT NOT NULL,
                        `era` TEXT NOT NULL,
                        `lineage` TEXT NOT NULL,
                        `years_lived` TEXT NOT NULL,
                        `place_of_preaching` TEXT NOT NULL,
                        `miracles` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create prophet_bookmarks table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `prophet_bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `prophet_id` INTEGER NOT NULL,
                        `is_favorite` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_prophet_bookmarks_prophet_id` ON `prophet_bookmarks` (`prophet_id`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create khatams table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `khatams` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `notes` TEXT,
                        `status` TEXT NOT NULL DEFAULT 'active',
                        `is_active` INTEGER NOT NULL DEFAULT 0,
                        `daily_target` INTEGER NOT NULL DEFAULT 20,
                        `deadline` INTEGER,
                        `reminder_enabled` INTEGER NOT NULL DEFAULT 0,
                        `reminder_time` TEXT,
                        `total_ayahs_read` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        `started_at` INTEGER,
                        `completed_at` INTEGER,
                        `updated_at` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create khatam_ayahs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `khatam_ayahs` (
                        `khatam_id` INTEGER NOT NULL,
                        `ayah_id` INTEGER NOT NULL,
                        `read_at` INTEGER NOT NULL,
                        PRIMARY KEY(`khatam_id`, `ayah_id`),
                        FOREIGN KEY(`khatam_id`) REFERENCES `khatams`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create indexes for khatam_ayahs
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khatam_ayahs_khatam_id` ON `khatam_ayahs` (`khatam_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khatam_ayahs_ayah_id` ON `khatam_ayahs` (`ayah_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khatam_ayahs_read_at` ON `khatam_ayahs` (`read_at`)")

                // Create khatam_daily_log table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `khatam_daily_log` (
                        `khatam_id` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `ayahs_read` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`khatam_id`, `date`),
                        FOREIGN KEY(`khatam_id`) REFERENCES `khatams`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create index for khatam_daily_log
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khatam_daily_log_khatam_id` ON `khatam_daily_log` (`khatam_id`)")
            }
        }
    }
}
