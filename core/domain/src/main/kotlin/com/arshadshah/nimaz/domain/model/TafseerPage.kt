package com.arshadshah.nimaz.domain.model

/**
 * Represents a page of tafseer content with its character range in the full text.
 */
data class TafseerPage(
    val text: String,
    val globalStartOffset: Int,
    val globalEndOffset: Int
)

private const val MAX_CHARS_PER_PAGE = 800

fun splitTafseerIntoPages(fullText: String): List<TafseerPage> {
    if (fullText.length <= MAX_CHARS_PER_PAGE) {
        return listOf(TafseerPage(fullText, 0, fullText.length))
    }

    val pages = mutableListOf<TafseerPage>()
    var currentStart = 0

    while (currentStart < fullText.length) {
        val remaining = fullText.length - currentStart
        if (remaining <= MAX_CHARS_PER_PAGE) {
            pages.add(TafseerPage(fullText.substring(currentStart), currentStart, fullText.length))
            break
        }

        val searchEnd = (currentStart + MAX_CHARS_PER_PAGE).coerceAtMost(fullText.length)
        val chunk = fullText.substring(currentStart, searchEnd)

        val paragraphBreak = chunk.lastIndexOf("\n\n")
        val splitPoint = if (paragraphBreak > MAX_CHARS_PER_PAGE / 4) {
            paragraphBreak + 2
        } else {
            val sentenceBreak = chunk.lastIndexOf(". ")
            val lineBreak = chunk.lastIndexOf('\n')
            val bestBreak = maxOf(sentenceBreak, lineBreak)
            if (bestBreak > MAX_CHARS_PER_PAGE / 4) {
                bestBreak + 1
            } else {
                val spaceBreak = chunk.lastIndexOf(' ')
                if (spaceBreak > MAX_CHARS_PER_PAGE / 4) spaceBreak + 1 else MAX_CHARS_PER_PAGE
            }
        }

        val pageEnd = currentStart + splitPoint
        pages.add(
            TafseerPage(
                fullText.substring(currentStart, pageEnd),
                currentStart,
                pageEnd
            )
        )
        currentStart = pageEnd
    }

    return pages
}

