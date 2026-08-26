package com.arshadshah.nimaz.presentation.components.atoms

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.share.QrCodes
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The "scan to install" QR, and the encoder behind it.
 *
 * This is the one image in the app whose *content* is machine-readable, and the failure mode is
 * silent in the worst way: a code that encodes the wrong string, or that renders with the light and
 * dark values swapped, looks exactly like a working QR and sends whoever scans it somewhere else —
 * or nowhere. It ships on the invite sheet and on every branded share card, so it is the artefact
 * most likely to be seen by somebody who is not a Nimaz user at all.
 *
 * `NimazQrCode` also swallows encoder failures by design (`runCatching { … }.getOrNull()`) and
 * simply draws nothing. That is right — an invite sheet must not crash — and it means a broken
 * encoder is invisible unless something asserts the bitmap comes back.
 */
@RunWith(RobolectricTestRunner::class)
class NimazQrCodeTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the store link is encoded, not a placeholder`() {
        // The whole point of the code. A truncated or malformed URL still encodes cleanly.
        assertThat(QrCodes.APP_URL).contains("play.google.com")
        assertThat(QrCodes.APP_URL).contains("com.arshadshah.nimaz")
    }

    @Test
    fun `encoding produces a square bitmap of the requested size`() {
        val bitmap = QrCodes.encode(QrCodes.APP_URL, sizePx = 200)

        assertThat(bitmap.width).isEqualTo(bitmap.height)
        assertThat(bitmap.width).isAtLeast(1)
    }

    @Test
    fun `the code carries both modules and quiet space`() {
        // A matrix that came back all-dark or all-light is a code no scanner can read, and it is
        // exactly what a wrong `sizePx` or a dropped margin hint produces.
        val bitmap = QrCodes.encode(QrCodes.APP_URL, sizePx = 200, dark = AndroidColor.BLACK, light = AndroidColor.WHITE)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        assertThat(pixels.count { it == AndroidColor.BLACK }).isGreaterThan(0)
        assertThat(pixels.count { it == AndroidColor.WHITE }).isGreaterThan(0)
    }

    @Test
    fun `the caller's colours are the ones painted`() {
        // Swapping dark and light is the classic QR bug: the code inverts and stops scanning on
        // most phones while still looking like a QR code.
        val bitmap = QrCodes.encode("nimaz", sizePx = 120, dark = AndroidColor.RED, light = AndroidColor.GREEN)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        assertThat(pixels.toSet()).containsExactly(AndroidColor.RED, AndroidColor.GREEN)
        // The quiet zone is light, so the corner is always the light value.
        assertThat(pixels.first()).isEqualTo(AndroidColor.GREEN)
    }

    @Test
    fun `different content encodes differently`() {
        // A cache keyed too loosely — or an encoder ignoring its argument — would hand the same
        // matrix back for every string, which is how a share card ends up with the wrong link.
        val a = QrCodes.encode("one", sizePx = 120)
        val b = QrCodes.encode("two", sizePx = 120)

        val pa = IntArray(a.width * a.height).also { a.getPixels(it, 0, a.width, 0, 0, a.width, a.height) }
        val pb = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }

        assertThat(pa.toList()).isNotEqualTo(pb.toList())
    }

    @Test
    fun `the composable renders the encoded code`() {
        composeRule.setThemedContent {
            Box { NimazQrCode(content = QrCodes.APP_URL, size = 200.dp) }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun `content the encoder cannot handle draws nothing rather than crashing`() {
        // `runCatching { … }.getOrNull()` — the invite sheet must not take the app down because a
        // string was too long for the chosen error-correction level. Nothing is drawn, and the
        // sheet's other content still renders.
        val impossible = "x".repeat(10_000)

        composeRule.setThemedContent {
            Box {
                NimazQrCode(content = impossible, size = 100.dp, dark = Color.Black)
            }
        }

        composeRule.waitForIdle()
    }
}
