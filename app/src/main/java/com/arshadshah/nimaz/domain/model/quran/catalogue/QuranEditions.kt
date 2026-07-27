package com.arshadshah.nimaz.domain.model.quran.catalogue

/**
 * The Quran content registry — the single source of truth for every selectable translation,
 * mushaf layout, tafseer, reciter and Arabic font the app ships.
 *
 * ## Adding an edition
 * Adding a translation, layout, tafseer, reciter or font means adding **one entry here**,
 * plus (for anything with a bundled asset) one line in
 * [com.arshadshah.nimaz.data.local.quran.QuranContentAssets] and a licence block in
 * `nimaz-pro-data/json/`. Pickers, page counts, credits, the tajweed guard and the audio
 * player all derive from this object — none of them enumerate editions themselves.
 *
 * Reciters and fonts need no asset at all, so for those it really is the one entry (plus, for
 * a font, the `.ttf` and its `FontFamily`).
 *
 * ## What must not go in here
 * Asset paths, CDN identifiers, Room entities and Compose `FontFamily`s. Domain cannot import
 * `data` or Compose, so those bindings live in their own layer and are held in step by
 * `QuranEditionRegistryTest`, which fails the build if the id sets diverge. See ADR-002.
 *
 * ## Ids are permanent
 * Ids are persisted — in DataStore preferences and in `translations.translator_id` /
 * `mushaf_layouts.layout_id` rows. Renaming one orphans user selections and seeded rows. To
 * rename an edition, change its `displayName` and leave the id alone. Values persisted before
 * this registry existed are carried as `legacyKeys` rather than migrated (ADR-003).
 */
object QuranEditions {

    /**
     * Shipped translations, one per language the app's UI supports, plus Urdu.
     *
     * All but the first are sourced from Tanzil and bundled as seeded assets. Tanzil hosts
     * these translations without relicensing them: its blanket term is **non-commercial use
     * only**, which Nimaz relies on — the app is free, with no ads and no in-app purchases. If
     * that ever changes, every Tanzil-sourced entry needs permission from its translator or
     * publisher first. See `nimaz-pro-data/json/LICENSES_TRANSLATIONS.md`.
     */
    val translations: List<TranslationEdition> = listOf(
        TranslationEdition(
            id = "sahih_international",
            displayName = "Sahih International",
            translatorName = "Sahih International",
            languageTag = "en"
        ),
        TranslationEdition(
            id = "pickthall",
            displayName = "Pickthall",
            translatorName = "Marmaduke Pickthall",
            languageTag = "en"
        ),
        TranslationEdition(
            id = "jalandhry",
            displayName = "جالندہری",
            translatorName = "Fateh Muhammad Jalandhry",
            languageTag = "ur",
            isRightToLeft = true,
            // Urdu is written in Nastaʿlīq; the body font has no coverage for it.
            fontId = "indopak"
        ),
        TranslationEdition(
            id = "diyanet",
            displayName = "Diyanet İşleri",
            translatorName = "Diyanet İşleri Başkanlığı",
            languageTag = "tr"
        ),
        TranslationEdition(
            id = "indonesian",
            displayName = "Bahasa Indonesia",
            translatorName = "Kementerian Agama Republik Indonesia",
            languageTag = "id"
        ),
        TranslationEdition(
            id = "basmeih",
            displayName = "Bahasa Melayu",
            translatorName = "Abdullah Muhammad Basmeih",
            languageTag = "ms"
        ),
        TranslationEdition(
            id = "hamidullah",
            displayName = "Hamidullah",
            translatorName = "Muhammad Hamidullah",
            languageTag = "fr"
        ),
        TranslationEdition(
            id = "bubenheim",
            displayName = "Bubenheim & Elyas",
            translatorName = "Frank Bubenheim and Nadeem Elyas",
            languageTag = "de"
        )
    )

    val mushafLayouts: List<MushafLayoutEdition> = listOf(
        MushafLayoutEdition(
            id = "madani",
            displayName = "Madani (Uthmani)",
            totalPages = 604,
            linesPerPage = null, // flowed: paginated by the `ayahs.page` column
            textSource = AyahTextSource.UTHMANI,
            supportsTajweed = true,
            fontId = "amiri",
            // Persisted as the MushafScript enum name before the registry existed.
            legacyKeys = setOf("MADANI")
        ),
        MushafLayoutEdition(
            id = "indopak16",
            displayName = "IndoPak — 16 line",
            totalPages = 548,
            linesPerPage = 16,
            textSource = AyahTextSource.INDOPAK,
            // The IndoPak text carries no per-letter tajweed spans (see MushafLineLayout).
            supportsTajweed = false,
            fontId = "indopak",
            legacyKeys = setOf("INDOPAK_16")
        )
    )

    val tafseers: List<TafseerEdition> = listOf(
        TafseerEdition(
            id = "ibn_kathir_en",
            displayName = "Ibn Kathir",
            authorName = "Ismāʿīl ibn Kathīr",
            languageTag = "en"
        ),
        TafseerEdition(
            id = "maariful_quran_en",
            displayName = "Ma'arif al-Qur'an",
            authorName = "Mufti Muhammad Shafi",
            languageTag = "en"
        )
    )

    /**
     * Reciters offered by the picker.
     *
     * This list is the *only* place a reciter is declared. It previously existed three times
     * over — `SelectReciterScreen.popularReciters`, `QuranAudioManager.RECITER_CDN_MAP` and
     * that class's `getReciterDisplayName` — which had already drifted: the CDN map carried
     * five reciters the picker never offered, and Maher Al-Muaiqly's name was spelled two
     * different ways depending on which copy rendered it.
     */
    val reciters: List<ReciterEdition> = listOf(
        ReciterEdition("mishary", "Mishary Rashid Alafasy", "Kuwait", "Murattal",
            // The audio layer accepted "alafasy" for the same reciter.
            legacyKeys = setOf("alafasy")),
        ReciterEdition("sudais", "Abdul Rahman Al-Sudais", "Saudi Arabia", "Murattal"),
        ReciterEdition("abdulbasit", "Abdul Basit Abdul Samad", "Egypt", "Mujawwad"),
        ReciterEdition("maher", "Maher Al-Muaiqly", "Saudi Arabia", "Murattal",
            legacyKeys = setOf("muaiqly")),
        ReciterEdition("minshawi", "Muhammad Siddiq Al-Minshawi", "Egypt", "Mujawwad"),
        ReciterEdition("hussary", "Mahmoud Khalil Al-Hussary", "Egypt", "Murattal"),
        ReciterEdition("ajamy", "Ahmed Al-Ajamy", "Saudi Arabia", "Murattal"),
        ReciterEdition("shuraim", "Saud Al-Shuraim", "Saudi Arabia", "Murattal"),
        ReciterEdition("shaatree", "Abu Bakr Al-Shaatree", "Saudi Arabia", "Murattal"),
        ReciterEdition("hudhaify", "Ali Al-Hudhaify", "Saudi Arabia", "Murattal"),
        // Present in the CDN map but never offered by the old picker; now selectable.
        ReciterEdition("ayyoub", "Muhammad Ayyoub", "Saudi Arabia", "Murattal"),
        ReciterEdition("jibreel", "Muhammad Jibreel", "Egypt", "Murattal"),
        ReciterEdition("basfar", "Abdullah Basfar", "Saudi Arabia", "Murattal")
    )

    /**
     * Selectable Arabic fonts. The `FontFamily` for each lives in
     * [com.arshadshah.nimaz.presentation.theme.QuranArabicFont], keyed by these ids.
     */
    val arabicFonts: List<ArabicFontEdition> = listOf(
        ArabicFontEdition("amiri", "Amiri"),
        ArabicFontEdition("scheherazade", "Scheherazade New"),
        ArabicFontEdition("indopak", "IndoPak (Nastaʿlīq)")
    )

    val defaultTranslation: TranslationEdition = translations.first()
    val defaultLayout: MushafLayoutEdition = mushafLayouts.first()
    val defaultTafseer: TafseerEdition = tafseers.first()
    val defaultReciter: ReciterEdition = reciters.first()
    val defaultArabicFont: ArabicFontEdition = arabicFonts.first()

    /**
     * The largest page count across layouts — the safe upper bound for a page number whose
     * target edition is not known yet (e.g. a deep link resolved before the user's layout
     * preference has been read). The reader clamps to the active edition afterwards.
     */
    val maxTotalPages: Int = mushafLayouts.maxOf { it.totalPages }

    /** Resolves a persisted translator id, falling back through legacy keys then the default. */
    fun translation(id: String?): TranslationEdition =
        translations.resolveEdition(id, defaultTranslation)

    /** Resolves a persisted layout id (including the legacy `MADANI` / `INDOPAK_16` names). */
    fun layout(id: String?): MushafLayoutEdition =
        mushafLayouts.resolveEdition(id, defaultLayout)

    fun tafseer(id: String?): TafseerEdition = tafseers.resolveEdition(id, defaultTafseer)

    fun reciter(id: String?): ReciterEdition = reciters.resolveEdition(id, defaultReciter)

    fun arabicFont(id: String?): ArabicFontEdition =
        arabicFonts.resolveEdition(id, defaultArabicFont)

    /** Every edition across every axis — used by the registry-pairing test and by credits. */
    val all: List<QuranEdition>
        get() = translations + mushafLayouts + tafseers + reciters + arabicFonts
}
