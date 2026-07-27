package com.arshadshah.nimaz.presentation.screens.worship

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.DUA_CATEGORY_WITR_AND_NIGHT_PRAYER
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazClockText
import com.arshadshah.nimaz.presentation.components.atoms.NimazCountdownText
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.NightWorshipEvent
import com.arshadshah.nimaz.presentation.viewmodel.NightWorshipUiState
import com.arshadshah.nimaz.presentation.viewmodel.NightWorshipViewModel
import kotlin.time.Instant

/**
 * Night worship hub — the destination for the Tahajjud and Witr Home cards.
 *
 * ## Why this screen exists
 *
 * Every other worship reminder had somewhere useful to go: the adhkar cards open their dua
 * category, the fasting cards open the fast tracker. Tahajjud and Witr had nothing, so their cards
 * counted down and then did nothing. This screen answers the question the reminder raises — *it is
 * the last third of the night, what now* — by assembling material that already ships: the last-third
 * instant from adhan2 `SunnahTimes`, the night duas (category 35), Surah Al-Mulk, and the narration
 * the whole practice rests on.
 *
 * ## Time is read, never pushed
 *
 * The ViewModel publishes instants only; the window state and countdown are derived here from the
 * shared ticker. That is deliberate — the frozen-countdown bug came from ViewModels pushing
 * pre-computed elapsed time as state.
 *
 * ## The screen is reachable at 3pm
 *
 * A user can open this any time, not just at 2am, so [NightWindow] distinguishes three states:
 * before the window (count down to it), inside it (count down to Fajr), and after Fajr (say so
 * plainly rather than showing a stale or negative countdown).
 */
@Composable
fun NightWorshipScreen(
    onNavigateBack: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    onOpenDuaCategory: (String) -> Unit,
    onOpenHadith: (String) -> Unit,
    viewModel: NightWorshipViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NightWorshipContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onOpenSurah = onOpenSurah,
        onOpenDuaCategory = onOpenDuaCategory,
        onOpenHadith = onOpenHadith,
    )
}

/**
 * The hub's UI, split from its ViewModel so every state it can be in is renderable in a test.
 *
 * That split is the difference between covering this screen and not: the window has three distinct
 * states (before, open, closed) that depend on the time of day, and a test that has to wait for
 * 2am to exercise "open" is a test nobody runs. Here they are just data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NightWorshipContent(
    state: NightWorshipUiState,
    onEvent: (NightWorshipEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    onOpenDuaCategory: (String) -> Unit,
    onOpenHadith: (String) -> Unit,
) {
    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.night_worship_title),
                onBackClick = onNavigateBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            NightWindowCard(lastThirdAt = state.lastThirdAt, fajrAt = state.fajrAt)

            RakahCounterCard(
                count = state.rakahCount,
                onAddPair = { onEvent(NightWorshipEvent.AddRakahPair) },
                onReset = { onEvent(NightWorshipEvent.ResetRakahs) },
            )

            NightWorshipRow(
                icon = Icons.Filled.Bedtime,
                title = stringResource(R.string.night_worship_witr_title),
                body = stringResource(R.string.night_worship_witr_body),
                testTag = NightWorshipWitrRowTestTag,
                onClick = { onOpenDuaCategory(DUA_CATEGORY_WITR_AND_NIGHT_PRAYER) },
            )

            NightWorshipRow(
                icon = Icons.Filled.MenuBook,
                title = stringResource(R.string.night_worship_recite_title),
                body = stringResource(R.string.night_worship_recite_body),
                testTag = NightWorshipReciteRowTestTag,
                onClick = { onOpenSurah(SURAH_AL_MULK) },
            )

            NightWorshipRow(
                icon = Icons.Filled.SelfImprovement,
                title = stringResource(R.string.night_worship_duas_title),
                body = stringResource(R.string.night_worship_duas_body),
                testTag = NightWorshipDuasRowTestTag,
                onClick = { onOpenDuaCategory(DUA_CATEGORY_WITR_AND_NIGHT_PRAYER) },
            )

            NightWorshipRow(
                icon = Icons.Filled.NightsStay,
                title = stringResource(R.string.night_worship_why_title),
                body = stringResource(R.string.night_worship_why_body),
                testTag = NightWorshipWhyRowTestTag,
                onClick = { onOpenHadith(HADITH_ID_LAST_THIRD_DESCENT) },
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Where "now" sits relative to tonight's window. */
private enum class NightWindow { BEFORE, OPEN, CLOSED }

@Composable
private fun NightWindowCard(lastThirdAt: Instant?, fajrAt: Instant?) {
    NimazCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NightWorshipWindowTestTag),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.night_worship_last_third),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            if (lastThirdAt == null || fajrAt == null) {
                Text(
                    text = stringResource(R.string.night_worship_times_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            // Minute resolution: the window boundaries are minute-granular, so reading seconds
            // here would recompose the card 60x more often for no visible difference. The
            // countdown below escalates itself.
            val now by rememberNow(TickResolution.MINUTES)
            val window = when {
                now < lastThirdAt -> NightWindow.BEFORE
                now < fajrAt -> NightWindow.OPEN
                else -> NightWindow.CLOSED
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                NimazClockText(
                    instant = lastThirdAt,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = " — ",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NimazClockText(
                    instant = fajrAt,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(6.dp))

            when (window) {
                NightWindow.BEFORE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.night_worship_opens_in),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(0.dp))
                    NimazCountdownText(
                        target = lastThirdAt,
                        showSeconds = false,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }

                NightWindow.OPEN -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.night_worship_open_now),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    NimazCountdownText(
                        target = fajrAt,
                        showSeconds = false,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }

                NightWindow.CLOSED -> Text(
                    text = stringResource(R.string.night_worship_closed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * In-memory rakah tally. Counts in pairs because night prayer is offered two by two
 * (Tirmidhi 437), with Witr made the last (Bukhari 998).
 */
@Composable
private fun RakahCounterCard(count: Int, onAddPair: () -> Unit, onReset: () -> Unit) {
    NimazCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NightWorshipCounterTestTag),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.night_worship_rakah_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.night_worship_rakah_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag(NightWorshipCountTestTag),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NimazButton(
                        text = stringResource(R.string.night_worship_reset),
                        onClick = onReset,
                        variant = NimazButtonVariant.TEXT,
                        size = NimazButtonSize.SMALL,
                    )
                    NimazButton(
                        text = stringResource(R.string.night_worship_add_pair),
                        onClick = onAddPair,
                        variant = NimazButtonVariant.TONAL,
                        size = NimazButtonSize.SMALL,
                        modifier = Modifier.testTag(NightWorshipAddRakahTestTag),
                    )
                }
            }
        }
    }
}

@Composable
private fun NightWorshipRow(
    icon: ImageVector,
    title: String,
    body: String,
    testTag: String,
    onClick: () -> Unit,
) {
    NimazCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        onClick = onClick,
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NimazIcon(imageVector = icon, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NimazIcon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

/** Surah Al-Mulk — 30 verses that intercede for their reader (Ibn Majah 3786, Sahih). */
private const val SURAH_AL_MULK = 67

/**
 * Row id of Bukhari 1145 (`bukhari:1145`) — "Our Lord descends… in the last third of the night".
 *
 * The reader resolves hadith by primary key, but the *stable* identifier is the `reference`
 * column, so this numeric id is only valid as long as the seeded content keeps its numbering.
 * `NightWorshipContentTest` asserts the two still line up, turning a silent wrong-hadith link
 * into a failing test.
 */
private const val HADITH_ID_LAST_THIRD_DESCENT = "1149"

const val NightWorshipWindowTestTag = "night_worship_window"
const val NightWorshipCounterTestTag = "night_worship_counter"
const val NightWorshipCountTestTag = "night_worship_count"
const val NightWorshipAddRakahTestTag = "night_worship_add_rakah"

// Row tags: the click action sits on the card, not on the title Text, so tests target the row
// itself rather than reaching for a clickable ancestor of a text node.
const val NightWorshipWitrRowTestTag = "night_worship_row_witr"
const val NightWorshipReciteRowTestTag = "night_worship_row_recite"
const val NightWorshipDuasRowTestTag = "night_worship_row_duas"
const val NightWorshipWhyRowTestTag = "night_worship_row_why"
