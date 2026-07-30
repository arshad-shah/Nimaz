package com.arshadshah.nimaz.support

import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiBookmarkEntity
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
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranFavoriteEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerBlockEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity

/**
 * Factory for fully-formed test entities.
 *
 * Every builder has sensible defaults so a test overrides only the fields it cares
 * about (`prayerRecord(status = "prayed")`). This keeps entity construction — and
 * the field-name strings that go with it — in one place instead of scattered across
 * the DAO tests. Timestamps default to a fixed epoch where ordering matters so
 * assertions stay deterministic.
 */
object TestData {

    /** A fixed reference instant: 2026-01-01T00:00:00Z, in millis. */
    const val T0: Long = 1_767_225_600_000L

    /** A fixed civil date key (midnight UTC) used as the day bucket for trackers. */
    const val DAY: Long = T0

    // ── Prayer ──────────────────────────────────────────────────────────────
    fun prayerRecord(
        date: Long = DAY,
        prayerName: String = "Fajr",
        status: String = "pending",
        prayedAt: Long? = null,
        scheduledTime: Long = DAY + 5 * 3_600_000L,
        isJamaah: Boolean = false,
        note: String? = null,
    ) = PrayerRecordEntity(
        date = date,
        prayerName = prayerName,
        status = status,
        prayedAt = prayedAt,
        scheduledTime = scheduledTime,
        isJamaah = isJamaah,
        note = note,
    )

    /** The five daily prayers for [date], all pending. */
    fun dailyPrayers(date: Long = DAY): List<PrayerRecordEntity> =
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").mapIndexed { i, name ->
            prayerRecord(date = date, prayerName = name, scheduledTime = date + (i + 1) * 3_600_000L)
        }

    // ── Fasting ─────────────────────────────────────────────────────────────
    fun fastRecord(
        date: Long = DAY,
        fastType: String = "ramadan",
        status: String = "fasted",
        hijriMonth: Int? = 9,
    ) = FastRecordEntity(
        date = date,
        hijriDate = null,
        hijriMonth = hijriMonth,
        hijriYear = 1447,
        fastType = fastType,
        status = status,
        exemptionReason = null,
        suhoorTime = null,
        iftarTime = null,
        note = null,
    )

    fun makeupFast(
        originalDate: Long = DAY,
        status: String = "pending",
        reason: String = "missed",
    ) = MakeupFastEntity(
        originalDate = originalDate,
        originalHijriDate = null,
        reason = reason,
        status = status,
        completedDate = null,
        fidyaAmount = null,
        note = null,
    )

    // ── Tasbih ──────────────────────────────────────────────────────────────
    fun tasbihPreset(
        name: String = "SubhanAllah",
        targetCount: Int = 33,
        isCustom: Int = 0,
        displayOrder: Int = 0,
        category: String? = null,
    ) = TasbihPresetEntity(
        name = name,
        arabic = "سبحان الله",
        transliteration = "SubhanAllah",
        translation = "Glory be to Allah",
        targetCount = targetCount,
        isCustom = isCustom,
        displayOrder = displayOrder,
        category = category,
    )

    fun tasbihSession(
        presetId: Long? = null,
        date: Long = DAY,
        currentCount: Int = 0,
        targetCount: Int = 33,
        totalLaps: Int = 0,
        isCompleted: Boolean = false,
    ) = TasbihSessionEntity(
        presetId = presetId,
        presetName = "SubhanAllah",
        date = date,
        currentCount = currentCount,
        targetCount = targetCount,
        totalLaps = totalLaps,
        isCompleted = isCompleted,
        duration = null,
        startedAt = date,
        completedAt = null,
        note = null,
    )

    // ── Khatam ──────────────────────────────────────────────────────────────
    fun khatam(
        name: String = "Ramadan Khatam",
        status: String = "active",
        isActive: Boolean = true,
        dailyTarget: Int = 20,
    ) = KhatamEntity(
        name = name,
        status = status,
        isActive = isActive,
        dailyTarget = dailyTarget,
    )

    fun khatamAyah(khatamId: Long, ayahId: Int) =
        KhatamAyahEntity(khatamId = khatamId, ayahId = ayahId)

    fun khatamDailyLog(khatamId: Long, date: Long = DAY, ayahsRead: Int = 20) =
        KhatamDailyLogEntity(khatamId = khatamId, date = date, ayahsRead = ayahsRead)

    // ── Quran ───────────────────────────────────────────────────────────────
    fun surah(
        id: Int = 1,
        number: Int = id,
        nameEnglish: String = "Al-Fatihah",
        versesCount: Int = 7,
        startPage: Int = 1,
    ) = SurahEntity(
        id = id,
        number = number,
        nameArabic = "الفاتحة",
        nameEnglish = nameEnglish,
        nameTransliteration = "Al-Fatihah",
        revelationType = "Meccan",
        versesCount = versesCount,
        orderRevealed = 5,
        startPage = startPage,
    )

    fun ayah(
        id: Int = 1,
        surahId: Int = 1,
        numberInSurah: Int = id,
        numberGlobal: Int = id,
        juz: Int = 1,
        page: Int = 1,
    ) = AyahEntity(
        id = id,
        surahId = surahId,
        numberInSurah = numberInSurah,
        numberGlobal = numberGlobal,
        textArabic = "بِسْمِ اللَّهِ",
        textUthmani = "بِسْمِ اللَّهِ",
        juz = juz,
        hizb = 1,
        page = page,
        sajda = 0,
        sajdaType = null,
    )

    fun quranBookmark(ayahId: Int = 1, surahNumber: Int = 1, ayahNumber: Int = 1) =
        QuranBookmarkEntity(
            ayahId = ayahId,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            note = null,
            color = null,
        )

    fun quranFavorite(ayahId: Int = 1, surahNumber: Int = 1, ayahNumber: Int = 1) =
        QuranFavoriteEntity(ayahId = ayahId, surahNumber = surahNumber, ayahNumber = ayahNumber)

    fun readingProgress(
        lastReadSurah: Int = 2,
        lastReadAyah: Int = 5,
        totalAyahsRead: Int = 42,
    ) = ReadingProgressEntity(
        lastReadSurah = lastReadSurah,
        lastReadAyah = lastReadAyah,
        lastReadPage = 2,
        lastReadJuz = 1,
        totalAyahsRead = totalAyahsRead,
        currentKhatmaCount = 0,
    )

    // ── Dua ─────────────────────────────────────────────────────────────────
    fun duaCategory(id: Int = 1, nameEnglish: String = "Morning") =
        DuaCategoryEntity(
            id = id,
            nameEnglish = nameEnglish,
            nameArabic = "الصباح",
            icon = "sun",
            displayOrder = 0,
            duaCount = 1,
        )

    fun dua(id: Int = 1, categoryId: Int = 1, titleEnglish: String = "Morning Dua") =
        DuaEntity(
            id = id,
            categoryId = categoryId,
            titleEnglish = titleEnglish,
            titleArabic = "دعاء",
            textArabic = "اللهم",
            transliteration = "Allahumma",
            translation = "O Allah",
            source = "Bukhari",
            virtue = null,
            repeatCount = 3,
            audioFile = null,
            displayOrder = 0,
        )

    fun duaBookmark(duaId: Int = 1, categoryId: Int = 1, isFavorite: Boolean = true) =
        DuaBookmarkEntity(
            duaId = duaId,
            categoryId = categoryId,
            note = null,
            isFavorite = isFavorite,
        )

    fun duaProgress(
        duaId: Int = 1,
        date: Long = DAY,
        completedCount: Int = 0,
        targetCount: Int = 3,
        isCompleted: Boolean = false,
    ) = DuaProgressEntity(
        duaId = duaId,
        date = date,
        completedCount = completedCount,
        targetCount = targetCount,
        isCompleted = isCompleted,
    )

    // ── Hadith ──────────────────────────────────────────────────────────────
    fun hadithBook(id: Int = 1, nameEnglish: String = "Sahih Bukhari") =
        HadithBookEntity(
            id = id,
            nameEnglish = nameEnglish,
            nameArabic = "صحيح البخاري",
            author = "Imam Bukhari",
            hadithCount = 1,
            description = "Authentic collection",
            icon = "book",
        )

    fun hadith(id: Int = 1, bookId: Int = 1, chapterId: Int = 1, numberInBook: Int = 1) =
        HadithEntity(
            id = id,
            bookId = bookId,
            chapterId = chapterId,
            numberInBook = numberInBook,
            numberInChapter = 1,
            textArabic = "إنما الأعمال بالنيات",
            textEnglish = "Actions are by intentions",
            narrator = "Umar ibn al-Khattab",
            grade = "Sahih",
            reference = "Bukhari 1",
        )

    fun hadithBookmark(hadithId: Int = 1, bookId: Int = 1, hadithNumber: Int = 1) =
        HadithBookmarkEntity(
            hadithId = hadithId,
            bookId = bookId,
            hadithNumber = hadithNumber,
            note = null,
            color = null,
        )

    // ── Zakat ───────────────────────────────────────────────────────────────
    fun zakat(
        calculatedAt: Long = T0,
        zakatDue: Double = 250.0,
        isPaid: Boolean = false,
    ) = ZakatHistoryEntity(
        calculatedAt = calculatedAt,
        totalAssets = 10_000.0,
        totalLiabilities = 0.0,
        netWorth = 10_000.0,
        zakatDue = zakatDue,
        nisabType = "gold",
        nisabValue = 5_000.0,
        isPaid = isPaid,
    )

    // ── Tafseer ─────────────────────────────────────────────────────────────
    fun tafseerBlock(ayahStart: Int = 1, ayahEnd: Int = ayahStart, tafseerId: String = "ibn-kathir") =
        TafseerBlockEntity(
            tafseerId = tafseerId,
            surahNumber = 1,
            ayahStart = ayahStart,
            ayahEnd = ayahEnd,
            text = "Commentary text",
        )

    fun tafseerHighlight(ayahId: Int = 1, tafseerId: String = "ibn-kathir") =
        TafseerHighlightEntity(
            ayahId = ayahId,
            tafseerId = tafseerId,
            startOffset = 0,
            endOffset = 10,
            color = "#FFEB3B",
            note = "important",
        )

    fun tafseerNote(ayahId: Int = 1, tafseerId: String = "ibn-kathir", text: String = "my note") =
        TafseerNoteEntity(ayahId = ayahId, tafseerId = tafseerId, text = text)

    // ── Qaida progress ──────────────────────────────────────────────────────
    fun qaidaLessonProgress(
        lessonId: Int = 1,
        status: String = "IN_PROGRESS",
        stars: Int = 0,
        completedCells: Int = 0,
        totalCells: Int = 10,
    ) = QaidaLessonProgressEntity(
        lessonId = lessonId,
        status = status,
        stars = stars,
        lastCellId = null,
        completedCells = completedCells,
        totalCells = totalCells,
        updatedAt = T0,
    )

    // ── Names of Allah / Prophet bookmarks ──────────────────────────────────
    fun asmaBookmark(nameId: Int = 1) = AsmaUlHusnaBookmarkEntity(nameId = nameId)
    fun asmaNabiBookmark(nameId: Int = 1) = AsmaUnNabiBookmarkEntity(nameId = nameId)
    fun prophetBookmark(prophetId: Int = 1) = ProphetBookmarkEntity(prophetId = prophetId)

    // ── Location & events ───────────────────────────────────────────────────
    fun location(
        name: String = "Makkah",
        latitude: Double = 21.4225,
        longitude: Double = 39.8262,
        isCurrent: Boolean = true,
        isFavorite: Boolean = false,
    ) = LocationEntity(
        name = name,
        latitude = latitude,
        longitude = longitude,
        timezone = "Asia/Riyadh",
        country = "Saudi Arabia",
        city = "Makkah",
        isCurrentLocation = isCurrent,
        isFavorite = isFavorite,
        calculationMethod = "MWL",
        asrCalculation = "standard",
        highLatitudeRule = null,
        fajrAngle = null,
        ishaAngle = null,
    )

    fun islamicEvent(
        id: Int = 1,
        hijriMonth: Int = 9,
        hijriDay: Int = 1,
        eventType: String = "fast",
        isHoliday: Int = 0,
    ) = IslamicEventEntity(
        id = id,
        nameEnglish = "Start of Ramadan",
        nameArabic = "رمضان",
        hijriMonth = hijriMonth,
        hijriDay = hijriDay,
        eventType = eventType,
        description = "First day of fasting",
        isHoliday = isHoliday,
    )
}
