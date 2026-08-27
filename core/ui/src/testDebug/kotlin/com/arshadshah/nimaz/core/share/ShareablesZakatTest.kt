package com.arshadshah.nimaz.core.share

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
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
        basis = "Gold",
        aboveNisab = true,
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
    fun `the due figure is the headline, not one line of five`() {
        val card = requireNotNull(subject().card)
        // The five figures used to be one newline-joined string in `body`, drawn centred at
        // prose size — nothing on the card said which was the answer and which the working.
        assertThat(card.body).isNull()
        val headline = requireNotNull(card.headline)
        assertThat(headline.value).isEqualTo("€1,284.50")
        assertThat(headline.label).isEqualTo(context.getString(R.string.share_zakat_due))
        assertThat(headline.badge)
            .isEqualTo(context.getString(R.string.zakat_status_above_nisab))
        assertThat(headline.caption)
            .isEqualTo(context.getString(R.string.zakat_rate_subtitle))
        assertThat(headline.muted).isFalse()
    }

    @Test
    fun `the working is a ledger, and every figure is still on it`() {
        val rows = requireNotNull(subject().card).rows
        assertThat(rows.map { it.value })
            .containsExactly("€52,180.00", "€800.00", "€51,380.00", "€5,687.10")
            .inOrder()
        // Tone is what tells a reader which figures went in and which came off — a ledger of
        // four identical grey lines is the flat block this replaced.
        assertThat(rows.map { it.tone }).containsExactly(
            ShareCardRowTone.POSITIVE,
            ShareCardRowTone.NEGATIVE,
            ShareCardRowTone.TOTAL,
            ShareCardRowTone.NEUTRAL,
        ).inOrder()
        // The nisab row names its basis: the threshold alone does not say whether it is the
        // gold one or the silver one, and they differ by roughly an order of magnitude.
        assertThat(rows.last().label).contains("Gold")
        assertThat(rows.last().label).contains(context.getString(R.string.share_zakat_nisab))
    }

    @Test
    fun `below nisab the card says so instead of restating the rate`() {
        val card = requireNotNull(
            Shareables.zakat(
                context = context,
                due = "€0.00",
                assets = "€1,000.00",
                deducted = "€0.00",
                net = "€1,000.00",
                nisab = "€5,687.10",
                yearLabel = "1447",
                basis = "Silver",
                aboveNisab = false,
            ).card
        )
        val headline = requireNotNull(card.headline)
        assertThat(headline.caption)
            .isEqualTo(context.getString(R.string.zakat_below_nisab_subtitle))
        assertThat(headline.badge)
            .isEqualTo(context.getString(R.string.zakat_status_below_nisab))
        // A full-strength €0.00 overstates a number that is not owed.
        assertThat(headline.muted).isTrue()
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
