package com.arshadshah.nimaz.core.share

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.Hadith
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Every optional field of a share arriving **blank** rather than absent.
 *
 * `ShareablesTest` covers the null case for each of them. Blank is the *other* shape, and it is the
 * one the app actually meets: the content database is a fetched artifact, and a column that was
 * never populated comes back as `""` rather than as `NULL`. Every one of these builders therefore
 * writes `?.takeIf { it.isNotBlank() }` instead of a plain null check — and a single one reduced to
 * `!= null` puts an empty paragraph, a bare em-dash or a trailing separator into a message the user
 * has already sent before anyone notices.
 *
 * The card is checked alongside the text, because the branded image reads from the same fields and
 * an empty Arabic slot is drawn large in Amiri.
 */
@RunWith(RobolectricTestRunner::class)
class ShareablesBlankFieldsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun ayah(translation: String?, transliteration: String?) = Ayah(
        id = 1,
        surahNumber = 2,
        ayahNumber = 255,
        textArabic = "اللَّهُ لَا إِلَٰهَ",
        textSimple = "allah",
        juzNumber = 3,
        hizbNumber = 5,
        rubNumber = 20,
        pageNumber = 42,
        sajdaType = null,
        sajdaNumber = null,
        translation = translation,
        transliteration = transliteration,
    )

    private fun hadith(narrator: String?, reference: String?) = Hadith(
        id = "bukhari_1",
        bookId = "bukhari",
        chapterId = "1",
        hadithNumber = 1,
        hadithNumberInBook = 1,
        textArabic = "إنما الأعمال",
        textEnglish = "Actions are but by intention",
        narratorChain = null,
        narratorName = narrator,
        grade = null,
        gradeArabic = null,
        reference = reference,
    )

    private fun dua(transliteration: String?, reference: String?, title: String) = Dua(
        id = "d1",
        categoryId = "morning",
        titleArabic = "دعاء",
        titleEnglish = title,
        textArabic = "رَبَّنَا",
        textTransliteration = transliteration,
        textEnglish = "Our Lord",
        reference = reference,
        occasion = null,
        benefits = null,
        repeatCount = null,
        audioUrl = null,
        displayOrder = 0,
    )

    @Test
    fun `a blank translation is dropped from the ayah's text and its card`() {
        val shareable = Shareables.ayah(context, ayah(translation = "   ", transliteration = " "))

        assertThat(shareable.plainText).doesNotContain("\n\n\n")
        assertThat(shareable.card?.body).isNull()
        assertThat(shareable.card?.transliteration).isNull()
    }

    @Test
    fun `a blank hadith reference falls through to the next candidate`() {
        // Three-deep elvis, and the middle candidate arriving blank is what a collection with an
        // unpopulated reference column looks like. Reading it as present labels the share with
        // nothing at all.
        val text = Shareables.hadith(context, hadith(narrator = "  ", reference = "  ")).plainText

        assertThat(text).contains("1")
        assertThat(text).doesNotContain("\n\n\n")
    }

    @Test
    fun `a blank dua reference leaves no source line and no trailing separator`() {
        val shareable = Shareables.dua(
            context,
            dua(transliteration = "  ", reference = "  ", title = "A supplication"),
        )

        assertThat(shareable.card?.attribution).isEqualTo("A supplication")
        assertThat(shareable.card?.transliteration).isNull()
    }

    @Test
    fun `a dua with a blank title still gets an attribution`() {
        // `ifBlank { dua.titleEnglish }` — the last guard in the chain. With every candidate blank
        // the card would otherwise carry an empty attribution line under a gold rule.
        val shareable = Shareables.dua(
            context,
            dua(transliteration = null, reference = "  ", title = "  "),
        )

        assertThat(shareable.card?.attribution).isEqualTo("  ")
    }

    @Test
    fun `an empty dua transliteration is skipped as readily as a null one`() {
        // This one is `isNullOrEmpty`, not `isNullOrBlank` — the odd one out in the file, and the
        // distinction only shows up on a value that is empty rather than whitespace.
        val text = Shareables.dua(
            context,
            dua(transliteration = "", reference = null, title = "A supplication"),
        ).plainText

        assertThat(text).doesNotContain("\n\n\n")
    }
}
