package com.arshadshah.nimaz.domain.model

/**
 * The language a [QuranTranslation] is written in.
 *
 * [isRtl] drives the text direction the translation is *rendered* with — an Urdu translation
 * must lay out right-to-left even when the app's own locale is left-to-right, so the reader
 * cannot simply inherit the ambient layout direction.
 */
enum class TranslationLanguage(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val isRtl: Boolean = false
) {
    ENGLISH("en", "English", "English"),
    URDU("ur", "Urdu", "اردو", isRtl = true),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia"),
    TURKISH("tr", "Turkish", "Türkçe"),
    FRENCH("fr", "French", "Français"),
    BENGALI("bn", "Bengali", "বাংলা"),
    HINDI("hi", "Hindi", "हिन्दी"),
    SPANISH("es", "Spanish", "Español"),
    RUSSIAN("ru", "Russian", "Русский"),
    MALAY("ms", "Malay", "Bahasa Melayu"),
    GERMAN("de", "German", "Deutsch")
}

/**
 * Every Quran translation the app ships — the single source of truth, in the spirit of
 * [com.arshadshah.nimaz.presentation.theme.QuranArabicFont]. The settings picker and the
 * reader both derive from this enum; nothing else enumerates translations.
 *
 * ## Adding one
 * 1. In the arshad-shah/nimaz-data repository, add the edition and run the importer.
 *    It writes a `tr.<id>` collection, and `nz build` ships it in the artifact.
 * 2. Add an entry here with the same [id].
 * 3. Run `nz import --check` in that repository, which fails if the two catalogues drift.
 *
 * That is the whole change: the picker lists it, and its verses arrive in the
 * `translations` table with the artifact — there is no app-side seeding step since
 * `QuranTranslationSeeder` was retired at versionCode 385 (`docs/retirement.yaml`).
 *
 * ## Why [id] is load-bearing
 * [id] is written to `translations.translator_id` **and** persisted as the user's
 * `quran_translator_id` preference, so renaming one silently strands that user on a
 * translation that no longer resolves. Treat these as frozen. [SAHIH_INTERNATIONAL] keeps
 * its unprefixed legacy id for exactly this reason — it predates the `<lang>_<name>` scheme
 * and is already stored on every existing install.
 */
enum class QuranTranslation(
    val id: String,
    val translator: String,
    val language: TranslationLanguage
) {
    // English
    SAHIH_INTERNATIONAL(
        id = "sahih_international",
        translator = "Saheeh International",
        language = TranslationLanguage.ENGLISH
    ),
    EN_YUSUF_ALI(
        id = "en_yusuf_ali",
        translator = "Abdullah Yusuf Ali",
        language = TranslationLanguage.ENGLISH
    ),
    EN_PICKTHALL(
        id = "en_pickthall",
        translator = "Marmaduke Pickthall",
        language = TranslationLanguage.ENGLISH
    ),
    EN_CLEAR_QURAN(
        id = "en_clear_quran",
        translator = "Talal Itani (Clear Qur'an)",
        language = TranslationLanguage.ENGLISH
    ),

    // Urdu
    UR_MAUDUDI(
        id = "ur_maududi",
        translator = "Abul A'ala Maududi",
        language = TranslationLanguage.URDU
    ),
    UR_JALANDHRY(
        id = "ur_jalandhry",
        translator = "Fateh Muhammad Jalandhry",
        language = TranslationLanguage.URDU
    ),

    // Other languages
    ID_INDONESIAN(
        id = "id_indonesian",
        translator = "Kementerian Agama",
        language = TranslationLanguage.INDONESIAN
    ),
    TR_DIYANET(
        id = "tr_diyanet",
        translator = "Diyanet İşleri",
        language = TranslationLanguage.TURKISH
    ),
    FR_HAMIDULLAH(
        id = "fr_hamidullah",
        translator = "Muhammad Hamidullah",
        language = TranslationLanguage.FRENCH
    ),
    BN_BENGALI(
        id = "bn_bengali",
        translator = "Muhiuddin Khan",
        language = TranslationLanguage.BENGALI
    ),
    HI_HINDI(
        id = "hi_hindi",
        translator = "Farooq Khan & Nadwi",
        language = TranslationLanguage.HINDI
    ),
    ES_GARCIA(
        id = "es_garcia",
        translator = "Isa García",
        language = TranslationLanguage.SPANISH
    ),
    RU_KULIEV(
        id = "ru_kuliev",
        translator = "Elmir Kuliev",
        language = TranslationLanguage.RUSSIAN
    ),
    MS_BASMEIH(
        id = "ms_basmeih",
        translator = "Abdullah Muhammad Basmeih",
        language = TranslationLanguage.MALAY
    ),
    DE_BUBENHEIM(
        id = "de_bubenheim",
        translator = "Bubenheim & Elyas",
        language = TranslationLanguage.GERMAN
    );

    /** True when the translation text must be laid out right-to-left. */
    val isRtl: Boolean get() = language.isRtl

    companion object {
        /** The translation a user has if they never picked one — the app's historical default. */
        val DEFAULT = SAHIH_INTERNATIONAL

        /**
         * Resolve a stored id, falling back to [DEFAULT] for null / unknown values so a
         * preference written by a newer build (or a removed translation) never breaks the
         * reader.
         */
        fun fromId(id: String?): QuranTranslation = entries.find { it.id == id } ?: DEFAULT

        /** Catalogue grouped for display: languages in enum order, translations within each. */
        fun byLanguage(): Map<TranslationLanguage, List<QuranTranslation>> =
            entries.groupBy { it.language }
                .toSortedMap(compareBy { it.ordinal })
    }
}
