package com.arshadshah.nimaz.core.navigation

import com.arshadshah.nimaz.domain.model.ContentTarget
import com.arshadshah.nimaz.domain.model.Proof
import com.arshadshah.nimaz.domain.model.ProofSource
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tapping a cited proof card, as a pure function.
 *
 * `SearchScreen` hands its `onNavigateToProof` the proof's [ContentTarget] and `NavGraph` maps
 * it with [toRoute] — the screen no longer knows the route graph exists. Both ends of that
 * hand-off were untested: the card's tap was only exercised, if at all, by running the app.
 */
class SearchProofNavigationTest {

    private fun tap(proof: Proof): Route = proof.target.toRoute()

    @Test
    fun `tapping a cited verse opens that verse in the reader`() {
        val proof = Proof.Quran(
            citationId = "quran:2:153",
            surahNumber = 2,
            ayahNumber = 153,
            surahName = "The Cow",
            displayText = "Be patient.",
            target = ContentTarget.Ayah(2, 153),
        )

        assertThat(proof.source).isEqualTo(ProofSource.QURAN)
        assertThat(tap(proof)).isEqualTo(Route.QuranReader(2, 153))
    }

    @Test
    fun `tapping a cited hadith opens that hadith record`() {
        val proof = Proof.Hadith(
            citationId = "hadith:6041",
            hadithNumber = 1,
            bookName = "Sahih al-Bukhari",
            displayText = "Actions are judged by intentions.",
            target = ContentTarget.Hadith("6041"),
        )

        assertThat(proof.source).isEqualTo(ProofSource.HADITH)
        assertThat(tap(proof)).isEqualTo(Route.HadithReader("6041"))
    }

    @Test
    fun `the destination follows the target, not the card's display fields`() {
        // Proof.Quran carries surahNumber/ayahNumber for the card's title as well as a target.
        // If the mapping ever read the display fields instead, a proof whose two disagreed
        // would navigate to the wrong verse — so make them disagree and pin the target.
        val proof = Proof.Quran(
            citationId = "quran:39:10",
            surahNumber = 39,
            ayahNumber = 10,
            surahName = "The Groups",
            displayText = "…",
            target = ContentTarget.Ayah(18, 10),
        )

        assertThat(tap(proof)).isEqualTo(Route.QuranReader(18, 10))
    }
}
