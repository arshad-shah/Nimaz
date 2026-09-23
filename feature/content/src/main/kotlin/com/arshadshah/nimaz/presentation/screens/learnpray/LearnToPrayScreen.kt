package com.arshadshah.nimaz.presentation.screens.learnpray

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.arshadshah.nimaz.domain.model.ContentTarget
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.*
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.theme.AdaptiveSpacing
import com.arshadshah.nimaz.presentation.theme.LearnPrayArtColors
import com.arshadshah.nimaz.presentation.theme.LightStatusBarIcons
import com.arshadshah.nimaz.presentation.model.PrayerFigure
import com.arshadshah.nimaz.presentation.viewmodel.learnpray.*
import kotlinx.coroutines.launch

@Composable
fun LearnToPrayScreen(
    onNavigateBack: () -> Unit,
    onOpenReference: (ContentTarget) -> Unit,
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
    LearnToPrayContent(state, viewModel::onEvent, onNavigateBack, audioState) { target ->
        viewModel.onEvent(LearnPrayEvent.StopAudio)
        onOpenReference(target)
    }
}

/**
 * The posture is the lesson, so it gets the top of the screen: a teal stage (the illustrations'
 * own ground, in both themes) under the status bar, carrying back, the Man / Woman choice and the
 * rak‘ah progress. Below it everything follows the colour scheme. On a step, *What to say* stays
 * pinned above one row of controls so the words are never scrolled out of reach; its full detail —
 * every recitation, audio notes and sources — opens in a sheet.
 *
 * Text is native Compose, never painted into artwork. No timers, auto-advance or tracker writes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LearnToPrayContent(
    state: LearnPrayUiState,
    onEvent: (LearnPrayEvent) -> Unit,
    onNavigateBack: () -> Unit,
    audioState: PrayerAudioState = PrayerAudioState(),
    onOpenReference: (ContentTarget) -> Unit = {},
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
    var showRecitations by rememberSaveable(state.page) { mutableStateOf(false) }
    BackHandler(enabled = !state.preparing) { onEvent(LearnPrayEvent.Previous) }
    // The stage is dark teal in both themes.
    LightStatusBarIcons()

    val step = PrayerLesson.steps.getOrNull(state.page)
    NimazScreenScaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding()
                        .padding(start = inset, end = inset, top = 8.dp, bottom = inset),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.widthIn(max = 600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (step != null) {
                            WhatToSayCard(step, audioState, onEvent) { showRecitations = true }
                        }
                        LessonControls(state, onEvent, onNavigateBack)
                    }
                }
            }
        },
    ) { padding ->
        // Keying also resets the scroll and disclosures on each movement, including repeats.
        key(state.page) {
            Column(
                Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PostureStage(
                    artwork = when {
                        step != null -> step.artworkFor(state.figure)
                        state.complete -> PrayerLesson.steps.last().artworkFor(state.figure)
                        else -> PrayerLesson.steps[1].artworkFor(state.figure)
                    },
                    state = state,
                    onEvent = onEvent,
                    onNavigateBack = onNavigateBack,
                )
                Column(
                    Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(horizontal = inset, vertical = gap),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    when {
                        state.preparing -> {
                            LessonHeading(stringResource(R.string.learn_pray_prepare))
                            Text(stringResource(R.string.learn_pray_prepare_body), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LessonNotice(stringResource(R.string.learn_pray_review))
                            Text(stringResource(R.string.learn_pray_guide_language), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ClothingNotes(state.figure)
                            SourceNotes(
                                listOf("https://quran.com/5/6", "https://quran.com/2/144",
                                    "https://quran.com/4/103", "https://sunnah.com/bukhari:1117",
                                    "https://sunnah.com/bukhari:631", "https://sunnah.com/bukhari:365",
                                    "https://sunnah.com/abudawud:641", "https://sunnah.com/abudawud:640"),
                                openSource,
                                onOpenReference,
                            )
                        }
                        state.complete -> {
                            LessonHeading(stringResource(R.string.learn_pray_complete))
                            Text(stringResource(R.string.learn_pray_complete_body), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        step != null -> {
                            LessonHeading(stringResource(step.title))
                            Text(stringResource(step.instructionFor(state.figure)), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (step != null && showRecitations) {
        NimazBottomSheet(
            onDismissRequest = { showRecitations = false },
            title = stringResource(R.string.learn_pray_recite),
            subtitle = stringResource(step.title),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AdaptiveSpacing.sectionSpacing())) {
                step.recitations.forEach { recitation ->
                    RecitationCard(recitation, audioState, onEvent, openSource)
                }
                SourceNotes(step.referencesFor(state.figure), openSource, onOpenReference)
            }
        }
    }
}

@Composable
private fun PostureStage(
    @DrawableRes artwork: Int,
    state: LearnPrayUiState,
    onEvent: (LearnPrayEvent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val onStage = LearnPrayArtColors.OnStage
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stageHeight = (maxWidth * 1.08f).coerceIn(300.dp, 440.dp)
        Box(
            Modifier.fillMaxWidth().height(stageHeight)
                .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                // The artwork's own ground fills the middle, so its faded edge has nothing to contrast
                // with; only the stage's far corners darken.
                .background(Brush.radialGradient(
                    0f to LearnPrayArtColors.Stage,
                    0.7f to LearnPrayArtColors.Stage,
                    1f to LearnPrayArtColors.StageEdge,
                )),
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp).padding(top = 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    NimazIconButton(
                        icon = NimazIcons.Back,
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.cd_back),
                        style = NimazIconButtonStyle.FILLED_TONAL,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = onStage.copy(alpha = 0.14f), contentColor = onStage),
                    )
                    Spacer(Modifier.weight(1f))
                    if (!state.complete) {
                        NimazSegmentedControl(
                            options = listOf(
                                NimazSegmentedOption(stringResource(R.string.learn_pray_man)),
                                NimazSegmentedOption(stringResource(R.string.learn_pray_woman)),
                            ),
                            selectedIndex = state.figure.ordinal,
                            onSelect = { onEvent(LearnPrayEvent.SelectFigure(PrayerFigure.entries[it])) },
                            size = NimazSegmentedSize.SMALL,
                            width = NimazSegmentedWidth.WRAP,
                            purpose = NimazSegmentedPurpose.VIEW,
                        )
                    }
                }
                if (!state.preparing && !state.complete) {
                    val step = PrayerLesson.steps[state.page]
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.learn_pray_progress, state.page + 1, LearnPrayUiState.STEP_COUNT, step.rakah),
                        style = MaterialTheme.typography.labelLarge, color = onStage,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Spacer(Modifier.height(8.dp))
                    RakahProgress(state.page)
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    // Sized to the artwork's own 3:2, so the fade reaches the painted edge rather
                    // than a letterboxed one.
                    Image(
                        painter = painterResource(artwork),
                        // The adjacent heading and instruction are the accessible equivalent.
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.aspectRatio(ARTWORK_ASPECT).softEdges(),
                    )
                }
            }
        }
    }
}

/** Every lesson illustration is 1536×1024. */
private const val ARTWORK_ASPECT = 1.5f

/**
 * The illustrations are rectangles on a flat teal ground; fading their edges into the stage's own
 * gradient removes the seam, so the figure stands on the stage rather than in a picture of one.
 */
private fun Modifier.softEdges(): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        // DstIn keeps the artwork only where the mask is opaque, so the mask has to cover every
        // pixel: an oval alone would leave the corners, and the picture's hard edge, untouched.
        // A circular gradient over a square, stretched to the artwork's width, is an oval that
        // fades to nothing before the edges and stays transparent in the corners.
        val side = size.height
        scale(scaleX = size.width / side, scaleY = 1f, pivot = center) {
            drawRect(
                brush = Brush.radialGradient(
                    0f to Color.Black, 0.45f to Color.Black, 0.92f to Color.Transparent,
                    center = center, radius = side / 2f,
                ),
                topLeft = Offset(center.x - side / 2f, 0f),
                size = Size(side, side),
                blendMode = BlendMode.DstIn,
            )
        }
    }

/** Two rak‘ahs as two halves: the steps passed, the step you are on (in the accent), the rest. */
@Composable
private fun RakahProgress(page: Int) {
    val onStage = LearnPrayArtColors.OnStage
    val current = MaterialTheme.colorScheme.secondary
    val perRakah = PrayerLesson.steps.count { it.rakah == 1 }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0 until perRakah, perRakah until LearnPrayUiState.STEP_COUNT).forEach { range ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                range.forEach { index ->
                    Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(
                        when {
                            index < page -> onStage.copy(alpha = 0.7f)
                            index == page -> current
                            else -> onStage.copy(alpha = 0.2f)
                        },
                    ))
                }
            }
        }
    }
}

/**
 * The step's main recitation — the first with audio, so the standing step leads with Al-Fatihah
 * rather than the optional opening — always in view. Tapping opens every recitation in a sheet.
 */
@Composable
private fun WhatToSayCard(
    step: PrayerLessonStep,
    audioState: PrayerAudioState,
    onEvent: (LearnPrayEvent) -> Unit,
    onShowAll: () -> Unit,
) {
    val main = step.recitations.firstOrNull { it.audio != null } ?: step.recitations.first()
    val others = step.recitations.size - 1
    NimazCard(onClick = onShowAll, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.learn_pray_recite), style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).semantics { heading() })
                main.audio?.let { clip ->
                    val active = audioState.current == clip
                    NimazButton(
                        text = stringResource(if (active) R.string.learn_pray_audio_stop else R.string.learn_pray_audio_listen),
                        onClick = { onEvent(LearnPrayEvent.ToggleAudio(clip)) },
                        leadingIcon = if (active) Icons.Default.Stop else Icons.Default.PlayArrow,
                        variant = NimazButtonVariant.QUIET, size = NimazButtonSize.SMALL,
                    )
                }
            }
            ArabicText(text = stringResource(main.arabic).lineSequence().first(), modifier = Modifier.fillMaxWidth())
            Text(
                if (others > 0) stringResource(R.string.learn_pray_more_recitations, others)
                else stringResource(main.transliteration).lineSequence().first(),
                style = MaterialTheme.typography.bodySmall,
                color = if (others > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            if (main.audio != null && audioState.current == main.audio && audioState.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

/** One row: a small way back beside the way forward, whose label names what it does. */
@Composable
private fun LessonControls(state: LearnPrayUiState, onEvent: (LearnPrayEvent) -> Unit, onNavigateBack: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            state.complete -> NimazButton(
                text = stringResource(R.string.learn_pray_restart),
                onClick = { onEvent(LearnPrayEvent.Restart) },
                variant = NimazButtonVariant.QUIET, modifier = Modifier.weight(1f),
            )
            !state.preparing -> NimazIconButton(
                icon = NimazIcons.Back,
                onClick = { onEvent(LearnPrayEvent.Previous) },
                contentDescription = stringResource(R.string.learn_pray_back),
                style = NimazIconButtonStyle.OUTLINED,
                size = NimazIconButtonSize.LARGE,
            )
        }
        NimazButton(
            text = stringResource(
                when {
                    state.preparing -> R.string.learn_pray_start
                    state.complete -> R.string.learn_pray_done
                    state.page == LearnPrayUiState.STEP_COUNT - 1 -> R.string.learn_pray_finish
                    else -> R.string.learn_pray_next
                },
            ),
            onClick = { if (state.complete) onNavigateBack() else onEvent(LearnPrayEvent.Next) },
            modifier = Modifier.weight(1f),
            fullWidth = true,
        )
    }
}

@Composable
private fun LessonHeading(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall,
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
private fun SourceNotes(
    references: List<String>,
    onOpenExternal: (String) -> Unit,
    onOpenReference: (ContentTarget) -> Unit,
) {
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
            val reference = PrayerReference.fromUrl(url)
            NimazButton(
                text = stringResource(reference.label, reference.number),
                onClick = {
                    reference.target?.let(onOpenReference) ?: onOpenExternal(url)
                },
                variant = NimazButtonVariant.QUIET,
                fullWidth = true,
            )
        }
    }
}
