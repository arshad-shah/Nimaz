package com.arshadshah.nimaz.core.share

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Turns a URL/string into a QR-code [Bitmap] using the pure-Java ZXing encoder.
 * Used both on the branded share card ([ShareCardRenderer]) and in-app (the invite
 * QR) so a recipient can scan straight to the app.
 */
object QrCodes {

    /** The Play Store link encoded on share cards and the in-app invite QR. */
    const val APP_URL =
        "https://play.google.com/store/apps/details?id=com.arshadshah.nimaz"

    /**
     * Encode [content] as a square QR [Bitmap] of [sizePx] pixels. [dark] is the
     * module colour, [light] the background (ARGB ints). Uses error-correction
     * level M so a small logo/quiet-zone crop still scans.
     */
    fun encode(
        content: String,
        sizePx: Int,
        dark: Int = Color.BLACK,
        light: Int = Color.WHITE,
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val w = matrix.width
        val h = matrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (matrix[x, y]) dark else light
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
}
