package com.arshadshah.nimaz.core.navigation

import com.arshadshah.nimaz.domain.model.ContentTarget
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The one mapping that keeps `ContentTarget` honest.
 *
 * Until this existed the only thing pinning "an AI-cited verse opens the Quran reader at that
 * ayah" was `AskWithProofUseCaseTest`, which asserted on the `Route` the use case produced. The
 * moment domain stopped producing routes those assertions lost their teeth — they now pin the
 * `ContentTarget`, and nothing pinned the *other* half of the journey. This does.
 */
class ContentTargetRoutesTest {

    @Test
    fun `an ayah target opens the Quran reader at that surah and ayah`() {
        assertThat(ContentTarget.Ayah(2, 153).toRoute())
            .isEqualTo(Route.QuranReader(2, 153))
    }

    @Test
    fun `a hadith target opens the hadith reader on that record id`() {
        assertThat(ContentTarget.Hadith("6041").toRoute())
            .isEqualTo(Route.HadithReader("6041"))
    }

    @Test
    fun `the ayah number is carried, not defaulted`() {
        // Route.QuranReader defaults ayahNumber to 1. A mapping that dropped the ayah would
        // still compile and still navigate — to the top of the surah, silently losing the
        // verse the model actually cited.
        assertThat(ContentTarget.Ayah(18, 10).toRoute())
            .isNotEqualTo(Route.QuranReader(18))
        assertThat((ContentTarget.Ayah(18, 10).toRoute() as Route.QuranReader).ayahNumber)
            .isEqualTo(10)
    }

    @Test
    fun `surah and ayah are not transposed`() {
        val route = ContentTarget.Ayah(surah = 4, ayah = 36).toRoute() as Route.QuranReader
        assertThat(route.surahNumber).isEqualTo(4)
        assertThat(route.ayahNumber).isEqualTo(36)
    }
}
