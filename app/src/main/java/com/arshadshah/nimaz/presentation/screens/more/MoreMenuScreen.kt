package com.arshadshah.nimaz.presentation.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.util.WorshipReminderContent
import com.arshadshah.nimaz.core.util.formatCurrency
import com.arshadshah.nimaz.domain.model.PinnedShortcut
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.screens.resolve
import com.arshadshah.nimaz.presentation.viewmodel.more.MoreEvent
import com.arshadshah.nimaz.presentation.viewmodel.more.MoreUiState
import com.arshadshah.nimaz.presentation.viewmodel.more.MoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onShareApp: () -> Unit,
    onRateApp: () -> Unit,
    onNavigateToHadith: () -> Unit,
    onNavigateToFasting: () -> Unit,
    onNavigateToZakat: () -> Unit,
    onNavigateToDuas: () -> Unit,
    onNavigateToTafseer: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToNightWorship: () -> Unit,
    onNavigateToPrayerTimes: () -> Unit,
    onNavigateToMonthlyPrayerTimes: () -> Unit,
    onNavigateToKhatam: () -> Unit,
    onNavigateToNames: () -> Unit,
    onNavigateToQaida: () -> Unit,
    onNavigateToTasbih: () -> Unit,
    onNavigateToQibla: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPinSheet by remember { mutableStateOf(false) }

    // The pin row navigates to destinations this screen already links to, so the pinnable set is
    // a *view* of the lambdas above rather than a second navigation surface. A pin that had its
    // own navigation would be the second place a destination is reached from, and the two would
    // drift the first time a route changed.
    val pinDestinations: Map<PinnedShortcut, () -> Unit> = remember(
        onNavigateToTasbih, onNavigateToPrayerTracker, onNavigateToKhatam, onNavigateToZakat,
        onNavigateToQibla, onNavigateToFasting, onNavigateToNightWorship, onNavigateToQaida,
        onNavigateToCalendar,
    ) {
        mapOf(
            PinnedShortcut.TASBIH to onNavigateToTasbih,
            PinnedShortcut.PRAYER_TRACKER to onNavigateToPrayerTracker,
            PinnedShortcut.KHATAM to onNavigateToKhatam,
            PinnedShortcut.ZAKAT to onNavigateToZakat,
            PinnedShortcut.QIBLA to onNavigateToQibla,
            PinnedShortcut.FASTING to onNavigateToFasting,
            PinnedShortcut.NIGHT_WORSHIP to onNavigateToNightWorship,
            PinnedShortcut.QAIDA to onNavigateToQaida,
            PinnedShortcut.ISLAMIC_CALENDAR to onNavigateToCalendar,
        )
    }

    // The countdown to the next worship window is a snapshot, not a live flow, and this
    // ViewModel outlives a trip to another screen — it is scoped to the back-stack entry. Without
    // this, coming back to More after an hour shows an hour-old "in 5h 12m". Asking on resume is
    // the alternative to keeping a timer alive behind a menu nobody is looking at.
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(MoreEvent.Refresh)
        onPauseOrDispose { }
    }

    if (showPinSheet) {
        PinnedShortcutsSheet(
            pinned = state.pinnedShortcuts,
            onPinnedChange = { viewModel.onEvent(MoreEvent.SetPins(it)) },
            onDismiss = { showPinSheet = false },
        )
    }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.more),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        NimazIcon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ScreenTags.MoreList)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Pinned shortcuts — above the first section, because reaching them without
            // scrolling is the entire point.
            item {
                NimazSectionHeader(
                    title = stringResource(R.string.more_pinned_title),
                    trailingContent = {
                        NimazIconButton(
                            icon = Icons.Default.Edit,
                            onClick = { showPinSheet = true },
                            contentDescription = stringResource(R.string.more_pinned_edit),
                            size = NimazIconButtonSize.SMALL,
                        )
                    }
                )
            }
            item {
                PinnedShortcutRow(
                    pinned = state.pinnedShortcuts,
                    destinations = pinDestinations,
                )
            }

            // Daily Practice Section
            item {
                NimazSectionHeader(title = stringResource(R.string.daily_practice))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.prayer_tracker),
                        subtitle = MoreSubtitles.prayerTracker(
                            logged = state.prayersLogged,
                            total = state.prayersTrackable ?: 0,
                        ).resolve(),
                        icon = Icons.Default.Schedule,
                        onClick = onNavigateToPrayerTracker
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.fasting),
                        subtitle = MoreSubtitles.fasting(state.pendingMakeupFasts).resolve(),
                        icon = Icons.Default.Fastfood,
                        onClick = onNavigateToFasting
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.night_worship_title),
                        subtitle = MoreSubtitles.nightWorship(
                            nameRes = state.nextWorship?.let(WorshipReminderContent::nameRes),
                            minutesUntil = state.minutesUntilNextWorship,
                        ).resolve(),
                        icon = Icons.Default.Bedtime,
                        onClick = onNavigateToNightWorship
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.khatam_quran),
                        subtitle = MoreSubtitles.khatam(
                            juz = state.khatamJuz,
                            daysAgainstPace = state.khatamDaysAgainstPace,
                        ).resolve(),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToKhatam
                    )
                }
            }

            // Learning Section
            item {
                NimazSectionHeader(title = stringResource(R.string.learning))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.qaida),
                        subtitle = MoreSubtitles.qaida(
                            currentLesson = state.qaidaLesson,
                            totalLessons = state.qaidaTotalLessons ?: 0,
                        ).resolve(),
                        icon = Icons.Default.Abc,
                        onClick = onNavigateToQaida
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    // No subtitle from here down: these are reference collections with nothing
                    // true to report about them. Restating the title is what was removed.
                    // One row for what used to be three — "Allah's 99 Names", "Prophet's 99
                    // Names" and "Prophets of Islam" are three tabs of one screen now, and
                    // this is the one place a reader looks for any of them.
                    NimazMenuItem(
                        title = stringResource(R.string.names_title),
                        subtitle = stringResource(R.string.names_more_subtitle),
                        icon = Icons.Default.AutoAwesome,
                        onClick = onNavigateToNames
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.hadith),
                        icon = Icons.Default.FormatQuote,
                        onClick = onNavigateToHadith
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.duas),
                        icon = ImageVector.vectorResource(R.drawable.ic_dua),
                        onClick = onNavigateToDuas
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.tafseer),
                        icon = Icons.AutoMirrored.Filled.Article,
                        onClick = onNavigateToTafseer
                    )
                }
            }

            // Tools Section
            item {
                NimazSectionHeader(title = stringResource(R.string.tools))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.calendar),
                        subtitle = MoreSubtitles.islamicCalendar(state.hijriToday).resolve(),
                        icon = Icons.Default.CalendarMonth,
                        onClick = onNavigateToCalendar
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.prayer_times),
                        icon = Icons.Default.Mosque,
                        onClick = onNavigateToPrayerTimes
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.monthly_prayer_times),
                        icon = Icons.Default.CalendarViewMonth,
                        onClick = onNavigateToMonthlyPrayerTimes
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.zakat),
                        subtitle = state.zakatSubtitle(),
                        icon = Icons.Default.Calculate,
                        onClick = onNavigateToZakat
                    )
                }
            }

            // Support Section
            item {
                NimazSectionHeader(title = stringResource(R.string.support))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.about_nimaz),
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.help_support),
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = onNavigateToHelp
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.share_app),
                        icon = Icons.Default.Share,
                        onClick = onShareApp
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.rate_us),
                        icon = Icons.Default.Star,
                        onClick = onRateApp
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * The pinned pills.
 *
 * A `LazyRow` rather than a grid: the grid alternative broke at the largest font scale and in
 * German and Turkish, where two of these labels are half again as long. Scrolling degrades
 * gracefully where a fixed grid does not.
 *
 * Each pill is `NimazCard(onClick = …)`, never a `Modifier.clickable` wrapped around a card — a
 * wrapping clickable paints a sharp-cornered ripple that ignores the card's radius (rule 8).
 */
@Composable
private fun PinnedShortcutRow(
    pinned: List<PinnedShortcut>,
    destinations: Map<PinnedShortcut, () -> Unit>,
) {
    if (pinned.isEmpty()) {
        Text(
            text = stringResource(R.string.more_pins_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(pinned, key = { it.key }) { shortcut ->
            val onClick = destinations[shortcut]
            // A pin whose destination is not wired is dropped rather than rendered dead. It can
            // only happen if the enum gains a member and this map does not, and a pill that does
            // nothing when tapped is worse than one that is not there.
            if (onClick != null) {
                NimazCard(onClick = onClick) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NimazIcon(imageVector = shortcut.icon(), contentDescription = null)
                        Text(
                            text = stringResource(shortcut.labelRes()),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

/** The same icon the destination's own menu row carries, so a pill is recognisable as it. */
private fun PinnedShortcut.icon(): ImageVector = when (this) {
    PinnedShortcut.TASBIH -> Icons.Default.RadioButtonChecked
    PinnedShortcut.PRAYER_TRACKER -> Icons.Default.Schedule
    PinnedShortcut.KHATAM -> Icons.AutoMirrored.Filled.MenuBook
    PinnedShortcut.ZAKAT -> Icons.Default.Calculate
    PinnedShortcut.QIBLA -> Icons.Default.Explore
    PinnedShortcut.FASTING -> Icons.Default.Fastfood
    PinnedShortcut.NIGHT_WORSHIP -> Icons.Default.Bedtime
    PinnedShortcut.QAIDA -> Icons.Default.Abc
    PinnedShortcut.ISLAMIC_CALENDAR -> Icons.Default.CalendarMonth
}

/**
 * Zakat's subtitle, which needs the amount formatted before the mapper can phrase it.
 *
 * The formatting stays here rather than in the ViewModel: `formatCurrency` is presentation, and
 * the mapper takes a `String` precisely so the layer that knows the user's currency is the one
 * that renders it.
 */
@Composable
private fun MoreUiState.zakatSubtitle(): String? = MoreSubtitles.zakat(
    loaded = zakatHistoryLoaded,
    dueThisYear = zakatDueThisYear?.let { formatCurrency(it, zakatCurrency) },
).resolve()
