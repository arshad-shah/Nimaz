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
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
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

    // The figure plinth. A warm gold wash rather than the deep teal the in-app hero uses:
    // this sits *inside* the white card, and a dark block there reads as a second card.
    private val PLINTH = 0xFFFEF9E7.toInt()
    private val PLINTH_LINE = 0xFFF3E3A8.toInt()
    private val NEGATIVE = 0xFFB91C1C.toInt()

    fun renderToCache(context: Context, card: ShareCard): File {
        val bitmap = Renderer(context, card).render()
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Nimaz_Share_${cardKey(card)}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    /**
     * A filename discriminator, over **everything drawn**.
     *
     * The headline and the ledger are in it because for a zakat card they are the only fields
     * that differ between two calculations: eyebrow and attribution are "Zakat" and the lunar
     * year, so a key built from those alone would give every calculation made in the same year
     * the same filename.
     */
    private fun cardKey(card: ShareCard): String =
        Integer.toHexString(
            buildList {
                add(card.eyebrow)
                add(card.attribution)
                add(card.arabic)
                add(card.body)
                add(card.transliteration)
                card.headline?.let { add(it.label); add(it.value); add(it.caption); add(it.badge) }
                card.rows.forEach { add(it.label); add(it.value); add(it.tone.name) }
            }.joinToString("|").hashCode()
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
            layout(
                ellipsize(it, MAX_ARABIC),
                textPaint(amiri, 60f, INK),
                Layout.Alignment.ALIGN_CENTER,
                14f
            )
        }
        private val translitLayout = card.transliteration?.trim()?.takeIf { it.isNotEmpty() }?.let {
            layout(
                ellipsize(it, MAX_TRANSLIT),
                textPaint(Typeface.create(bodyFont, Typeface.ITALIC), 30f, MUTED),
                Layout.Alignment.ALIGN_CENTER, 6f
            )
        }
        private val bodyLayout = card.body?.trim()?.takeIf { it.isNotEmpty() }?.let {
            layout(
                ellipsize(it, MAX_BODY),
                textPaint(bodyFont, 34f, INK),
                Layout.Alignment.ALIGN_CENTER,
                8f
            )
        }
        private val attrLayout =
            layout(
                card.attribution,
                textPaint(bold, 32f, TEAL_DARK),
                Layout.Alignment.ALIGN_CENTER,
                4f
            )
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
            val bitmap = createBitmap(W, height)
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
                    Paint().apply {
                        isAntiAlias = true; color = INK; typeface = bold; textSize = 50f
                    }
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

            // ── Headline figure (zakat and anything else that is a number, not prose) ──
            card.headline?.let { y = drawHeadline(canvas, it, left, right, y) + 34f }
            // ── The working behind it ──
            if (card.rows.isNotEmpty()) y = drawRows(canvas, left, right, y) + 34f

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

        // ── Figure + ledger ────────────────────────────────────────────────────────────
        //
        // These two are drawn with `drawText` rather than `StaticLayout` because both are
        // *tabular*: a ledger row needs its label flush left and its value flush right on one
        // shared baseline, which is a layout a StaticLayout of "label   value" cannot promise
        // at any font. Every height below therefore comes from the paint's own font metrics,
        // so the measuring walk (canvas == null) and the drawing walk agree by construction.

        private val figureLabelPaint = paint(bold, 24f, GOLD_INK, letterSpacing = 0.12f)
        private val figureValuePaint = paint(bold, 88f, INK)
        private val figureCaptionPaint = paint(bodyFont, 28f, MUTED)
        private val badgePaint = paint(bold, 22f, Color.WHITE, letterSpacing = 0.08f)
        private val rowLabelPaint = paint(bodyFont, 30f, MUTED)
        private val rowValuePaint = paint(bold, 30f, INK)
        private val totalLabelPaint = paint(bold, 32f, INK)
        private val totalValuePaint = paint(bold, 34f, INK)

        /**
         * The headline figure in its plinth: caption, optional status pill, the number, and a
         * line saying how the number was arrived at.
         */
        private fun drawHeadline(
            canvas: Canvas?,
            figure: ShareCardFigure,
            left: Float,
            right: Float,
            y: Float,
        ): Float {
            val pad = 32f
            var cursor = y + pad
            cursor += lineHeight(figureLabelPaint)
            val labelBaseline = cursor - figureLabelPaint.descent()
            cursor += 14f + lineHeight(figureValuePaint)
            val valueBaseline = cursor - figureValuePaint.descent()
            val captionBaseline = figure.caption?.let {
                cursor += 10f + lineHeight(figureCaptionPaint)
                cursor - figureCaptionPaint.descent()
            }
            val bottom = cursor + pad

            if (canvas != null) {
                canvas.drawRoundRect(left, y, right, bottom, 28f, 28f, fill(PLINTH))
                canvas.drawRoundRect(
                    left, y, right, bottom, 28f, 28f,
                    Paint().apply {
                        isAntiAlias = true; style = Paint.Style.STROKE
                        strokeWidth = 2f; color = PLINTH_LINE
                    }
                )
                canvas.drawText(
                    figure.label.uppercase(), left + pad, labelBaseline, figureLabelPaint
                )
                figure.badge?.let {
                    drawBadgePill(canvas, it.uppercase(), right - pad, labelBaseline - 10f)
                }
                // A muted figure is one that is zero *because nothing is owed* — drawing it at
                // full strength overstates a number the card is saying is not due.
                figureValuePaint.alpha = if (figure.muted) 110 else 255
                canvas.drawText(figure.value, left + pad, valueBaseline, figureValuePaint)
                figureValuePaint.alpha = 255
                if (captionBaseline != null && figure.caption != null) {
                    canvas.drawText(
                        figure.caption, left + pad, captionBaseline, figureCaptionPaint
                    )
                }
            }
            return bottom
        }

        /** The ledger: one label/value line per row, ruled, with totals set apart. */
        private fun drawRows(canvas: Canvas?, left: Float, right: Float, y: Float): Float {
            var cursor = y
            card.rows.forEachIndexed { index, row ->
                val total = row.tone == ShareCardRowTone.TOTAL
                val labelPaint = if (total) totalLabelPaint else rowLabelPaint
                val valuePaint = if (total) totalValuePaint else rowValuePaint

                if (index > 0) {
                    // A total is separated by a full rule; ordinary rows by a hairline.
                    if (canvas != null) {
                        canvas.drawRect(
                            left, cursor, right, cursor + if (total) 2f else 1f,
                            fill(if (total) MUTED else LINE)
                        )
                    }
                    cursor += (if (total) 2f else 1f) + if (total) 22f else 16f
                }

                val height = lineHeight(labelPaint).coerceAtLeast(lineHeight(valuePaint))
                if (canvas != null) {
                    val baseline = cursor + height - valuePaint.descent()
                    valuePaint.color = when (row.tone) {
                        ShareCardRowTone.POSITIVE -> TEAL_DARK
                        ShareCardRowTone.NEGATIVE -> NEGATIVE
                        ShareCardRowTone.TOTAL, ShareCardRowTone.NEUTRAL -> INK
                    }
                    // The value is laid out first and keeps its width — it is the figure, and
                    // a truncated amount is worse than a truncated label by a wide margin.
                    val valueWidth = valuePaint.measureText(row.value)
                    valuePaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(row.value, right, baseline, valuePaint)
                    valuePaint.textAlign = Paint.Align.LEFT
                    val labelWidth = (right - left) - valueWidth - 24f
                    canvas.drawText(
                        TextUtils.ellipsize(
                            row.label, TextPaint(labelPaint), labelWidth.coerceAtLeast(1f),
                            TextUtils.TruncateAt.END,
                        ).toString(),
                        left, baseline, labelPaint,
                    )
                }
                cursor += height + if (index == card.rows.lastIndex) 0f else 16f
            }
            return cursor
        }

        private fun drawBadgePill(canvas: Canvas, text: String, rightEdge: Float, centerY: Float) {
            val width = badgePaint.measureText(text)
            val height = 44f
            val l = rightEdge - width - 32f
            canvas.drawRoundRect(
                l, centerY - height / 2, rightEdge, centerY + height / 2,
                height / 2, height / 2, fill(TEAL_800),
            )
            badgePaint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, (l + rightEdge) / 2f, centerY + 8f, badgePaint)
            badgePaint.textAlign = Paint.Align.LEFT
        }

        private fun lineHeight(paint: Paint): Float = paint.descent() - paint.ascent()

        private fun paint(
            face: Typeface,
            size: Float,
            colorInt: Int,
            letterSpacing: Float = 0f,
        ) = Paint().apply {
            isAntiAlias = true
            typeface = face
            textSize = size
            color = colorInt
            this.letterSpacing = letterSpacing
        }

        /** Draws (or just advances past) a text block, returning the new y cursor. */
        private fun drawBlock(canvas: Canvas?, block: StaticLayout, left: Float, y: Float): Float {
            if (canvas != null) {
                canvas.withTranslation(left, y) {
                    block.draw(this)
                }
            }
            return y + block.height
        }

        private fun drawEyebrowPill(
            canvas: Canvas,
            text: String,
            rightEdge: Float,
            centerY: Float
        ) {
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
