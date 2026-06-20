package com.arshadshah.nimaz.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.arshadshah.nimaz.R
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Exports a month of prayer times to a branded, single-page A4 PDF — a teal
 * Nimaz header, a colour-coded table (one row per day, the six prayers as
 * columns), with today and Fridays highlighted, and a footer noting the
 * calculation method. Drawn with [PdfDocument] so it needs no extra deps.
 */
object PrayerTimesPdfExporter {

    /** One day's row. [times] are in column order: Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. */
    data class Row(val date: LocalDate, val times: List<String>)

    private val COLUMN_TITLES = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
    private val COLUMN_COLORS = intArrayOf(
        0xFF6366F1.toInt(), // Fajr — indigo
        0xFFF59E0B.toInt(), // Sunrise — amber
        0xFFEAB308.toInt(), // Dhuhr — yellow
        0xFFF97316.toInt(), // Asr — orange
        0xFFEF4444.toInt(), // Maghrib — red
        0xFF8B5CF6.toInt(), // Isha — violet
    )

    private val TEAL = 0xFF14B8A6.toInt()
    private val INK = 0xFF1C1917.toInt()
    private val MUTED = 0xFF78716C.toInt()
    private val ROW_ALT = 0xFFF5F5F4.toInt()
    private val LINE = 0xFFE7E5E4.toInt()
    private val TODAY_TINT = 0x2214B8A6
    private val FRIDAY_TINT = 0x18EAB308

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 32f

    fun export(
        context: Context,
        month: YearMonth,
        locationName: String,
        methodLabel: String,
        rows: List<Row>,
    ): File {
        val heading = (ResourcesCompat.getFont(context, R.font.outfit_variable) ?: Typeface.DEFAULT)
        val body = (ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_variable) ?: Typeface.DEFAULT)
        val headingBold = Typeface.create(heading, Typeface.BOLD)
        val bodyBold = Typeface.create(body, Typeface.BOLD)

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas

        // ── Header band ───────────────────────────────────────────────
        c.drawRect(0f, 0f, PAGE_W.toFloat(), 104f, fill(TEAL))
        runCatching {
            val icon = context.packageManager.getApplicationIcon(context.packageName)
            icon.setBounds(MARGIN.toInt(), 28, (MARGIN + 48).toInt(), 76)
            icon.draw(c)
        }
        val white = Paint().apply { isAntiAlias = true; color = Color.WHITE; typeface = headingBold; textSize = 26f }
        c.drawText("Nimaz", MARGIN + 60f, 54f, white)
        val whiteSub = Paint().apply { isAntiAlias = true; color = 0xCCFFFFFF.toInt(); typeface = body; textSize = 12f }
        c.drawText("Prayer Times", MARGIN + 60f, 74f, whiteSub)

        val monthLabel = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"
        white.textSize = 18f
        white.textAlign = Paint.Align.RIGHT
        c.drawText(monthLabel, PAGE_W - MARGIN, 50f, white)
        whiteSub.textAlign = Paint.Align.RIGHT
        c.drawText(locationName, PAGE_W - MARGIN, 70f, whiteSub)

        // ── Table geometry ────────────────────────────────────────────
        val left = MARGIN
        val right = PAGE_W - MARGIN
        val dateColW = 96f
        val colW = (right - (left + dateColW)) / 6f
        val colLeft = FloatArray(6) { left + dateColW + colW * it }

        var y = 134f
        val hdr = Paint().apply { isAntiAlias = true; typeface = bodyBold; textSize = 10.5f }
        hdr.color = MUTED
        hdr.textAlign = Paint.Align.LEFT
        c.drawText("DATE", left + 4f, y, hdr)
        hdr.textAlign = Paint.Align.CENTER
        for (i in 0 until 6) {
            hdr.color = COLUMN_COLORS[i]
            c.drawText(COLUMN_TITLES[i].uppercase(), colLeft[i] + colW / 2f, y, hdr)
        }
        y += 8f
        c.drawLine(left, y, right, y, Paint().apply { color = TEAL; strokeWidth = 1.5f })
        y += 6f

        // ── Rows ──────────────────────────────────────────────────────
        val today = LocalDate.now()
        val rowH = if (rows.size > 28) 20f else 22f
        val cell = Paint().apply { isAntiAlias = true; typeface = body; textSize = 10f; color = INK; textAlign = Paint.Align.CENTER }
        val dayNum = Paint().apply { isAntiAlias = true; typeface = bodyBold; textSize = 10.5f; color = INK }
        val dow = Paint().apply { isAntiAlias = true; typeface = body; textSize = 8.5f; color = MUTED }

        rows.forEach { row ->
            val top = y
            val bottom = y + rowH
            when {
                row.date == today -> c.drawRect(left, top, right, bottom, fill(TODAY_TINT))
                row.date.dayOfWeek == DayOfWeek.FRIDAY -> c.drawRect(left, top, right, bottom, fill(FRIDAY_TINT))
                row.date.dayOfMonth % 2 == 0 -> c.drawRect(left, top, right, bottom, fill(ROW_ALT))
            }
            val baseline = top + rowH * 0.68f
            c.drawText(row.date.dayOfMonth.toString(), left + 6f, baseline, dayNum)
            c.drawText(row.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), left + 30f, baseline, dow)
            for (i in 0 until 6) {
                c.drawText(row.times.getOrElse(i) { "--" }, colLeft[i] + colW / 2f, baseline, cell)
            }
            y = bottom
        }
        c.drawLine(left, y, right, y, Paint().apply { color = LINE; strokeWidth = 1f })

        // ── Footer ────────────────────────────────────────────────────
        val footer = Paint().apply { isAntiAlias = true; typeface = body; textSize = 9f; color = MUTED }
        footer.textAlign = Paint.Align.LEFT
        c.drawText(
            "Generated by Nimaz · ${today.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}",
            left, PAGE_H - 24f, footer,
        )
        if (methodLabel.isNotBlank()) {
            footer.textAlign = Paint.Align.RIGHT
            c.drawText(methodLabel, right, PAGE_H - 24f, footer)
        }

        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Nimaz_PrayerTimes_${month.year}_${"%02d".format(month.monthValue)}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** A share chooser intent for the exported [file] via the app's FileProvider. */
    fun buildShareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun fill(c: Int) = Paint().apply { isAntiAlias = true; color = c; style = Paint.Style.FILL }
}
