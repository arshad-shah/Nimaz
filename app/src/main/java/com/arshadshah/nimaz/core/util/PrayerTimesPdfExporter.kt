package com.arshadshah.nimaz.core.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.domain.model.IslamicEvents
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Exports a month of prayer times to a branded, single-page A4 PDF.
 *
 * Base design: a teal gradient header with a crescent monogram and the Hijri
 * month, colour-coded prayer column "pills", per-row Hijri day, a muted
 * Sunrise (not a salat), today/Friday accents, and a footer with the
 * calculation method and coordinates.
 *
 * Context-aware: during Ramadan it shows a gold badge, a Suhūr/Iftar strip,
 * emphasised Fajr/Maghrib columns, a per-day "Ramadan N · fast duration", and
 * tints the last ten nights / Laylat al-Qadr. Islamic events (Eid, Arafah,
 * Ashura, Mawlid, …) get a coloured accent and a tag, derived from
 * [HijriDateCalculator] + [IslamicEvents] — no extra data needs to be passed in.
 */
object PrayerTimesPdfExporter {

    /** One day's row. [times] in column order: Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. */
    data class Row(
        val date: LocalDate,
        val times: List<String>,
        /** Fasting length (Fajr → Maghrib) in minutes, for Ramadan stats; null if N/A. */
        val fastMinutes: Int? = null,
    )

    /** A table column: which [idx] of Row.times, titles, accent, Ramadan emphasis. */
    private data class Col(
        val idx: Int,
        val title: String,
        val ramadan: String?,
        val color: Int,
        val emph: Boolean
    )

    private val TEAL = 0xFF14B8A6.toInt()
    private val TEAL_DARK = 0xFF0F766E.toInt()
    private val GOLD = 0xFFEAB308.toInt()
    private val GOLD_DARK = 0xFFA07C06.toInt()
    private val INK = 0xFF1C1917.toInt()
    private val MUTED = 0xFF78716C.toInt()
    private val FAINT = 0xFFA8A29E.toInt()
    private val ROW_ALT = 0xFFFAFAF9.toInt()
    private val LINE = 0xFFE7E5E4.toInt()
    private val NIGHT = 0xFF8B5CF6.toInt()
    private val INDIGO = 0xFF6366F1.toInt()

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 32f

    fun export(
        context: Context,
        locationName: String,
        methodLabel: String,
        rows: List<Row>,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
    ): File {
        require(rows.isNotEmpty()) { "No rows to export" }
        val body =
            ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_variable) ?: Typeface.DEFAULT
        val heading = ResourcesCompat.getFont(context, R.font.outfit_variable) ?: Typeface.DEFAULT
        val bold = Typeface.create(heading, Typeface.BOLD)
        val bodyBold = Typeface.create(body, Typeface.BOLD)

        // Derive the span + Hijri/Ramadan context from the rows themselves, so a
        // Ramadan export (which can cross Gregorian months) titles correctly.
        val first = rows.first().date
        val last = rows.last().date
        val firstHijri = HijriDateCalculator.toHijri(first)
        val lastHijri = HijriDateCalculator.toHijri(last)
        val gregTitle = if (first.month == last.month && first.year == last.year) {
            "${
                first.month.getDisplayName(
                    java.time.format.TextStyle.FULL,
                    Locale.getDefault()
                )
            } ${first.year}"
        } else {
            "${
                first.month.getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    Locale.getDefault()
                )
            } – " +
                    "${
                        last.month.getDisplayName(
                            java.time.format.TextStyle.SHORT,
                            Locale.getDefault()
                        )
                    } ${last.year}"
        }
        val hijriMonths = if (firstHijri.month == lastHijri.month) {
            "${HijriDateCalculator.getHijriMonthName(firstHijri.month)} ${firstHijri.year}"
        } else {
            "${HijriDateCalculator.getHijriMonthName(firstHijri.month)} – " +
                    "${HijriDateCalculator.getHijriMonthName(lastHijri.month)} ${lastHijri.year}"
        }
        val ramadanYear =
            rows.map { HijriDateCalculator.toHijri(it.date) }.firstOrNull { it.month == 9 }?.year
        val ramadanMode = ramadanYear != null
        val avgFast = if (ramadanMode) averageFast(rows) else null

        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val c = page.canvas

        // ── Header ────────────────────────────────────────────────────
        val headerH = 96f
        c.drawRect(
            0f, 0f, PAGE_W.toFloat(), headerH,
            Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f,
                    0f,
                    PAGE_W.toFloat(),
                    0f,
                    TEAL,
                    TEAL_DARK,
                    Shader.TileMode.CLAMP
                )
            },
        )
        // Official app logo in a white tile.
        val tileSize = 42f
        c.drawRoundRect(MARGIN, 27f, MARGIN + tileSize, 27f + tileSize, 11f, 11f, fill(Color.WHITE))
        runCatching {
            val logo = context.packageManager.getApplicationIcon(context.packageName)
            val pad = 5
            logo.setBounds(
                (MARGIN + pad).toInt(),
                27 + pad,
                (MARGIN + tileSize - pad).toInt(),
                (27 + tileSize - pad).toInt()
            )
            logo.draw(c)
        }.onFailure { CrashReporter.recordException(it) }
        val title = Paint().apply {
            isAntiAlias = true; color = Color.WHITE; typeface = bold; textSize = 25f
        }
        c.drawText("Nimaz", MARGIN + 52f, 44f, title)
        if (ramadanMode) {
            drawBadge(c, MARGIN + 52f, 56f, "RAMADAN $ramadanYear", body)
        } else {
            val sub = Paint().apply {
                isAntiAlias = true; color = 0xCCFFFFFF.toInt(); typeface = body; textSize = 11f
            }
            c.drawText("PRAYER TIMES", MARGIN + 53f, 64f, sub)
        }
        val mTitle = Paint().apply {
            isAntiAlias = true; color = Color.WHITE; typeface = bold; textSize = 19f; textAlign =
            Paint.Align.RIGHT
        }
        c.drawText(gregTitle, PAGE_W - MARGIN, 42f, mTitle)
        val mSub = Paint().apply {
            isAntiAlias = true; color = 0xCCFFFFFF.toInt(); typeface = body; textSize =
            11f; textAlign = Paint.Align.RIGHT
        }
        c.drawText(hijriMonths, PAGE_W - MARGIN, 60f, mSub)
        c.drawText(locationName, PAGE_W - MARGIN, 76f, mSub)

        var y = headerH
        // Ramadan info strip.
        if (ramadanMode) {
            c.drawRect(0f, y, PAGE_W.toFloat(), y + 20f, fill(tint(TEAL, 0x12)))
            val strip = Paint().apply {
                isAntiAlias = true; color = TEAL_DARK; typeface = bodyBold; textSize = 10f
            }
            c.drawText("Suhūr ends at Fajr · Iftar at Maghrib", MARGIN, y + 14f, strip)
            if (avgFast != null) {
                strip.textAlign = Paint.Align.RIGHT
                c.drawText("Avg fast $avgFast", PAGE_W - MARGIN, y + 14f, strip)
            }
            y += 20f
        }

        // ── Table geometry + column header (Sunrise omitted) ───────────
        val left = MARGIN
        val right = PAGE_W - MARGIN
        val dateColW = 134f
        // Columns map to indices of Row.times (which still includes Sunrise at 1).
        val columns = listOf(
            Col(0, "FAJR", "SUHŪR", 0xFF6366F1.toInt(), true),
            Col(2, "DHUHR", null, 0xFFEAB308.toInt(), false),
            Col(3, "ASR", null, 0xFFF97316.toInt(), false),
            Col(4, "MAGHRIB", "IFTAR", 0xFFEF4444.toInt(), true),
            Col(5, "ISHA", null, 0xFF8B5CF6.toInt(), false),
        )
        val colW = (right - (left + dateColW)) / columns.size
        val colCenter = FloatArray(columns.size) { left + dateColW + colW * it + colW / 2f }

        var headerY = y + 22f
        val pill = Paint().apply { isAntiAlias = true; typeface = bodyBold; textSize = 8.5f }
        val dh =
            Paint().apply { isAntiAlias = true; color = MUTED; typeface = bodyBold; textSize = 10f }
        c.drawText("DATE", left + 4f, headerY, dh)
        columns.forEachIndexed { i, col ->
            val pillTitle = if (ramadanMode && col.ramadan != null) col.ramadan else col.title
            drawPill(
                c,
                colCenter[i],
                headerY - 3f,
                pillTitle,
                col.color,
                ramadanMode && col.emph,
                pill
            )
        }
        headerY += 8f
        c.drawLine(
            left,
            headerY,
            right,
            headerY,
            Paint().apply { color = TEAL; strokeWidth = 1.5f })

        // ── Rows — distributed to fill the full height between the header
        //    line and the footer divider (above the generation metadata). ──
        val tableTop = headerY
        val footerLineY = PAGE_H - 34f
        val rowH = (footerLineY - 8f - tableTop) / rows.size
        val today = LocalDate.now()
        val cell = Paint().apply {
            isAntiAlias = true; typeface = body; textSize = 10.5f; color = INK; textAlign =
            Paint.Align.CENTER
        }
        val cellBold = Paint().apply {
            isAntiAlias = true; typeface = bodyBold; textSize = 10.5f; color = INK; textAlign =
            Paint.Align.CENTER
        }
        val dayNum =
            Paint().apply { isAntiAlias = true; typeface = bodyBold; textSize = 12f; color = INK }
        val dowP =
            Paint().apply { isAntiAlias = true; typeface = body; textSize = 9f; color = MUTED }
        val subP = Paint().apply { isAntiAlias = true; typeface = body; textSize = 8.5f }
        val divider = Paint().apply { color = LINE; strokeWidth = 0.8f }

        rows.forEachIndexed { index, row ->
            val top = tableTop + index * rowH
            val bottom = top + rowH
            val hijri = HijriDateCalculator.toHijri(row.date)
            val event = IslamicEvents.events
                .filter { it.hijriMonth == hijri.month && it.hijriDay == hijri.day }
                .maxByOrNull { it.priority }
            val isToday = row.date == today
            val isHoliday = event?.eventType == IslamicEventType.HOLIDAY
            val isFriday = row.date.dayOfWeek == java.time.DayOfWeek.FRIDAY
            val lastTen = hijri.month == 9 && hijri.day >= 21

            // Row background + left accent (precedence: today > holiday > event > last-ten > friday > zebra).
            val (bg, accent) = when {
                isToday -> tint(TEAL, 0x1A) to TEAL
                isHoliday -> tint(GOLD, 0x20) to GOLD
                event != null -> tint(
                    eventColor(event.eventType),
                    0x12
                ) to eventColor(event.eventType)

                lastTen -> tint(NIGHT, 0x0E) to 0
                isFriday -> tint(GOLD, 0x10) to 0
                index % 2 == 1 -> ROW_ALT to 0
                else -> 0 to 0
            }
            if (bg != 0) c.drawRect(left, top, right, bottom, fill(bg))
            if (accent != 0) c.drawRect(left, top, left + 3f, bottom, fill(accent))

            // Date cell — two vertically-centred lines kept inside dateColW.
            val midY = (top + bottom) / 2f
            val numStr = row.date.dayOfMonth.toString()
            c.drawText(numStr, left + 8f, midY - 2f, dayNum)
            val numW = dayNum.measureText(numStr)
            c.drawText(
                row.date.dayOfWeek.getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    Locale.getDefault()
                ),
                left + 12f + numW, midY - 2f, dowP,
            )
            val hijriText =
                "${hijri.day} ${HijriDateCalculator.getHijriMonthName(hijri.month).take(3)}"
            subP.typeface = body; subP.color = FAINT
            c.drawText(hijriText, left + 8f, midY + 9f, subP)
            val markerX = left + 8f + subP.measureText("$hijriText  ")
            when {
                isHoliday -> {
                    subP.typeface = bodyBold; subP.color =
                        GOLD_DARK; c.drawText("★ ${event.nameEnglish}", markerX, midY + 9f, subP)
                }

                event != null -> {
                    subP.typeface = bodyBold; subP.color = eventColor(event.eventType); c.drawText(
                        event.nameEnglish,
                        markerX,
                        midY + 9f,
                        subP
                    )
                }

                hijri.month == 9 -> {
                    val fast = row.fastMinutes?.let { formatFastLength(it) }
                    if (fast != null) {
                        subP.typeface = bodyBold; subP.color = TEAL_DARK; c.drawText(
                            "· $fast fast",
                            markerX,
                            midY + 9f,
                            subP
                        )
                    }
                }
            }

            // Times (Sunrise omitted) — Suhūr/Iftar bold in Ramadan mode.
            columns.forEachIndexed { i, col ->
                val t = row.times.getOrElse(col.idx) { "--" }
                c.drawText(
                    t,
                    colCenter[i],
                    midY + 3.5f,
                    if (ramadanMode && col.emph) cellBold else cell
                )
            }

            // Divider between rows.
            if (index < rows.size - 1) c.drawLine(left, bottom, right, bottom, divider)
        }

        // ── Footer ────────────────────────────────────────────────────
        c.drawLine(
            left,
            PAGE_H - 34f,
            right,
            PAGE_H - 34f,
            Paint().apply { color = TEAL; strokeWidth = 1.2f })
        val ft = Paint().apply { isAntiAlias = true; color = MUTED; typeface = body; textSize = 9f }
        c.drawText(
            "Generated by Nimaz · ${today.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}",
            left,
            PAGE_H - 20f,
            ft
        )
        ft.textAlign = Paint.Align.RIGHT
        val coords = if (latitude != 0.0 || longitude != 0.0) " · ${
            formatCoord(
                latitude,
                'N',
                'S'
            )
        }, ${formatCoord(longitude, 'E', 'W')}" else ""
        c.drawText(methodLabel + coords, right, PAGE_H - 20f, ft)

        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val baseName = if (ramadanMode) "Nimaz_Ramadan_$ramadanYear"
        else "Nimaz_PrayerTimes_${first.year}_${"%02d".format(first.monthValue)}"
        val file = File(dir, "$baseName.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    // ── helpers ────────────────────────────────────────────────────────
    private fun eventColor(type: IslamicEventType): Int = when (type) {
        IslamicEventType.HOLIDAY -> GOLD
        IslamicEventType.NIGHT -> NIGHT
        IslamicEventType.FAST -> TEAL
        IslamicEventType.HISTORICAL -> INDIGO
    }

    private fun drawPill(
        c: Canvas,
        centerX: Float,
        centerY: Float,
        text: String,
        color: Int,
        filled: Boolean,
        paint: Paint
    ) {
        val tw = paint.measureText(text)
        val h = 13f
        val l = centerX - tw / 2 - 7f
        val r = centerX + tw / 2 + 7f
        c.drawRoundRect(
            l,
            centerY - h / 2,
            r,
            centerY + h / 2,
            h / 2,
            h / 2,
            fill(if (filled) color else tint(color, 0x1F))
        )
        paint.color = if (filled) Color.WHITE else color
        paint.textAlign = Paint.Align.CENTER
        c.drawText(text, centerX, centerY + 3f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawBadge(c: Canvas, x: Float, topY: Float, text: String, font: Typeface) {
        val p = Paint().apply {
            isAntiAlias = true; typeface = Typeface.create(font, Typeface.BOLD); textSize = 8.5f
        }
        val tw = p.measureText(text)
        c.drawRoundRect(x, topY, x + tw + 14f, topY + 14f, 7f, 7f, fill(GOLD))
        p.color = 0xFF3A2A00.toInt()
        c.drawText(text, x + 7f, topY + 10f, p)
    }

    private fun averageFast(rows: List<Row>): String? {
        val mins = rows.mapNotNull { r ->
            if (HijriDateCalculator.toHijri(r.date).month != 9) null else r.fastMinutes
        }
        if (mins.isEmpty()) return null
        val avg = mins.average().toInt()
        return formatFastLength(avg)
    }

    private fun formatCoord(value: Double, pos: Char, neg: Char) =
        "%.2f°%s".format(abs(value), if (value >= 0) pos else neg)

    private fun tint(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha shl 24)

    private fun fill(c: Int) =
        Paint().apply { isAntiAlias = true; color = c; style = Paint.Style.FILL }
}
