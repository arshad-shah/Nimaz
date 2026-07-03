package com.arshadshah.nimaz.core.share

import android.content.Context
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.Hadith

/**
 * Single home for turning each content type into a [Shareable]. Every share-body
 * string in the app is built here **exactly once** — call sites never assemble the
 * `"$arabic\n\n$translation\n\n— …"` shape by hand any more.
 *
 * Each builder takes a [Context] so it can resolve its labels/attribution from
 * string resources (keeping the output localized), and produces both the plain
 * text and — where it reads well — a [ShareCard] for the branded image path.
 */
object Shareables {

    /** A single ayah. [surahName] is used when known, otherwise the surah number. */
    fun ayah(context: Context, ayah: Ayah, surahName: String? = null): Shareable {
        val surahLabel = surahName?.takeIf { it.isNotBlank() } ?: ayah.surahNumber.toString()
        val reference =
            context.getString(R.string.share_reference_ayah, surahLabel, ayah.ayahNumber)
        val plain = buildString {
            appendLine(ayah.textArabic)
            if (!ayah.translation.isNullOrBlank()) {
                appendLine()
                appendLine(ayah.translation)
            }
            appendLine()
            append("— ").append(reference)
            appendBranding(context)
        }
        return Shareable(
            plainText = plain,
            card = ShareCard(
                eyebrow = context.getString(R.string.share_eyebrow_quran),
                arabic = ayah.textArabic,
                transliteration = ayah.transliteration?.takeIf { it.isNotBlank() },
                body = ayah.translation?.takeIf { it.isNotBlank() },
                attribution = reference,
            ),
        )
    }

    /** A favourited ayah shown in the Quran favourites tab. */
    fun favorite(
        context: Context,
        surahName: String,
        verseLabel: String,
        arabicText: String?,
    ): Shareable {
        val reference = "$surahName · $verseLabel"
        val plain = buildString {
            append(reference)
            arabicText?.takeIf { it.isNotBlank() }?.let { appendLine(); appendLine(); append(it) }
            appendBranding(context)
        }
        return Shareable(
            plainText = plain,
            card = arabicText?.takeIf { it.isNotBlank() }?.let {
                ShareCard(
                    eyebrow = context.getString(R.string.share_eyebrow_quran),
                    arabic = it,
                    body = null,
                    attribution = reference,
                )
            },
        )
    }

    /** A hadith. [sourceLabel] is the collection reference (e.g. "Sahih al-Bukhari 1"). */
    fun hadith(context: Context, hadith: Hadith, sourceLabel: String? = null): Shareable {
        val reference = sourceLabel?.takeIf { it.isNotBlank() }
            ?: hadith.reference?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.share_reference_hadith, hadith.hadithNumber)
        val plain = buildString {
            appendLine(hadith.textArabic)
            appendLine()
            appendLine(hadith.textEnglish)
            hadith.narratorName?.takeIf { it.isNotBlank() }?.let {
                appendLine()
                appendLine(context.getString(R.string.hadith_narrated_by_format, it))
            }
            appendLine()
            append("— ").append(reference)
            appendBranding(context)
        }
        return Shareable(
            plainText = plain,
            card = ShareCard(
                eyebrow = context.getString(R.string.share_eyebrow_hadith),
                arabic = hadith.textArabic,
                body = hadith.textEnglish,
                attribution = reference,
            ),
        )
    }

    /** A dua. */
    fun dua(context: Context, dua: Dua): Shareable {
        val source = dua.reference?.takeIf { it.isNotBlank() }
            ?.let { context.getString(R.string.dua_reader_source_label, it) }
        val plain = buildString {
            appendLine(dua.titleEnglish)
            appendLine()
            appendLine(dua.textArabic)
            appendLine()
            if (!dua.textTransliteration.isNullOrEmpty()) {
                appendLine(dua.textTransliteration)
                appendLine()
            }
            append(dua.textEnglish)
            if (source != null) {
                appendLine()
                appendLine()
                append(source)
            }
            appendBranding(context)
        }
        val attribution = listOfNotNull(
            dua.titleEnglish.takeIf { it.isNotBlank() },
            dua.reference?.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { dua.titleEnglish }
        return Shareable(
            plainText = plain,
            card = ShareCard(
                eyebrow = context.getString(R.string.share_eyebrow_dua),
                arabic = dua.textArabic,
                transliteration = dua.textTransliteration?.takeIf { it.isNotBlank() },
                body = dua.textEnglish,
                attribution = attribution,
            ),
        )
    }

    /**
     * A saved bookmark (Quran / Hadith / Dua). Text-only: bookmarks mix content
     * types and already carry their own title, so they keep the simple text path.
     */
    fun bookmark(
        context: Context,
        title: String,
        arabicText: String? = null,
        note: String? = null,
    ): Shareable {
        val plain = buildString {
            append(title)
            arabicText?.takeIf { it.isNotBlank() }?.let { append("\n\n").append(it) }
            note?.takeIf { it.isNotBlank() }?.let { append("\n\n").append(it) }
            appendBranding(context)
        }
        return Shareable(plainText = plain)
    }

    /** The "invite a friend" app share. Already branded via its message + store link. */
    fun appInvite(context: Context): Shareable =
        Shareable(plainText = context.getString(R.string.share_message))

    /** A pre-built file caption / arbitrary text passthrough (e.g. PDF exports). */
    fun text(plainText: String, subject: String? = null): Shareable =
        Shareable(plainText = plainText, subject = subject)

    /** Appends the shared, localized Nimaz attribution line to a content body. */
    private fun StringBuilder.appendBranding(context: Context) {
        append("\n\n").append(context.getString(R.string.share_text_footer))
    }
}
