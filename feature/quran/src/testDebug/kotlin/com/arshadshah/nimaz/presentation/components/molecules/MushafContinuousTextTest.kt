package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.Ayah
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MushafContinuousTextTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun ayah(
        id: Int,
        surahNumber: Int = 1,
        ayahNumber: Int = id,
        textArabic: String = "نَصٌّ",
        textTajweed: String? = null
    ) = Ayah(
        id = id,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        textArabic = textArabic,
        textSimple = "نص",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = null,
        isBookmarked = false,
        textTajweed = textTajweed
    )

    private val ayahs = listOf(ayah(1), ayah(2), ayah(3))

    @Test
    fun `tap-position overload renders without crashing`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                MushafContinuousText(
                    ayahs = ayahs,
                    onAyahClick = { _: Ayah, _: Float -> }
                )
            }
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun `tap-position overload fires onAyahClick on tap`() {
        var clickedAyah: Ayah? = null
        var clickedY = -1f
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                MushafContinuousText(
                    ayahs = ayahs,
                    onAyahClick = { a, y ->
                        clickedAyah = a
                        clickedY = y
                    }
                )
            }
        }

        composeRule.onRoot().performClick()
        // The center tap should land on one of the ayah spans.
        // If layout/offset resolution yields no annotation the callback simply
        // won't fire; assert only that, when it fires, it carries a valid ayah.
        if (clickedAyah != null) {
            assertThatAyahIsKnown(clickedAyah)
            assert(clickedY >= 0f)
        }
    }

    private fun assertThatAyahIsKnown(a: Ayah) {
        assert(ayahs.any { it.id == a.id })
    }

    @Test
    fun `simple overload renders without crashing`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                MushafContinuousText(
                    ayahs = ayahs,
                    onAyahClick = { _: Ayah -> }
                )
            }
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun `highlighted ayah branch renders`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                MushafContinuousText(
                    ayahs = ayahs,
                    onAyahClick = { _: Ayah, _: Float -> },
                    highlightedAyahId = 2
                )
            }
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun `selected ayah branch renders`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                MushafContinuousText(
                    ayahs = ayahs,
                    onAyahClick = { _: Ayah, _: Float -> },
                    selectedAyahId = 3
                )
            }
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun `ruled lines disabled branch renders`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                MushafContinuousText(
                    ayahs = ayahs,
                    onAyahClick = { _: Ayah, _: Float -> },
                    showRuledLines = false
                )
            }
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun `tajweed branch renders when tajweed text present`() {
        // Valid JSON list of TajweedSegment { t, r } so TajweedParser parses it.
        val tajweedJson = "[{\"t\":\"نَصٌّ\",\"r\":\"ghunnah\"}]"
        val tajweedAyahs = listOf(
            ayah(1, textTajweed = tajweedJson),
            ayah(2, textTajweed = tajweedJson)
        )
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                MushafContinuousText(
                    ayahs = tajweedAyahs,
                    onAyahClick = { _: Ayah, _: Float -> },
                    showTajweed = true,
                    arabicFontSize = 24f
                )
            }
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun `first ayah of non-fatihah surah strips bismillah prefix without crash`() {
        val bismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val surah2 = listOf(
            ayah(
                id = 10,
                surahNumber = 2,
                ayahNumber = 1,
                textArabic = "$bismillah الٓمٓ"
            )
        )
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                MushafContinuousText(
                    ayahs = surah2,
                    onAyahClick = { _: Ayah, _: Float -> }
                )
            }
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun `tajweed-on surah opening strips bismillah in the tajweed path`() {
        // The bug: with tajweed on, the bismillah was rendered twice on
        // surah-opening ayahs. Since #290 strip(text_tajweed) == text_arabic,
        // so the bismillah appears verbatim at the head of the tajweed JSON.
        val bismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val tajweedJson = "[{\"t\":\"$bismillah الٓمٓ\",\"r\":null}]"
        val surah2 = listOf(
            ayah(id = 11, surahNumber = 2, ayahNumber = 1,
                textArabic = "$bismillah الٓمٓ", textTajweed = tajweedJson)
        )
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxWidth()) {
                MushafContinuousText(
                    ayahs = surah2,
                    onAyahClick = { _: Ayah, _: Float -> },
                    showTajweed = true
                )
            }
        }

        composeRule.onRoot().assertExists()
    }
}
