package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.ThematicLink
import com.arshadshah.nimaz.domain.model.SurahOverviewSection
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.icon
import com.arshadshah.nimaz.presentation.components.atoms.labelRes
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.ThematicText
import com.arshadshah.nimaz.presentation.components.organisms.NimazScrollSpyIndex
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.rememberScrollSpyIndex
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicViewModel
import kotlinx.coroutines.launch

/**
 * A surah's long-form background, read the way prose is read.
 *
 * The sections used to be accordions, which is the wrong instrument twice over. It asked the
 * reader to choose what was worth opening before they had read any of it, and the one it opened
 * for them was whichever came first — almost always "Name", the shortest and least interesting
 * of the four. Here everything is open and the navigation is an *index*: a sticky row of pills
 * that scrolls you to a section and tells you which one you are in.
 *
 * The pills are labelled from the section's [com.arshadshah.nimaz.domain.model.SurahOverviewGroup]
 * because that is stable across all 114 surahs; the section itself is titled with the source's
 * own heading, which is not — 65 spellings of four ideas. So the index reads the same everywhere
 * and the prose still says what its author wrote.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahBackgroundScreen(
    surahNumber: Int,
    onNavigateBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onOpenTopic: (topicId: Int) -> Unit,
    viewModel: SurahThematicViewModel = hiltViewModel(),
) {
    val state by viewModel.backgroundState.collectAsStateWithLifecycle()

    LaunchedEffect(surahNumber) {
        viewModel.onEvent(SurahThematicEvent.Load(surahNumber))
    }

    val sections = state.overview?.sections.orEmpty()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Each section is one item, so its index in the list *is* its ordinal. Kept as an explicit
    // anchor list anyway: the day a header item joins the top of this list, the offset lives
    // here rather than as a silent +1 inside the spy.
    val anchors = remember(sections) { sections.indices.toList() }
    val activeSection by rememberScrollSpyIndex(listState, anchors)

    NimazScreenScaffold(
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.surah_info_background),
                subtitle = state.surah?.nameEnglish,
                navigationIcon = {
                    NimazIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                sections.isEmpty() -> NimazEmptyState(
                    title = stringResource(R.string.quran_topics_unavailable_title),
                    message = stringResource(R.string.quran_topics_unavailable),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    modifier = Modifier.padding(20.dp),
                )

                else -> {
                    NimazScrollSpyIndex(
                        labels = sections.map { stringResource(it.group.labelRes) },
                        selectedIndex = activeSection,
                        onSelect = { index ->
                            scope.launch {
                                listState.animateScrollToItem(anchors.getOrElse(index) { 0 })
                            }
                        },
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 40.dp),
                    ) {
                        itemsIndexed(
                            items = sections,
                            key = { _, section -> section.position },
                        ) { _, section ->
                            BackgroundSection(
                                section = section,
                                fontSize = state.proseFontSize,
                                onOpenAyah = onOpenAyah,
                                onOpenTopic = onOpenTopic,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One section: whose part of the document this is, what the source called it, and the prose.
 *
 * The eyebrow carries the stable group label and the title carries the source's own heading.
 * Where the source printed no heading at all — the shared note on 113 and 114 — the group label
 * stands in as the title and the eyebrow steps aside rather than saying the same word twice.
 */
@Composable
private fun BackgroundSection(
    section: SurahOverviewSection,
    fontSize: Float,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onOpenTopic: (topicId: Int) -> Unit,
) {
    val groupLabel = stringResource(section.group.labelRes)
    val hasOwnHeading = section.heading.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp),
    ) {
        if (hasOwnHeading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NimazIcon(
                    imageVector = section.group.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    size = NimazIconSize.SMALL,
                )
                Text(
                    text = groupLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Text(
            text = if (hasOwnHeading) section.heading else groupLabel,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(12.dp))

        ThematicText(
            html = section.body,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp),
            onLinkClick = { link ->
                when (link) {
                    is ThematicLink.Verses -> onOpenAyah(link.surah, link.from ?: 1)
                    is ThematicLink.Topic -> onOpenTopic(link.id)
                }
            },
        )
    }
}
