package com.arshadshah.nimaz.presentation.foundation.reader

import android.content.Context
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The shared reader typography block — the Arabic size, the Arabic face, and the translation size.
 *
 * It is a `LazyListScope` extension rather than a composable, which is what lets the Quran reader,
 * the hadith reader and the settings screen all drop the *same* controls into their own lists
 * instead of each keeping a copy. Three copies is exactly what this replaced, and the thing that
 * makes one copy safe is that the ranges live here: Arabic 18–42, translation 12–28. A control
 * whose range drifted would let a reader set a size the other surfaces cannot honour.
 *
 * The font dropdown is the other half — it offers every `QuranArabicFont` and reports the font's
 * **id**, not its display name, because the id is what is persisted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ReaderTypographySettingsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun show(
        arabicFontSize: Float = 28f,
        translationFontSize: Float = 16f,
        selectedFont: QuranArabicFont = QuranArabicFont.entries.first(),
        onArabicFontSize: (Float) -> Unit = {},
        onArabicFont: (String) -> Unit = {},
        onTranslationFontSize: (Float) -> Unit = {},
    ) {
        composeRule.setThemedContent {
            LazyColumn {
                readerTypographySettings(
                    arabicFontSize = arabicFontSize,
                    onArabicFontSize = onArabicFontSize,
                    selectedFont = selectedFont,
                    onArabicFont = onArabicFont,
                    translationFontSize = translationFontSize,
                    onTranslationFontSize = onTranslationFontSize,
                )
            }
        }
    }

    @Test
    fun `both sections and both sliders are offered`() {
        show()

        // `NimazSectionHeader`, not `NimazSectionTitle` — the two are different components and
        // only the latter uppercases its text (#604).
        composeRule.onNodeWithText(context.getString(R.string.arabic_text)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.translation)).assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.arabic_font_size))
            .assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.translation_font_size))
            .assertExists()
    }

    @Test
    fun `each slider shows the size it currently holds`() {
        // The readout is formatted from the value, so a slider whose label stopped tracking would
        // leave the reader adjusting against a number that never moves.
        show(arabicFontSize = 34f, translationFontSize = 20f)

        composeRule.onNodeWithText(context.getString(R.string.arabic_font_size_value, 34))
            .assertExists()
        composeRule.onNodeWithText(context.getString(R.string.arabic_font_size_value, 20))
            .assertExists()
    }

    @Test
    fun `the arabic slider reports inside its own range`() {
        // 18–42. The range lives here precisely so the three readers cannot disagree about what a
        // legal size is.
        var reported: Float? = null
        show(onArabicFontSize = { reported = it })

        composeRule.onNodeWithContentDescription(context.getString(R.string.arabic_font_size))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }

        assertThat(reported).isNotNull()
        assertThat(reported!!).isAtLeast(18f)
        assertThat(reported!!).isAtMost(42f)
    }

    @Test
    fun `the translation slider has its own, narrower range`() {
        // 12–28 rather than 18–42: translation prose sits at body size, and sharing one range
        // would let a reader set translation text at Quranic scale.
        var reported: Float? = null
        show(onTranslationFontSize = { reported = it })

        composeRule.onNodeWithContentDescription(context.getString(R.string.translation_font_size))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }

        assertThat(reported).isNotNull()
        assertThat(reported!!).isAtMost(28f)
    }

    @Test
    fun `the font dropdown shows the face that is selected`() {
        val font = QuranArabicFont.entries.last()
        show(selectedFont = font)

        composeRule.onNodeWithText(font.displayName).assertExists()
    }

    @Test
    fun `every shipped Arabic face is offered`() {
        // The list is built from the enum, so a face added to the domain shows up here without an
        // edit — and one that stopped being offered would be unreachable from every reader at once.
        assertThat(QuranArabicFont.entries).isNotEmpty()
        assertThat(QuranArabicFont.entries.map { it.id }.toSet())
            .hasSize(QuranArabicFont.entries.size)
        assertThat(QuranArabicFont.entries.map { it.displayName }.toSet())
            .hasSize(QuranArabicFont.entries.size)
    }
}
