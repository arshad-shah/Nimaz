package com.arshadshah.nimaz.domain.model

/**
 * The citation-ID grammar shared with the AI Worker. IDs round-trip cleanly so
 * a citation returned by the model can be resolved back to a local record:
 *
 *  - Quran:  `quran:{surah}:{ayah}`   (both positive integers)
 *  - Hadith: `hadith:{hadithId}`      (hadithId is an opaque String)
 *  - Dua:    `dua:{duaId}`            (duaId is an opaque String)
 *
 * [parse] is deliberately strict: malformed IDs return null (the caller then
 * drops that citation silently) rather than throwing.
 */
sealed interface CitationId {
    val raw: String
    val source: ProofSource

    data class Quran(val surah: Int, val ayah: Int) : CitationId {
        override val source get() = ProofSource.QURAN
        override val raw get() = "quran:$surah:$ayah"
    }

    data class Hadith(val hadithId: String) : CitationId {
        override val source get() = ProofSource.HADITH
        override val raw get() = "hadith:$hadithId"
    }

    data class Dua(val duaId: String) : CitationId {
        override val source get() = ProofSource.DUA
        override val raw get() = "dua:$duaId"
    }

    companion object {
        fun parse(id: String): CitationId? {
            val trimmed = id.trim()
            // Split into at most 3 parts: prefix, then the remainder(s).
            val firstColon = trimmed.indexOf(':')
            if (firstColon <= 0) return null
            val prefix = trimmed.substring(0, firstColon)
            val rest = trimmed.substring(firstColon + 1)
            if (rest.isBlank()) return null

            return when (prefix) {
                "quran" -> {
                    val parts = rest.split(':')
                    if (parts.size != 2) return null
                    val surah = parts[0].toIntOrNull() ?: return null
                    val ayah = parts[1].toIntOrNull() ?: return null
                    if (surah <= 0 || ayah <= 0) return null
                    Quran(surah, ayah)
                }

                "hadith" -> Hadith(rest)
                "dua" -> Dua(rest)
                else -> null
            }
        }
    }
}
