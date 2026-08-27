package com.arshadshah.nimaz.presentation.screens.catalog

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.presentation.screens.asma.AsmaUlHusnaDetailScreen
import com.arshadshah.nimaz.presentation.screens.asmaunnabi.AsmaUnNabiDetailScreen
import com.arshadshah.nimaz.presentation.screens.names.divineName
import com.arshadshah.nimaz.presentation.screens.names.prophetName
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUlHusnaViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUnNabiViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogDetailState
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The two catalogue detail screens that share `CatalogDetailScreen`, and the shared shell itself.
 *
 * The shell was written twice before it was extracted — scaffold, back bar titled by the item,
 * favourite FAB, loading branch, `LazyColumn`, header — and the **sections** are the only genuinely
 * per-catalogue part. So the properties worth pinning are the ones the shell decides for both:
 *
 * - **The FAB shows only when there is an item.** It reads `state.item?.let`, and a FAB rendered
 *   over the loading state would toggle a favourite on nothing.
 * - **`isLoading || item == null` is one branch, not two.** A detail read that resolves to null —
 *   an id from a stale deep link, a name the content artifact no longer ships — leaves
 *   `isLoading = false` and no item, and without the second half of that guard the screen
 *   renders a header full of nulls.
 * - **The bar falls back to a generic title.** Every navigation spends its first frame with no
 *   item, and an empty app bar reads as a broken screen.
 *
 * And the one thing each screen owns: which sections it draws, and that a `LaunchedEffect` asks
 * for the id in the *route* rather than trusting whatever the shared ViewModel last loaded — the
 * three catalogue screens reuse one ViewModel instance per back-stack entry.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class CatalogDetailScreensTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val allahDetail = MutableStateFlow(CatalogDetailState<AsmaUlHusna>())
    private val prophetNameDetail = MutableStateFlow(CatalogDetailState<AsmaUnNabi>())

    private val allahEvents = mutableListOf<CatalogEvent>()
    private val prophetNameEvents = mutableListOf<CatalogEvent>()

    private val allahViewModel: AsmaUlHusnaViewModel = mockk(relaxed = true) {
        every { detailState } returns allahDetail
        every { onEvent(any()) } answers { allahEvents += firstArg<CatalogEvent>() }
    }
    private val prophetNameViewModel: AsmaUnNabiViewModel = mockk(relaxed = true) {
        every { detailState } returns prophetNameDetail
        every { onEvent(any()) } answers { prophetNameEvents += firstArg<CatalogEvent>() }
    }

    private var backs = 0

    private fun setAllahContent(nameId: Int = 1) {
        composeRule.setThemedContent {
            AsmaUlHusnaDetailScreen(
                nameId = nameId,
                onNavigateBack = { backs++ },
                viewModel = allahViewModel,
            )
        }
    }

    private fun setProphetNameContent(nameId: Int = 1) {
        composeRule.setThemedContent {
            AsmaUnNabiDetailScreen(
                nameId = nameId,
                onNavigateBack = { backs++ },
                viewModel = prophetNameViewModel,
            )
        }
    }

    private fun string(@StringRes res: Int): String = context.getString(res)

    // ---- one of the ninety-nine ------------------------------------------------------

    @Test
    fun `opening a name asks for the id the route carried`() {
        // All three catalogue screens share one ViewModel instance per back-stack entry, so
        // "whatever it last loaded" is a real and wrong answer here.
        allahDetail.value = CatalogDetailState(isLoading = false, item = divineName(1))

        setAllahContent(nameId = 42)

        assertThat(allahEvents).containsExactly(CatalogEvent.LoadDetail(42))
    }

    @Test
    fun `a name renders all five of its sections`() {
        allahDetail.value = CatalogDetailState(
            isLoading = false,
            item = divineName(
                id = 1,
                nameArabic = "الرحمن",
                nameTransliteration = "Ar-Rahman",
                meaning = "The One whose mercy encompasses all",
                explanation = "Mercy that reaches everyone",
                benefits = "Reciting it softens the heart",
                usageInDua = "Ya Rahman, have mercy on me",
            ),
        )

        setAllahContent()

        composeRule.onNodeWithText(string(R.string.asma_ul_husna_meaning)).assertExists()
        composeRule.onNodeWithText("The One whose mercy encompasses all").assertExists()
        composeRule.onNodeWithText(string(R.string.asma_ul_husna_explanation)).assertExists()
        composeRule.onNodeWithText(string(R.string.asma_ul_husna_benefits)).assertExists()
        composeRule.onNodeWithText(string(R.string.asma_ul_husna_usage_in_dua)).assertExists()
        composeRule.onNodeWithText("Ya Rahman, have mercy on me").assertExists()
    }

    @Test
    fun `the Quran references are chips, and absent when there are none`() {
        // They are the reason the shared shell takes a `LazyListScope` slot rather than a list
        // of (title, body) pairs — a `FlowRow` of chips is not prose.
        allahDetail.value = CatalogDetailState(
            isLoading = false,
            item = divineName(quranReferences = listOf("1:1", "55:1")),
        )

        setAllahContent()

        composeRule.onNodeWithText(string(R.string.asma_ul_husna_quran_references)).assertExists()
        composeRule.onNodeWithText("1:1").assertExists()
        composeRule.onNodeWithText("55:1").assertExists()
    }

    @Test
    fun `a name the artifact cites no verses for shows no references card`() {
        allahDetail.value = CatalogDetailState(
            isLoading = false,
            item = divineName(quranReferences = emptyList()),
        )

        setAllahContent()

        composeRule.onNodeWithText(string(R.string.asma_ul_husna_quran_references))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.asma_ul_husna_meaning)).assertExists()
    }

    @Test
    fun `the favourite button toggles this name and reflects its state`() {
        allahDetail.value = CatalogDetailState(
            isLoading = false,
            item = divineName(id = 7, isFavorite = false),
        )

        setAllahContent(nameId = 7)
        allahEvents.clear()
        composeRule.onNodeWithContentDescription(string(R.string.add_to_favorites)).performClick()

        assertThat(allahEvents).containsExactly(CatalogEvent.ToggleFavorite(7))
    }

    @Test
    fun `an already-starred name offers to remove rather than to add`() {
        allahDetail.value = CatalogDetailState(
            isLoading = false,
            item = divineName(id = 7, isFavorite = true),
        )

        setAllahContent(nameId = 7)

        composeRule.onNodeWithContentDescription(string(R.string.remove_from_favorites))
            .assertExists()
        composeRule.onNodeWithContentDescription(string(R.string.add_to_favorites))
            .assertDoesNotExist()
    }

    @Test
    fun `the bar carries a generic title until the name arrives`() {
        composeRule.mainClock.autoAdvance = false
        allahDetail.value = CatalogDetailState(isLoading = true)

        setAllahContent()

        composeRule.onNodeWithText(string(R.string.name_detail)).assertExists()
        // Nothing to favourite yet, so nothing offers to.
        composeRule.onNodeWithContentDescription(string(R.string.add_to_favorites))
            .assertDoesNotExist()
    }

    @Test
    fun `an id that resolves to nothing keeps the loading shell rather than a header of nulls`() {
        // `isLoading` is false and `item` is null — the second half of the shell's guard, and
        // the state a stale deep link or a retired name lands in.
        composeRule.mainClock.autoAdvance = false
        allahDetail.value = CatalogDetailState(isLoading = false, item = null)

        setAllahContent(nameId = 999)

        composeRule.onNodeWithText(string(R.string.asma_ul_husna_meaning)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.name_detail)).assertExists()
    }

    // ---- one of the names of the Prophet ﷺ -------------------------------------------

    @Test
    fun `a name of the Prophet renders its three sections and its own source`() {
        prophetNameDetail.value = CatalogDetailState(
            isLoading = false,
            item = prophetName(
                nameTransliteration = "Al-Mustafa",
                meaning = "The chosen one",
                explanation = "Chosen above all creation",
                source = "Sahih Muslim 2278",
            ),
        )

        setProphetNameContent()

        composeRule.onNodeWithText(string(R.string.asma_ul_husna_meaning)).assertExists()
        composeRule.onNodeWithText("The chosen one").assertExists()
        composeRule.onNodeWithText(string(R.string.asma_un_nabi_source)).assertExists()
        composeRule.onNodeWithText("Sahih Muslim 2278").assertExists()
        // It has no benefits or duʿāʾ-usage section — those belong to the other catalogue.
        composeRule.onNodeWithText(string(R.string.asma_ul_husna_benefits)).assertDoesNotExist()
    }

    @Test
    fun `opening a name of the Prophet asks for the route's id and can be starred`() {
        prophetNameDetail.value = CatalogDetailState(
            isLoading = false,
            item = prophetName(id = 12, isFavorite = false),
        )

        setProphetNameContent(nameId = 12)

        assertThat(prophetNameEvents).containsExactly(CatalogEvent.LoadDetail(12))

        composeRule.onNodeWithContentDescription(string(R.string.add_to_favorites)).performClick()
        assertThat(prophetNameEvents).contains(CatalogEvent.ToggleFavorite(12))
    }

    @Test
    fun `the header names the item the reader opened`() {
        prophetNameDetail.value = CatalogDetailState(
            isLoading = false,
            item = prophetName(nameArabic = "المصطفى", nameTransliteration = "Al-Mustafa"),
        )

        setProphetNameContent()

        composeRule.onNodeWithText("المصطفى").assertIsDisplayed()
    }
}
