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
 * well as a card populates this (ayah / dua / hadith).
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
)
