package com.arshadshah.nimaz.core.share

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The branded share image when the content carries a transliteration.
 *
 * `ShareCardRendererTest` covers the zakat figure and a plain scripture card. What neither reaches
 * is the **transliteration block**, which is drawn muted-italic between the Arabic and the body and
 * is measured into the card's height like every other block. A card that measured it and did not
 * draw it leaves a gap; one that drew it without measuring overlaps the translation. Both are
 * silent — the image renders either way, and it is a *shared* image, so the first person to see the
 * mistake is whoever the user sent it to.
 *
 * Dua and ayah shares are the two content types that carry one, so this is not an edge case.
 */
@RunWith(RobolectricTestRunner::class)
class ShareCardRendererTransliterationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun render(card: ShareCard) = ShareCardRenderer.renderToCache(context, card)

    private fun heightOf(card: ShareCard): Int {
        val file = requireNotNull(render(card))
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outHeight
    }

    private fun scripture(transliteration: String?) = ShareCard(
        eyebrow = "Quran",
        arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        transliteration = transliteration,
        body = "In the name of God, the Lord of Mercy, the Giver of Mercy.",
        attribution = "Surah Al-Fatihah · Ayah 1",
    )

    @Test
    fun `a transliteration is drawn, and the card grows to hold it`() {
        val withIt = heightOf(scripture("Bismillahi r-rahmani r-rahim"))
        val without = heightOf(scripture(null))

        assertThat(withIt).isGreaterThan(without)
    }

    @Test
    fun `a blank transliteration is treated as none`() {
        // `trim().takeIf { it.isNotEmpty() }` — content arrives from a fetched artifact, and an
        // empty column would otherwise reserve a block's worth of height for nothing.
        assertThat(heightOf(scripture("   "))).isEqualTo(heightOf(scripture(null)))
    }

    @Test
    fun `a card with no arabic and no body still renders`() {
        // The invite card's shape: an eyebrow, a line of body and an attribution, with the Arabic
        // slot deliberately empty. Every block is independently optional, and the height has to
        // come out sane with most of them absent.
        val file = render(
            ShareCard(
                eyebrow = "Nimaz",
                arabic = null,
                body = null,
                attribution = "nimaz.app",
            )
        )

        assertThat(file).isNotNull()
        assertThat(file!!.length()).isGreaterThan(0L)
    }
}
