package com.arshadshah.nimaz.domain.model

/**
 * The piece of shipped content something points at, named in domain terms.
 *
 * Domain types that resolve to a place in the app — an AI-cited [Proof], for instance — used to
 * carry a navigation destination directly, which made the domain layer depend on the navigation
 * graph. What they actually know is *which verse* or *which hadith*; turning that into a screen
 * is the navigation layer's job. `ContentTarget.toRoute()` in `core/navigation` does the mapping
 * at the presentation edge.
 */
sealed interface ContentTarget {
    /** A single verse, addressed the way the Quran is: surah then ayah, both 1-based. */
    data class Ayah(val surah: Int, val ayah: Int) : ContentTarget

    /** A hadith by its local record id — the same id the hadith reader takes. */
    data class Hadith(val id: String) : ContentTarget
}
