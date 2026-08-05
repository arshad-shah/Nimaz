package com.arshadshah.nimaz.core.share

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The zakat share, asserted against real string resources.
 *
 * Robolectric rather than a mocked `Context` on purpose: what this builder is *for* is that
 * every label comes out of `R.string`, so a test that stubs `getString` would pass while the
 * builder assembled English by hand.
 */
@RunWith(RobolectricTestRunner::class)
class ShareablesZakatTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun subject() = Shareables.zakat(
        context = context,
        due = "€1,284.50",
        assets = "€52,180.00",
        deducted = "€800.00",
        net = "€51,380.00",
        nisab = "€5,687.10",
        yearLabel = "1447",
    )

    @Test
    fun `the card is a receipt, not scripture`() {
        val card = requireNotNull(subject().card)
        assertThat(card.eyebrow).isEqualTo(context.getString(R.string.share_eyebrow_zakat))
        // The Arabic slot is drawn large in Amiri. A currency breakdown does not belong in it.
        assertThat(card.arabic).isNull()
        assertThat(card.transliteration).isNull()
        assertThat(card.attribution).contains("1447")
    }

    @Test
    fun `the card body carries the due figure and the whole working`() {
        val body = requireNotNull(subject().card?.body)
        assertThat(body).contains("€1,284.50")
        assertThat(body).contains("€52,180.00")
        assertThat(body).contains("€800.00")
        assertThat(body).contains("€51,380.00")
        assertThat(body).contains("€5,687.10")
    }

    @Test
    fun `the plain text carries the same figures for text-only targets`() {
        // shareBranded falls back to plainText when rendering fails, so a figure present only
        // on the card is a figure that silently disappears on some devices.
        val plain = subject().plainText
        for (figure in listOf(
            "€1,284.50", "€52,180.00", "€800.00", "€51,380.00", "€5,687.10"
        )) {
            assertThat(plain).contains(figure)
        }
    }

    @Test
    fun `every label is resolved from resources, not written here`() {
        val plain = subject().plainText
        for (label in listOf(
            R.string.share_zakat_due,
            R.string.share_zakat_assets,
            R.string.share_zakat_deducted,
            R.string.share_zakat_net,
            R.string.share_zakat_nisab,
        )) {
            assertThat(plain).contains(context.getString(label))
        }
    }

    @Test
    fun `the branding footer is appended like every other builder`() {
        assertThat(subject().plainText)
            .endsWith(context.getString(R.string.share_text_footer))
    }

    @Test
    fun `no subject line is set`() {
        // A zakat breakdown is not an email; the share sheet's own title is enough.
        assertThat(subject().subject).isNull()
    }
}
