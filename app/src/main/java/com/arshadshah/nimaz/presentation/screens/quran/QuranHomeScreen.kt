package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.presentation.components.atoms.GradientCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.BookmarkCard
import com.arshadshah.nimaz.presentation.components.molecules.ContinueReadingCard
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.molecules.QuranRecommendedSurahs
import com.arshadshah.nimaz.presentation.components.molecules.VerseOfTheDayCard
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranHomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranViewModel
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The Qur'an section's front door.
 *
 * It used to stack four rows of chrome — an app bar, a tab row, a sub-tab row and a search
 * field — before any content, and two of those tabs were whole screens wearing a tab's clothes:
 * a 114-row browse list and a favourites list, both living inside a screen whose back arrow did
 * not return to them. They are destinations now, with their own back arrows, and what is left
 * here is what a front door is for: where you were, where you can go, and something to read.
 *
 * The bookmark action leaves the app bar with them — Saved is one of the four rows, and an
 * action bar carrying a destination that is also a row is the same thing said twice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranHomeScreen(
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToBrowse: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToTopics: () -> Unit = {},
    onNavigateToQuranAyah: (Int, Int) -> Unit = { surah, _ -> onNavigateToSurah(surah) },
    onNavigateToKhatam: () -> Unit = {},
    viewModel: QuranViewModel = hiltViewModel()
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.quran_home_title),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        NimazIcon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.quran_home_search)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        NimazIcon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.quran_home_quran_settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            NimazLoadingState()
        } else {
            HomeContent(
                state = state,
                bookmarks = bookmarksState.bookmarks,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onNavigateToSurah = onNavigateToSurah,
                onNavigateToBrowse = onNavigateToBrowse,
                onNavigateToSaved = onNavigateToSaved,
                onNavigateToTopics = onNavigateToTopics,
                onNavigateToQuranAyah = onNavigateToQuranAyah,
                onNavigateToKhatam = onNavigateToKhatam,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: QuranHomeUiState,
    bookmarks: List<QuranBookmark>,
    modifier: Modifier = Modifier,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToBrowse: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToTopics: () -> Unit,
    onNavigateToQuranAyah: (Int, Int) -> Unit,
    onNavigateToKhatam: () -> Unit,
) {
    val isFriday = remember { LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Resume first. Exactly one card on this screen carries the teal gradient:
        // continue-reading when there is progress, otherwise the start-reading hero.
        val progress = state.readingProgress
        if (progress != null) {
            item(key = "continue_reading") {
                ContinueReadingCard(
                    surahNumber = progress.lastSurah,
                    ayahNumber = progress.lastAyah,
                    juzNumber = progress.lastReadJuz,
                    pageNumber = progress.lastReadPage,
                    totalAyahsRead = progress.totalAyahsRead,
                    surahName = state.surahs.find { it.number == progress.lastSurah },
                    onClick = { onNavigateToQuranAyah(progress.lastSurah, progress.lastAyah) },
                    totalPages = state.pagination.totalPages
                )
            }
        } else {
            item(key = "start_reading") {
                StartReadingHero(
                    enabled = state.surahs.isNotEmpty(),
                    onClick = { onNavigateToSurah(1) }
                )
            }
        }

        // 2. The four destinations. Each carries a count or a status, because a row that only
        // names a place cannot tell you whether it is worth opening — "Saved · 12" and
        // "Khatam · 40%" are the difference between a menu and a dashboard.
        item(key = "destinations") {
            Destinations(
                state = state,
                savedCount = bookmarks.size,
                onNavigateToBrowse = onNavigateToBrowse,
                onNavigateToSaved = onNavigateToSaved,
                onNavigateToTopics = onNavigateToTopics,
                onNavigateToKhatam = onNavigateToKhatam,
            )
        }

        // 3. Recommended surahs (contextual — Al-Kahf first on Fridays). Kept because it is
        // the only surfaced way into Al-Kahf / Al-Mulk / Yasin by occasion, and the only thing
        // on this screen that works for a reader with nothing saved.
        item(key = "recommended_header") {
            HomeSectionTitle(text = stringResource(R.string.quran_home_recommended))
        }
        item(key = "recommended_surahs") {
            QuranRecommendedSurahs(
                surahs = state.surahs,
                isFriday = isFriday,
                onSurahClick = onNavigateToSurah,
                pagination = state.pagination,
            )
        }

        // 4. Recently saved — the strip that used to be titled "Bookmarks" and pointed at a
        // screen reached from an app-bar icon.
        if (bookmarks.isNotEmpty()) {
            item(key = "recently_saved_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeSectionTitle(text = stringResource(R.string.quran_home_recently_saved))
                    Text(
                        text = stringResource(R.string.quran_home_see_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(4.dp)
                            // rule 8's exemption: a text link inside a header row, not a card.
                            .clickable(onClick = onNavigateToSaved)
                    )
                }
            }

            item(key = "recently_saved_row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items = bookmarks, key = { quranBookmarkKey(it) }) { bookmark ->
                        BookmarkCard(
                            bookmark = bookmark,
                            onClick = { onNavigateToSurah(bookmark.surahNumber) }
                        )
                    }
                }
            }
        }

        // 5. Verse of the Day — a daily nudge, not the headline.
        val verse = state.verseOfTheDay
        if (verse != null) {
            item(key = "verse_of_the_day") {
                val surahName = state.surahs.find { it.number == verse.surahNumber }?.nameEnglish
                    ?: stringResource(R.string.quran_home_surah_fallback, verse.surahNumber)
                VerseOfTheDayCard(
                    arabicText = verse.textArabic,
                    translation = verse.translation,
                    reference = stringResource(
                        R.string.quran_home_verse_reference,
                        surahName,
                        verse.surahNumber,
                        verse.ayahNumber
                    ),
                    onClick = { onNavigateToQuranAyah(verse.surahNumber, verse.ayahNumber) }
                )
            }
        }
    }
}

/**
 * Browse · Saved · Themes · Khatam, each pushing a real destination.
 *
 * Themes is drawn only where the install's artifact actually carries the thematic layer — an
 * install between the migration and a schemaVersion 24 release has the tables and no rows, and
 * a row promising 2,512 subjects would open onto an explanation.
 */
@Composable
private fun Destinations(
    state: QuranHomeUiState,
    savedCount: Int,
    onNavigateToBrowse: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToTopics: () -> Unit,
    onNavigateToKhatam: () -> Unit,
) {
    NimazMenuGroup {
        NimazMenuItem(
            title = stringResource(R.string.quran_home_tab_browse),
            subtitle = stringResource(R.string.quran_home_destination_browse_subtitle),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = onNavigateToBrowse,
        )
        NimazMenuItem(
            title = stringResource(R.string.saved),
            subtitle = stringResource(R.string.quran_home_destination_saved_subtitle),
            icon = Icons.Default.Bookmark,
            onClick = onNavigateToSaved,
            trailing = if (savedCount > 0) {
                { CountBadge(savedCount.toString(), NimazTone.NEUTRAL) }
            } else {
                null
            },
        )
        if (state.hasThematicContent) {
            NimazMenuItem(
                title = stringResource(R.string.quran_home_browse_subjects),
                subtitle = stringResource(R.string.quran_home_browse_subjects_subtitle),
                icon = Icons.Default.AccountTree,
                onClick = onNavigateToTopics,
            )
        }
        val khatamPercent = state.activeKhatam?.let {
            (it.progressPercent * 100).toInt().coerceIn(0, 100)
        }
        NimazMenuItem(
            title = stringResource(R.string.khatam),
            subtitle = stringResource(R.string.quran_home_destination_khatam_subtitle),
            icon = Icons.Default.DonutLarge,
            onClick = onNavigateToKhatam,
            trailing = khatamPercent?.let { percent ->
                { CountBadge("$percent%", NimazTone.ACCENT) }
            },
        )
    }
}

@Composable
private fun CountBadge(text: String, tone: NimazTone) {
    NimazBadge(
        text = text,
        size = NimazBadgeSize.SMALL,
        tone = tone,
        emphasis = NimazBadgeEmphasis.SOFT,
    )
    Spacer(modifier = Modifier.width(8.dp))
}

@Composable
private fun StartReadingHero(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    GradientCard(
        gradientColors = NimazColors.QuranColors.BannerGradient,
        shape = shape,
        onClick = { if (enabled) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = NimazColors.QuranColors.BannerBorder, shape = shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NimazIcon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = NimazColors.QuranColors.BannerAccent,
                iconSize = 32.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quran_home_start_reading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.quran_home_begin_journey),
                    style = MaterialTheme.typography.bodySmall,
                    color = NimazColors.Gray300
                )
            }
        }
    }
}

@Composable
private fun HomeSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

/**
 * The stable identity of a bookmark in a lazy list.
 *
 * **Not** [QuranBookmark.id]. `BookmarkEntity` has no id column — its primary key is
 * the composite `(kind, target_id)` — so `QuranRepositoryImpl.toQuranBookmark()` has
 * nothing to map and passes a literal `0` for every bookmark. Keying by it gave every
 * row the same key, and Compose threw `Key "0" was already used` as soon as a reader
 * had two bookmarks, taking the whole Qur'an home screen down.
 *
 * [QuranBookmark.ayahId] is the global ayah id, which is precisely what
 * `(kind = AYAH, target_id)` makes unique.
 */
internal fun quranBookmarkKey(bookmark: QuranBookmark): Int = bookmark.ayahId
