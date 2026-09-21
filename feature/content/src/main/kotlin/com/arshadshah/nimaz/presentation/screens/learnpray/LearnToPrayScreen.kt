package com.arshadshah.nimaz.presentation.screens.learnpray

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arshadshah.nimaz.domain.model.PrayerAudio
import com.arshadshah.nimaz.domain.model.PrayerAudioState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.*
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.AdaptiveSpacing
import com.arshadshah.nimaz.presentation.model.PrayerFigure
import com.arshadshah.nimaz.presentation.viewmodel.learnpray.*
import kotlinx.coroutines.launch

@Composable
fun LearnToPrayScreen(
    onNavigateBack: () -> Unit,
    viewModel: LearnPrayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onEvent(LearnPrayEvent.StopAudio)
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            viewModel.onEvent(LearnPrayEvent.StopAudio)
        }
    }
    LearnToPrayContent(state, viewModel::onEvent, onNavigateBack, audioState)
}

/** Text is native Compose, never painted into artwork. No timers, auto-advance or tracker writes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LearnToPrayContent(
    state: LearnPrayUiState,
    onEvent: (LearnPrayEvent) -> Unit,
    onNavigateBack: () -> Unit,
    audioState: PrayerAudioState = PrayerAudioState(),
) {
    val inset = AdaptiveSpacing.screenPadding()
    val gap = AdaptiveSpacing.sectionSpacing()
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sourceError = stringResource(R.string.learn_pray_source_unavailable)
    val openSource: (String) -> Unit = { url ->
        runCatching { uriHandler.openUri(url) }.onFailure {
            scope.launch { snackbar.showSnackbar(sourceError) }
        }
    }
    BackHandler(enabled = !state.preparing) { onEvent(LearnPrayEvent.Previous) }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.learn_pray_title),
                onBackClick = onNavigateBack,
                subtitle = stringResource(R.string.learn_pray_practice),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(inset),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NimazButton(
                            text = stringResource(
                                when {
                                    state.preparing -> R.string.learn_pray_start
                                    state.complete -> R.string.learn_pray_done
                                    state.page == LearnPrayUiState.STEP_COUNT - 1 -> R.string.learn_pray_finish
                                    else -> R.string.learn_pray_next
                                },
                            ),
                            onClick = {
                                if (state.complete) onNavigateBack() else onEvent(LearnPrayEvent.Next)
                            },
                            fullWidth = true,
                        )
                        if (!state.preparing) {
                            NimazButton(
                                text = stringResource(
                                    if (state.complete) R.string.learn_pray_restart
                                    else R.string.learn_pray_back,
                                ),
                                onClick = {
                                    onEvent(
                                        if (state.complete) LearnPrayEvent.Restart
                                        else LearnPrayEvent.Previous,
                                    )
                                },
                                variant = NimazButtonVariant.QUIET,
                                fullWidth = true,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            // Keying also resets the scroll and disclosure on each movement, including repeats.
            key(state.page) {
                Column(
                    Modifier.widthIn(max = 700.dp).fillMaxSize()
                        .verticalScroll(rememberScrollState()).padding(inset),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    if (!state.complete) {
                        Text(stringResource(R.string.learn_pray_illustrations),
                            style = MaterialTheme.typography.labelLarge)
                        NimazSegmentedControl(
                            options = listOf(
                                NimazSegmentedOption(stringResource(R.string.learn_pray_man)),
                                NimazSegmentedOption(stringResource(R.string.learn_pray_woman)),
                            ),
                            selectedIndex = state.figure.ordinal,
                            onSelect = { onEvent(LearnPrayEvent.SelectFigure(PrayerFigure.entries[it])) },
                            purpose = NimazSegmentedPurpose.VIEW,
                        )
                    }
                    when {
                        state.preparing -> {
                            LessonHeading(stringResource(R.string.learn_pray_prepare))
                            Image(
                                painter = painterResource(PrayerLesson.steps[1].artworkFor(state.figure)),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1.5f)
                                    .clip(MaterialTheme.shapes.large),
                            )
                            Text(stringResource(R.string.learn_pray_prepare_body), style = MaterialTheme.typography.bodyLarge)
                            LessonNotice(stringResource(R.string.learn_pray_review))
                            Text(stringResource(R.string.learn_pray_guide_language), style = MaterialTheme.typography.bodyMedium)
                            ClothingNotes(state.figure)
                            SourceNotes(
                                listOf("https://quran.com/5/6", "https://quran.com/2/144",
                                    "https://quran.com/4/103", "https://sunnah.com/bukhari:1117",
                                    "https://sunnah.com/bukhari:631", "https://sunnah.com/bukhari:365",
                                    "https://sunnah.com/abudawud:641", "https://sunnah.com/abudawud:640"),
                                openSource,
                            )
                        }
                        state.complete -> {
                            LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
                            LessonHeading(stringResource(R.string.learn_pray_complete))
                            Text(stringResource(R.string.learn_pray_complete_body), style = MaterialTheme.typography.bodyLarge)
                        }
                        else -> {
                            val step = PrayerLesson.steps[state.page]
                            Text(
                                stringResource(R.string.learn_pray_progress, state.page + 1,
                                    LearnPrayUiState.STEP_COUNT, step.rakah),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                            LinearProgressIndicator(
                                progress = { (state.page + 1f) / LearnPrayUiState.STEP_COUNT },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            LessonHeading(stringResource(step.title))
                            Image(
                                painter = painterResource(step.artworkFor(state.figure)),
                                // The adjacent instruction is the accessible equivalent.
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1.5f)
                                    .clip(MaterialTheme.shapes.large),
                            )
                            Text(stringResource(step.instructionFor(state.figure)), style = MaterialTheme.typography.bodyLarge)
                            step.recitations.forEach { recitation ->
                                RecitationCard(recitation, audioState, onEvent, openSource)
                            }
                            SourceNotes(step.referencesFor(state.figure), openSource)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonHeading(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() })
}

@Composable
private fun LessonNotice(text: String) {
    NimazCard(modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(AdaptiveSpacing.sectionSpacing()),
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RecitationCard(recitation: PrayerRecitation, audioState: PrayerAudioState,
    onEvent: (LearnPrayEvent) -> Unit, openSource: (String) -> Unit) {
    NimazCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(AdaptiveSpacing.sectionSpacing()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(recitation.title), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            if (recitation.title == R.string.learn_pray_optional_opening) {
                Text(stringResource(R.string.learn_pray_optional_opening_body),
                    style = MaterialTheme.typography.bodyMedium)
            }
            ArabicText(text = stringResource(recitation.arabic), modifier = Modifier.fillMaxWidth())
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(stringResource(R.string.learn_pray_transliteration),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(recitation.transliteration), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.learn_pray_meaning),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(recitation.meaning), style = MaterialTheme.typography.bodyMedium)
            val clip = recitation.audio
            if (clip != null) {
                val active = audioState.current == clip
                NimazButton(
                    text = stringResource(if (active) R.string.learn_pray_audio_stop else R.string.learn_pray_audio_listen),
                    onClick = { onEvent(LearnPrayEvent.ToggleAudio(clip)) },
                    leadingIcon = if (active) Icons.Default.Stop else Icons.Default.PlayArrow,
                    variant = NimazButtonVariant.QUIET, fullWidth = true,
                )
                if (active && audioState.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (audioState.failed) Text(stringResource(R.string.learn_pray_audio_error),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                val quran = clip == PrayerAudio.FATIHAH || clip == PrayerAudio.IKHLAS
                Text(stringResource(if (quran) R.string.learn_pray_audio_quran else R.string.learn_pray_audio_hisn),
                    style = MaterialTheme.typography.bodySmall)
                NimazButton(stringResource(R.string.learn_pray_audio_source), {
                    openSource(if (quran) "https://everyayah.com/data/Husary_128kbps/" else "https://www.hisnmuslim.com/")
                }, variant = NimazButtonVariant.TEXT)
            } else {
                Text(stringResource(R.string.learn_pray_audio_pending), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SourceNotes(references: List<String>, onOpen: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    NimazButton(
        text = stringResource(if (expanded) R.string.learn_pray_sources_hide else R.string.learn_pray_sources),
        onClick = { expanded = !expanded },
        variant = NimazButtonVariant.TEXT,
        leadingIcon = if (expanded) NimazIcons.Collapse else NimazIcons.Expand,
        fullWidth = true,
    )
    if (expanded) {
        Text(stringResource(R.string.learn_pray_source_intro), style = MaterialTheme.typography.bodyMedium)
        references.forEach { url ->
            NimazButton(
                text = url.removePrefix("https://"),
                onClick = { onOpen(url) },
                variant = NimazButtonVariant.QUIET,
                fullWidth = true,
            )
        }
    }
}
