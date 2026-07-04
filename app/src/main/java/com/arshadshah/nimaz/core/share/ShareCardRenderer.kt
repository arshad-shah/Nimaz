package com.arshadshah.nimaz.core.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a [ShareCard] into a **branded Nimaz share image** — a teal-and-gold card
 * with the Nimaz wordmark, an app-icon monogram, the Amiri Arabic, its translation
 * and an attribution line. Deliberately mirrors the visual language of the Tafseer /
 * Prayer-times PDF exporters (same teal `14B8A6`/gold `EAB308`, Amiri, "Nimaz"
 * wordmark) so image, PDF and text share all read as one system.
 *
 * The PNG is written to the app's `exports/` cache dir — the same FileProvider-shared
 * location the PDF exporters use — and handed to [ContentShareManager.shareFile].
 * Rendering is CPU/allocation heavy; call it off the main thread (see
 * [ContentShareManager.shareBranded]).
 */
object ShareCardRenderer {

    private const val W = 1080
    private const val OUTER = 40f
    private const val CARD_PAD = 64f
    private const val TILE = 96f
    private const val CORNER = 40f

    // Text length caps — the full content always survives in the text fallback.
    private const val MAX_ARABIC = 700
    private const val MAX_BODY = 700
    private const val MAX_TRANSLIT = 400

    private val TEAL = 0xFF14B8A6.toInt()
    private val TEAL_DARK = 0xFF0F766E.toInt()
    private val TEAL_800 = 0xFF115E59.toInt()
    private val TEAL_950 = 0xFF042F2E.toInt()
    private val INK = 0xFF1C1917.toInt()
    private val MUTED = 0xFF78716C.toInt()
    private val GOLD = 0xFFEAB308.toInt()
    private val GOLD_INK = 0xFF3A2A00.toInt()
    private val LINE = 0xFFE7E5E4.toInt()

    fun renderToCache(context: Context, card: ShareCard): File {
        val bitmap = Renderer(context, card).render()
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Nimaz_Share_${cardKey(card)}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    private fun cardKey(card: ShareCard): String =
        Integer.toHexString(
            listOf(card.eyebrow, card.attribution, card.arabic, card.body, card.transliteration)
                .joinToString("|").hashCode()
        )

    /**
     * Lays out every text block once, then draws it. [draw] runs the same top-down
     * walk twice — once with a null canvas to measure the total height, once with the
     * real canvas — so measurement and drawing can never drift apart.
     */
    private class Renderer(private val context: Context, private val card: ShareCard) {

        private val contentWidth = (W - 2 * OUTER - 2 * CARD_PAD).toInt()

        private val amiri =
            ResourcesCompat.getFont(context, R.font.amiri_regular) ?: Typeface.SERIF
        private val heading =
            ResourcesCompat.getFont(context, R.font.outfit_variable) ?: Typeface.DEFAULT
        private val bodyFont =
            ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_variable) ?: Typeface.DEFAULT
        private val bold = Typeface.create(heading, Typeface.BOLD)

        private val arabicLayout = card.arabic?.trim()?.takeIf { it.isNotEmpty() }?.let {
            layout(ellipsize(it, MAX_ARABIC), textPaint(amiri, 60f, INK), Layout.Alignment.ALIGN_CENTER, 14f)
        }
        private val translitLayout = card.transliteration?.trim()?.takeIf { it.isNotEmpty() }?.let {
            layout(
                ellipsize(it, MAX_TRANSLIT),
                textPaint(Typeface.create(bodyFont, Typeface.ITALIC), 30f, MUTED),
                Layout.Alignment.ALIGN_CENTER, 6f
            )
        }
        private val bodyLayout = card.body?.trim()?.takeIf { it.isNotEmpty() }?.let {
            layout(ellipsize(it, MAX_BODY), textPaint(bodyFont, 34f, INK), Layout.Alignment.ALIGN_CENTER, 8f)
        }
        private val attrLayout =
            layout(card.attribution, textPaint(bold, 32f, TEAL_DARK), Layout.Alignment.ALIGN_CENTER, 4f)
        private val footerLayout =
            layout(
                context.getString(R.string.share_card_qr_caption),
                textPaint(bodyFont, 26f, MUTED), Layout.Alignment.ALIGN_CENTER, 2f
            )

        // "Scan to install" QR, drawn in the footer in brand teal on white.
        private val qrBitmap: Bitmap? = runCatching {
            QrCodes.encode(QrCodes.APP_URL, 200, dark = TEAL_800, light = Color.WHITE)
        }.onFailure { CrashReporter.recordException(it) }.getOrNull()

        fun render(): Bitmap {
            val height = draw(null).toInt()
            val bitmap = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            return bitmap
        }

        /** Returns the total canvas height; only paints when [canvas] is non-null. */
        private fun draw(canvas: Canvas?): Float {
            val left = OUTER + CARD_PAD
            val right = W - OUTER - CARD_PAD

            // We need the card height to paint the background + card first, but height
            // is only known after walking the content. So compute the content walk
            // height, then (when drawing) paint background/card, then re-walk to draw.
            val contentTop = OUTER + CARD_PAD

            // ── Header: monogram tile + wordmark, gold eyebrow pill on the right ──
            if (canvas != null) {
                val cardBottom = totalHeight() - OUTER
                // Deep-teal gradient backdrop.
                canvas.drawRect(
                    0f, 0f, W.toFloat(), totalHeight(),
                    Paint().apply {
                        isAntiAlias = true
                        shader = LinearGradient(
                            0f, 0f, W.toFloat(), totalHeight(),
                            TEAL_800, TEAL_950, Shader.TileMode.CLAMP
                        )
                    }
                )
                // White card.
                canvas.drawRoundRect(
                    OUTER, OUTER, W - OUTER, cardBottom, CORNER, CORNER, fill(Color.WHITE)
                )
                // Monogram tile.
                canvas.drawRoundRect(
                    left, contentTop, left + TILE, contentTop + TILE, 24f, 24f, fill(TEAL_800)
                )
                runCatching {
                    val logo = context.packageManager.getApplicationIcon(context.packageName)
                    val pad = 12
                    logo.setBounds(
                        (left + pad).toInt(), (contentTop + pad).toInt(),
                        (left + TILE - pad).toInt(), (contentTop + TILE - pad).toInt()
                    )
                    logo.draw(canvas)
                }.onFailure { CrashReporter.recordException(it) }
                // Wordmark + tagline.
                canvas.drawText(
                    "Nimaz", left + TILE + 28f, contentTop + 44f,
                    Paint().apply { isAntiAlias = true; color = INK; typeface = bold; textSize = 50f }
                )
                canvas.drawText(
                    context.getString(R.string.share_card_tagline).uppercase(),
                    left + TILE + 30f, contentTop + 78f,
                    Paint().apply {
                        isAntiAlias = true; color = MUTED; typeface = bodyFont
                        textSize = 22f; letterSpacing = 0.12f
                    }
                )
                // Gold eyebrow pill (right-aligned, vertically centred on the tile).
                drawEyebrowPill(canvas, card.eyebrow.uppercase(), right, contentTop + TILE / 2f)
            }
            var y = contentTop + TILE + 44f

            // ── Gold rule ──
            if (canvas != null) {
                canvas.drawRect(left, y, right, y + 4f, fill(GOLD))
            }
            y += 4f + 44f

            // ── Arabic ──
            arabicLayout?.let { y = drawBlock(canvas, it, left, y) + 36f }
            // ── Transliteration ──
            translitLayout?.let { y = drawBlock(canvas, it, left, y) + 28f }
            // ── Body / translation ──
            bodyLayout?.let { y = drawBlock(canvas, it, left, y) + 40f }

            // ── Faint divider ──
            if (canvas != null) {
                canvas.drawRect(left, y, right, y + 2f, fill(LINE))
            }
            y += 2f + 28f

            // ── Attribution ──
            y = drawBlock(canvas, attrLayout, left, y) + 28f

            // ── "Scan to install" QR (centred) + caption ──
            qrBitmap?.let { qr ->
                if (canvas != null) {
                    canvas.drawBitmap(qr, left + (contentWidth - qr.width) / 2f, y, null)
                }
                y += qr.height + 14f
            }
            y = drawBlock(canvas, footerLayout, left, y)

            return y + CARD_PAD
        }

        /** Draws (or just advances past) a text block, returning the new y cursor. */
        private fun drawBlock(canvas: Canvas?, block: StaticLayout, left: Float, y: Float): Float {
            if (canvas != null) {
                canvas.save()
                canvas.translate(left, y)
                block.draw(canvas)
                canvas.restore()
            }
            return y + block.height
        }

        private fun drawEyebrowPill(canvas: Canvas, text: String, rightEdge: Float, centerY: Float) {
            val p = Paint().apply {
                isAntiAlias = true; typeface = bold; textSize = 24f; letterSpacing = 0.1f
            }
            val tw = p.measureText(text)
            val h = 52f
            val r = rightEdge
            val l = r - tw - 40f
            canvas.drawRoundRect(l, centerY - h / 2, r, centerY + h / 2, h / 2, h / 2, fill(GOLD))
            p.color = GOLD_INK
            p.textAlign = Paint.Align.CENTER
            canvas.drawText(text, (l + r) / 2f, centerY + 8f, p)
        }

        private var cachedHeight: Float = -1f
        private fun totalHeight(): Float {
            if (cachedHeight < 0f) cachedHeight = draw(null)
            return cachedHeight
        }

        private fun layout(
            text: String,
            paint: TextPaint,
            align: Layout.Alignment,
            extra: Float,
        ): StaticLayout =
            StaticLayout.Builder
                .obtain(text, 0, text.length, paint, contentWidth.coerceAtLeast(1))
                .setAlignment(align)
                .setLineSpacing(extra, 1f)
                .setIncludePad(false)
                .build()
    }

    private fun textPaint(face: Typeface, size: Float, colorInt: Int) = TextPaint().apply {
        isAntiAlias = true; typeface = face; textSize = size; color = colorInt
    }

    private fun fill(colorInt: Int) =
        Paint().apply { isAntiAlias = true; color = colorInt; style = Paint.Style.FILL }

    private fun ellipsize(text: String, max: Int): String =
        if (text.length <= max) text else text.take(max).trimEnd() + "…"
}
