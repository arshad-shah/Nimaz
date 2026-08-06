package com.arshadshah.nimaz.core.share

/**
 * Domain-agnostic description of a piece of content the app can share through the
 * system share sheet. Any feature maps its content to a [Shareable] and hands it
 * to [ContentShareManager] — **no feature builds share Intents, share-body strings
 * or chooser titles by hand.**
 *
 * - [plainText] is always present: the attributed text body that text-only targets
 *   receive and the caption/fallback for image & file shares.
 * - [subject] is an optional line for targets that carry one (e.g. email).
 * - [card], when non-null, lets the share be rendered as a **branded Nimaz image**
 *   (gold accents, Amiri Arabic, the Nimaz wordmark) via [ShareCardRenderer], with
 *   [plainText] kept as the fallback. Content where an image adds no value
 *   (app-invite, email) leaves it null.
 *
 * Build instances through the [Shareables] factory so each content type's body
 * formatting lives in exactly one place.
 */
data class Shareable(
    val plainText: String,
    val subject: String? = null,
    val card: ShareCard? = null,
)

/**
 * Structured payload for the branded share-image renderer. Only content that reads
 * well as a card populates this (ayah / dua / hadith / zakat).
 *
 * Two shapes share one card. **Scripture** fills [arabic] / [transliteration] / [body] —
 * centred prose under a gold rule. **A figure** fills [headline] and [rows] — a plinth
 * carrying one number, then a ledger. They are not exclusive (the renderer draws whatever is
 * present, in that order), but a card that sets both is almost certainly saying the same thing
 * twice.
 */
data class ShareCard(
    /** Small uppercase eyebrow above the card: "Quran" · "Hadith" · "Dua". */
    val eyebrow: String,
    /** Arabic line, drawn large in Amiri (RTL). */
    val arabic: String?,
    /** Translation / English body, drawn below the Arabic. */
    val body: String?,
    /** Optional transliteration, drawn muted-italic between Arabic and body. */
    val transliteration: String? = null,
    /** Attribution footer: "Surah Al-Fatihah · Ayah 1" / "Sahih al-Bukhari 1". */
    val attribution: String,
    /** The one figure the card is about, drawn in a tinted plinth. Null for scripture. */
    val headline: ShareCardFigure? = null,
    /** The working behind [headline], drawn as a label/value ledger. Empty for scripture. */
    val rows: List<ShareCardRow> = emptyList(),
)

/**
 * A headline figure: the number, what it is, and — optionally — how it was arrived at.
 *
 * The zakat card exists because "€1,284.50" typed into a message says nothing. Given a label,
 * a rate and a status it becomes a receipt, which is what somebody forwarding this to their
 * family is actually sending.
 */
data class ShareCardFigure(
    /** Small uppercase caption above the number: "Zakat due". */
    val label: String,
    /** The figure itself, already formatted in the user's currency and locale. */
    val value: String,
    /** One line under the number explaining it: "2.5% of eligible wealth". */
    val caption: String? = null,
    /** An optional pill beside the label: "Above nisab". */
    val badge: String? = null,
    /** Renders [value] at reduced strength — for a figure that is zero because nothing is owed. */
    val muted: Boolean = false,
)

/** One line of a card's ledger: a label on the left, its value on the right. */
data class ShareCardRow(
    val label: String,
    val value: String,
    val tone: ShareCardRowTone = ShareCardRowTone.NEUTRAL,
)

/** How a [ShareCardRow]'s value reads: an addition, a subtraction, or the line they sum to. */
enum class ShareCardRowTone {
    NEUTRAL,

    /** Something added — drawn in brand teal. */
    POSITIVE,

    /** Something taken off — drawn in the deduction red, with the value already signed. */
    NEGATIVE,

    /** A subtotal: bolder, over a full-width rule. */
    TOTAL,
}
