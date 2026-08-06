package com.arshadshah.nimaz.core.share

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The figure-and-ledger half of the share card, rendered for real.
 *
 * `NATIVE` graphics rather than Robolectric's legacy no-op canvas, because everything asserted
 * here is a *measurement*: the renderer walks its content twice — once with a null canvas to
 * total the height, once with the real one to paint — and a legacy shadow would let the two
 * disagree silently. That dual walk is the only thing standing between a taller card and one
 * whose last rows are cropped off the bottom of the PNG.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareCardRendererTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun card(
        headline: ShareCardFigure? = null,
        rows: List<ShareCardRow> = emptyList(),
    ) = ShareCard(
        eyebrow = "Zakat",
        arabic = null,
        body = null,
        attribution = "Lunar year 1447",
        headline = headline,
        rows = rows,
    )

    private val figure = ShareCardFigure(
        label = "Zakat due",
        value = "€1,284.50",
        caption = "2.5% of eligible wealth",
        badge = "Above nisab",
    )

    private val ledger = listOf(
        ShareCardRow("Assets", "€52,180.00", ShareCardRowTone.POSITIVE),
        ShareCardRow("Deducted", "€800.00", ShareCardRowTone.NEGATIVE),
        ShareCardRow("Net zakatable", "€51,380.00", ShareCardRowTone.TOTAL),
        ShareCardRow("Nisab threshold · Gold", "€5,687.10"),
    )

    private fun heightOf(card: ShareCard): Int {
        val file = ShareCardRenderer.renderToCache(context, card)
        return BitmapFactory.decodeFile(file.absolutePath).height
    }

    @Test
    fun `the canvas grows to fit the figure and every ledger row`() {
        val bare = heightOf(card())
        val withFigure = heightOf(card(headline = figure))
        val withLedger = heightOf(card(headline = figure, rows = ledger))

        assertThat(withFigure).isGreaterThan(bare)
        assertThat(withLedger).isGreaterThan(withFigure)
        // Four rows is four rows' worth of height, not one: a measuring walk that priced the
        // ledger as a single block would crop three of them.
        val oneRow = heightOf(card(headline = figure, rows = ledger.take(1)))
        assertThat(withLedger - withFigure).isGreaterThan(3 * (oneRow - withFigure))
    }

    @Test
    fun `two calculations do not share one filename`() {
        // The cache key used to be built from the eyebrow and the attribution alone. On a zakat
        // card those are "Zakat" and the lunar year, so every calculation made in the same year
        // hashed to the same path — and a share is a file handed to another app by URI.
        val first = ShareCardRenderer.renderToCache(context, card(headline = figure, rows = ledger))
        val second = ShareCardRenderer.renderToCache(
            context,
            card(headline = figure.copy(value = "€99.00"), rows = ledger),
        )

        assertThat(first.name).isNotEqualTo(second.name)
    }

    @Test
    fun `a scripture card still renders without a figure`() {
        val height = heightOf(
            ShareCard(
                eyebrow = "Quran",
                arabic = "بِسْمِ ٱللَّهِ",
                body = "In the name of Allah",
                attribution = "Surah Al-Fatihah · Ayah 1",
            )
        )

        assertThat(height).isGreaterThan(0)
    }
}
