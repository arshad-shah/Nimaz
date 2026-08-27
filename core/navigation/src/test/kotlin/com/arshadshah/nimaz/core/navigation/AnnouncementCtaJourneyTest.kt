package com.arshadshah.nimaz.core.navigation

import com.arshadshah.nimaz.domain.model.AnnouncementAction
import com.arshadshah.nimaz.domain.usecase.ResolveAnnouncementRouteUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * The whole announcement CTA journey, end to end, in one JVM test.
 *
 * `ResolveAnnouncementRouteUseCase` now answers a `(String) -> Boolean` predicate instead of
 * handing back a `Route`, so domain and navigation hold two halves of the same decision:
 * domain decides *whether* the CTA shows, `NavGraph` decides *where* it goes, and they agree
 * only because both are wired to `announcementRoute`. If the predicate ever drifts from the
 * allowlist the CTA silently disappears — no crash, no log, just a banner with no button — so
 * the agreement is asserted here for every key the allowlist knows, not for a sampled few.
 *
 * **Moved here with its subject in PR 11 of #551.** It reads `AnnouncementRoutes.kt` from disk, so
 * it broke the moment that file left `:app` — loudly, because it asserts the file exists before
 * parsing it and floors the key count at 40. That is the behaviour to copy: the same milestone
 * found `MaterialTextFieldGuardTest` checking only that its *directory* existed, which stayed true
 * after its subject moved, so it would have gone on passing while scanning almost nothing. An
 * existence check on a directory is not a floor; an existence check on the exact file, plus a
 * minimum count, is.
 */
class AnnouncementCtaJourneyTest {

    // Wired exactly as AnnouncementModule wires it.
    private val resolve =
        ResolveAnnouncementRouteUseCase(isKnownFeatureKey = { announcementRoute(it) != null })

    /**
     * Every literal key in `staticAnnouncementRoute`'s `when`, read from the source.
     *
     * The function is private and there is no registry to enumerate, so the source is the only
     * honest list — the same approach `scripts/check_docs.py` takes for NAV-06/07. A hand-copied
     * list would rot the first time a key is added, which is precisely the drift being guarded.
     */
    private fun staticKeys(): List<String> {
        val source = File("src/main/kotlin/com/arshadshah/nimaz/core/navigation/AnnouncementRoutes.kt")
        assertThat(source.exists()).isTrue()
        val body = source.readText()
            .substringAfter("private fun staticAnnouncementRoute(key: String): Route? = when (key) {")
            .substringBefore("\n}")
        val keys = Regex("^\\s*\"([^\"]+)\"\\s*->", RegexOption.MULTILINE)
            .findAll(body)
            .map { it.groupValues[1] }
            .toList()
        // A parse that silently found nothing would make every assertion below vacuous.
        assertThat(keys.size).isAtLeast(40)
        return keys
    }

    @Test
    fun `the CTA predicate agrees with the route allowlist for every static key`() {
        val disagreeing = staticKeys().filter { key ->
            val navigable = announcementRoute(key) != null
            val ctaShows = resolve(key) is AnnouncementAction.NavigateToFeature
            navigable != ctaShows
        }
        assertThat(disagreeing).isEmpty()
    }

    @Test
    fun `every static key resolves to NavigateToFeature carrying the key unchanged`() {
        staticKeys().forEach { key ->
            assertThat(resolve(key)).isEqualTo(AnnouncementAction.NavigateToFeature(key))
        }
    }

    @Test
    fun `a resolved CTA key navigates to the destination the allowlist names`() {
        // The journey the user takes: FCM payload key -> domain action -> navigation target.
        val cases = mapOf(
            "quran/surah/18" to Route.QuranReader(18),
            "search/ask" to Route.GlobalSearch,
            "prayer/tracker" to Route.PrayerTracker,
            "settings/notifications/worship" to Route.SettingsWorshipReminders,
            "khatam/7" to Route.KhatamDetail(7L),
        )
        cases.forEach { (key, expected) ->
            val action = resolve(key)
            assertThat(action).isInstanceOf(AnnouncementAction.NavigateToFeature::class.java)
            val routeKey = (action as AnnouncementAction.NavigateToFeature).routeKey
            // NavGraph re-resolves from the key it was given — this is that step.
            assertThat(announcementRoute(routeKey)).isEqualTo(expected)
        }
    }

    @Test
    fun `an https key is an OpenUrl, never a navigation target`() {
        val url = "https://nimaz.arshadshah.com/privacy"
        assertThat(resolve(url)).isEqualTo(AnnouncementAction.OpenUrl(url))
        assertThat(announcementRoute(url)).isNull()
    }

    @Test
    fun `a key this version does not know shows no CTA at all`() {
        // An older install receiving a broadcast aimed at a newer one.
        assertThat(resolve("brand/new/feature")).isEqualTo(AnnouncementAction.None)
        assertThat(resolve("http://example.com")).isEqualTo(AnnouncementAction.None)
        assertThat(resolve(" ")).isEqualTo(AnnouncementAction.None)
        assertThat(resolve(null)).isEqualTo(AnnouncementAction.None)
    }
}
