package com.arshadshah.nimaz.domain.search

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * Holds this folding to the one the index was built with (#330, nimaz-data#7).
 *
 * The interesting failure here is not an exception. If the app folds a query even
 * slightly differently from the way the data repository folded the text, every query
 * matches nothing and *no test fails* — each side is perfectly self-consistent. So the
 * agreement is a generated file, `fold-fixtures.json`, written by `nz search fixtures`
 * and copied here by `nz app sync`, and both sides assert against it.
 *
 * A failure here means one of two things: this file drifted from `arabic.py`, or the
 * fixtures were regenerated and this implementation was not updated to match. Either
 * way, search is broken on every device that has the index.
 */
class ArabicSearchNormaliserTest {

    // kotlinx.serialization rather than org.json: this is a JVM unit test, and Android's
    // org.json is a stub on that classpath — every getter quietly returns a default, so
    // the assertions would pass or NPE on nothing rather than on the fixtures.
    private val fixtures: JsonObject by lazy {
        val stream = javaClass.classLoader!!.getResourceAsStream("search/fold-fixtures.json")
        requireNotNull(stream) {
            "search/fold-fixtures.json is missing — run `nz app sync --app-repo <this repo>`"
        }
        Json.parseToJsonElement(stream.bufferedReader().use { it.readText() }).jsonObject
    }

    @Test
    fun `the folding is the one the shipped index was built with`() {
        assertThat(fixtures.getValue("fold_version").jsonPrimitive.int)
            .isEqualTo(ArabicSearchNormaliser.FOLD_VERSION)
    }

    @Test
    fun `every fixture pair folds the way the data repository folded it`() {
        val pairs = fixtures.getValue("pairs").jsonArray
        assertThat(pairs).isNotEmpty()
        for (element in pairs) {
            val pair = element.jsonObject
            assertThat(ArabicSearchNormaliser.fold(pair.string("raw")))
                .isEqualTo(pair.string("folded"))
        }
    }

    @Test
    fun `every fixture tokenises the way the indexed text was tokenised`() {
        val cases = fixtures.getValue("tokenised").jsonArray
        assertThat(cases).isNotEmpty()
        for (element in cases) {
            val case = element.jsonObject
            val expected = case.getValue("tokens").jsonArray.map { it.jsonPrimitive.content }
            assertThat(ArabicSearchNormaliser.tokens(case.string("raw"))).isEqualTo(expected)
        }
    }

    // --- the bug itself -------------------------------------------------------------

    @Test
    fun `what a keyboard produces folds onto what the corpus stores`() {
        // Twelve codepoints against six. This is why `LIKE '%الرحمن%'` returned 0 rows
        // for every Arabic query the app has ever run.
        val stored = "ٱلرَّحْمَٰنِ"
        val typed = "الرحمن"
        assertThat(stored).doesNotContain(typed)
        assertThat(ArabicSearchNormaliser.fold(stored))
            .isEqualTo(ArabicSearchNormaliser.fold(typed))
    }

    @Test
    fun `alef wasla folds although it is a letter rather than a diacritic`() {
        // 77% of ayahs start with U+0671, so stripping marks alone fixed nothing.
        assertThat(ArabicSearchNormaliser.fold("ٱلله")).isEqualTo("الله")
    }

    @Test
    fun `folding is idempotent`() {
        val once = ArabicSearchNormaliser.fold("ٱللَّهُ")
        assertThat(ArabicSearchNormaliser.fold(once)).isEqualTo(once)
    }

    @Test
    fun `an empty or symbol-only query produces no match expression`() {
        // MATCH throws on an empty expression rather than matching nothing, and a search
        // box holds exactly this halfway through being typed.
        assertThat(ArabicSearchNormaliser.matchExpression("")).isEmpty()
        assertThat(ArabicSearchNormaliser.matchExpression("  ...  ")).isEmpty()
        assertThat(ArabicSearchNormaliser.matchExpression(null)).isEmpty()
    }

    @Test
    fun `several words are a phrase, so the phrase bonus still means something`() {
        // SearchLibraryUseCase scores a whole-phrase hit at 100 and each word at 1. If
        // the phrase pass were OR-ed it would match everything the word passes match and
        // hand all of them the phrase score, which is a ranking of nothing.
        assertThat(ArabicSearchNormaliser.matchExpression("patience during hardship"))
            .isEqualTo("\"patience during hardship\"")
        assertThat(ArabicSearchNormaliser.matchExpression("patience"))
            .isEqualTo("patience*")
    }

    @Test
    fun `a prefix expression is what lets rahman reach ar-rahmani`() {
        // There is no stemmer. The transliteration column stores "Ar-Rahmani"; a whole-word
        // match for "rahman" would miss it.
        val transliterated = ArabicSearchNormaliser.tokens("Ar-Raḥmāni")
        assertThat(transliterated).containsExactly("ar", "rahmani").inOrder()
        assertThat(ArabicSearchNormaliser.matchExpression("rahman")).isEqualTo("rahman*")
    }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    @Test
    fun `case is folded independently of the device locale`() {
        // `toLowerCase()` with a Turkish default locale turns I into ı, which would fold a
        // query differently on a Turkish phone than the index was built.
        val default = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("tr", "TR"))
            assertThat(ArabicSearchNormaliser.fold("PATIENCE")).isEqualTo("patience")
        } finally {
            java.util.Locale.setDefault(default)
        }
    }
}
