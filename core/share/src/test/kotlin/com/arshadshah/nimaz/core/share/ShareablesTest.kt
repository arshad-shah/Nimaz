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
 * Every share body the app can produce, assembled against real string resources.
 *
 * `Shareables` exists so no screen ever writes `"$arabic\n\n$translation\n\n— …"` by hand again,
 * which makes it the single place where a share can go wrong for *every* content type at once: a
 * missing attribution line, an optional field rendering as a blank paragraph, a null reference
 * falling through to the wrong fallback. None of that throws — it just sends somebody a message
 * with a hole in it.
 *
 * Robolectric rather than a mocked `Context`, for the reason `ShareablesZakatTest` states: what
 * these builders are *for* is that every label comes out of `R.string`, so a test that stubbed
 * `getString` would pass while the builder assembled English by hand.
 */
@RunWith(RobolectricTestRunner::class)
class ShareablesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun ayah(
        translation: String? = "In the name of Allah",
        transliteration: String? = "Bismillah",
    ) = Ayah(
        id = 1,
        surahNumber = 1,
        ayahNumber = 1,
        textArabic = "بِسْمِ اللَّهِ",
        textSimple = "bismillah",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = translation,
        transliteration = transliteration,
    )

    private fun hadith(
        narrator: String? = "Umar ibn al-Khattab",
        reference: String? = "Sahih al-Bukhari 1",
    ) = Hadith(
        id = "bukhari_1",
        bookId = "bukhari",
        chapterId = "1",
        hadithNumber = 1,
        hadithNumberInBook = 1,
        textArabic = "إنما الأعمال بالنيات",
        textEnglish = "Actions are but by intention",
        narratorChain = null,
        narratorName = narrator,
        grade = null,
        gradeArabic = null,
        reference = reference,
    )

    private fun dua(
        transliteration: String? = "Rabbana atina",
        reference: String? = "Quran 2:201",
    ) = Dua(
        id = "d1",
        categoryId = "morning",
        titleArabic = "دعاء",
        titleEnglish = "A comprehensive supplication",
        textArabic = "رَبَّنَا آتِنَا",
        textTransliteration = transliteration,
        textEnglish = "Our Lord, give us good",
        reference = reference,
        occasion = null,
        benefits = null,
        repeatCount = null,
        audioUrl = null,
        displayOrder = 0,
    )

    // ---- ayah ----

    @Test
    fun `an ayah share carries the arabic, the translation and the reference`() {
        val text = Shareables.ayah(context, ayah(), surahName = "Al-Fatihah").plainText

        assertThat(text).contains("بِسْمِ اللَّهِ")
        assertThat(text).contains("In the name of Allah")
        assertThat(text).contains("Al-Fatihah")
        // Every body ends with the localized attribution — the one line that says where this came
        // from, and the reason `appendBranding` is not a call site's job.
        assertThat(text).endsWith(context.getString(com.arshadshah.nimaz.core.ui.R.string.share_text_footer))
    }

    @Test
    fun `an ayah with no translation does not leave a blank paragraph`() {
        val text = Shareables.ayah(context, ayah(translation = null)).plainText

        // The null arm. Appending unconditionally gives "arabic\n\n\n\n— ref", which reads as a
        // formatting bug in whatever app receives it.
        assertThat(text).doesNotContain("\n\n\n")
    }

    @Test
    fun `a blank surah name falls back to the surah number`() {
        // `takeIf { isNotBlank() }` — a whitespace name is not a name. Reading this as a plain
        // null check labels the share "Surah  · Ayah 1".
        val text = Shareables.ayah(context, ayah(), surahName = "   ").plainText

        assertThat(text).contains("1")
    }

    @Test
    fun `an ayah card carries the transliteration only when there is one`() {
        assertThat(Shareables.ayah(context, ayah()).card?.transliteration).isEqualTo("Bismillah")
        assertThat(Shareables.ayah(context, ayah(transliteration = "  ")).card?.transliteration)
            .isNull()
    }

    // ---- favourite ----

    @Test
    fun `a favourite share reads as surah then verse`() {
        val shareable = Shareables.favorite(
            context = context,
            surahName = "Al-Baqarah",
            verseLabel = "Ayah 255",
            arabicText = "اللَّهُ لَا إِلَٰهَ",
        )

        assertThat(shareable.plainText).startsWith("Al-Baqarah · Ayah 255")
        assertThat(shareable.card?.arabic).isEqualTo("اللَّهُ لَا إِلَٰهَ")
        assertThat(shareable.card?.attribution).isEqualTo("Al-Baqarah · Ayah 255")
    }

    @Test
    fun `a favourite with no arabic gets no card at all`() {
        // A card whose Arabic slot is empty is a branded image of a blank space, so the builder
        // drops to the text path instead of rendering one.
        val shareable = Shareables.favorite(context, "Al-Baqarah", "Ayah 255", arabicText = null)

        assertThat(shareable.card).isNull()
        assertThat(shareable.plainText).contains("Al-Baqarah · Ayah 255")
    }

    @Test
    fun `a favourite with blank arabic is treated as having none`() {
        assertThat(Shareables.favorite(context, "Al-Baqarah", "Ayah 255", "   ").card).isNull()
    }

    // ---- hadith ----

    @Test
    fun `a hadith share carries both texts, the narrator and the reference`() {
        val text = Shareables.hadith(context, hadith()).plainText

        assertThat(text).contains("إنما الأعمال بالنيات")
        assertThat(text).contains("Actions are but by intention")
        assertThat(text).contains("Umar ibn al-Khattab")
        assertThat(text).contains("Sahih al-Bukhari 1")
    }

    @Test
    fun `an explicit source label wins over the hadith's own reference`() {
        val text = Shareables.hadith(context, hadith(), sourceLabel = "Riyad as-Salihin 1").plainText

        assertThat(text).contains("Riyad as-Salihin 1")
        assertThat(text).doesNotContain("Sahih al-Bukhari 1")
    }

    @Test
    fun `a hadith with neither label falls back to its number`() {
        // Three-deep elvis: label, then the hadith's own reference, then the number. The last arm
        // is the one that only runs for content that arrived without a reference at all.
        val text = Shareables.hadith(context, hadith(reference = null), sourceLabel = "  ").plainText

        assertThat(text).contains("1")
    }

    @Test
    fun `a hadith with no narrator omits the narration line`() {
        val text = Shareables.hadith(context, hadith(narrator = null)).plainText

        assertThat(text).doesNotContain("\n\n\n")
    }

    // ---- dua ----

    @Test
    fun `a dua share carries its title, arabic, transliteration, english and source`() {
        val text = Shareables.dua(context, dua()).plainText

        assertThat(text).startsWith("A comprehensive supplication")
        assertThat(text).contains("رَبَّنَا آتِنَا")
        assertThat(text).contains("Rabbana atina")
        assertThat(text).contains("Our Lord, give us good")
        assertThat(text).contains("Quran 2:201")
    }

    @Test
    fun `a dua with no transliteration skips that block`() {
        val text = Shareables.dua(context, dua(transliteration = null)).plainText

        assertThat(text).doesNotContain("Rabbana")
    }

    @Test
    fun `a dua with no reference gets its title alone as attribution`() {
        // The `joinToString(" · ")` over a list that is now one long — a `listOf` instead of
        // `listOfNotNull` leaves a trailing separator on the card.
        val card = Shareables.dua(context, dua(reference = null)).card

        assertThat(card?.attribution).isEqualTo("A comprehensive supplication")
    }

    @Test
    fun `a dua with a reference joins it to the title`() {
        assertThat(Shareables.dua(context, dua()).card?.attribution)
            .isEqualTo("A comprehensive supplication · Quran 2:201")
    }

    // ---- bookmark ----

    @Test
    fun `a bookmark share is text only`() {
        val shareable = Shareables.bookmark(
            context = context,
            title = "Ayat al-Kursi",
            arabicText = "اللَّهُ",
            note = "Read after every prayer",
        )

        assertThat(shareable.card).isNull()
        assertThat(shareable.plainText).contains("Ayat al-Kursi")
        assertThat(shareable.plainText).contains("اللَّهُ")
        assertThat(shareable.plainText).contains("Read after every prayer")
    }

    @Test
    fun `a bookmark with neither arabic nor note is just its title and the footer`() {
        val text = Shareables.bookmark(context, title = "Ayat al-Kursi").plainText

        assertThat(text.substringBefore("\n\n")).isEqualTo("Ayat al-Kursi")
    }

    @Test
    fun `blank optional fields on a bookmark are dropped, not appended`() {
        val text = Shareables.bookmark(context, "Ayat al-Kursi", arabicText = " ", note = " ")

        assertThat(text.plainText.substringBefore("\n\n")).isEqualTo("Ayat al-Kursi")
    }

    // ---- app invite and passthrough ----

    @Test
    fun `the app invite keeps a card and a tappable message`() {
        // The invite is the one content share that deliberately keeps its text alongside the
        // image: the store link is the whole point of an invite, and an image is not tappable.
        val shareable = Shareables.appInvite(context)

        assertThat(shareable.plainText).isNotEmpty()
        assertThat(shareable.card).isNotNull()
        assertThat(shareable.card?.arabic).isNull()
    }

    @Test
    fun `a text passthrough carries its subject`() {
        val shareable = Shareables.text("Prayer times for June", subject = "Nimaz export")

        assertThat(shareable.plainText).isEqualTo("Prayer times for June")
        assertThat(shareable.subject).isEqualTo("Nimaz export")
        assertThat(shareable.card).isNull()
    }

    @Test
    fun `a text passthrough may carry no subject`() {
        assertThat(Shareables.text("Just the body").subject).isNull()
    }
}
