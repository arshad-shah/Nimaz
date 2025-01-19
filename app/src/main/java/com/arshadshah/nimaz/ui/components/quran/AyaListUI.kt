package com.arshadshah.nimaz.ui.components.quran

import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.BannerDuration
import com.arshadshah.nimaz.ui.components.common.BannerLarge
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.almajeed
import com.arshadshah.nimaz.ui.theme.amiri
import com.arshadshah.nimaz.ui.theme.englishQuranTranslation
import com.arshadshah.nimaz.ui.theme.hidayat
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.QuranViewModel
import java.io.File
import kotlin.reflect.KFunction1


@Composable
fun AyaListUI(
    ayaList: ArrayList<LocalAya>,
    paddingValues: PaddingValues,
    loading: Boolean,
    type: String,
    number: Int,
    scrollToAya: Int? = null,
    surah: LocalSurah,
    arabicFontSize: Float,
    arabicFont: String,
    translationFontSize: Float,
    translation: String,
    scrollToVerse: LocalAya?,
    downloadAyaAudioFile: (Int, Int, (File?, Exception?, progress: Int, completed: Boolean) -> Unit) -> Unit,
    handleAyaEvents: KFunction1<QuranViewModel.AyaEvent, Unit>,
    handleQuranMenuEvents: KFunction1<QuranViewModel.QuranMenuEvents, Unit>,
) {
    val state = rememberLazyListState()

    // Placeholder for loading
    if (loading) {
        PlaceholderAyaList(
            paddingValues,
            arabicFontSize,
            arabicFont,
            translationFontSize,
            translation,
            downloadAyaAudioFile,
            handleAyaEvents,
        )
    } else {
        val sharedPref = LocalContext.current.getSharedPreferences("quran", 0)
        val visibleItemIndex =
            remember {
                mutableIntStateOf(
                    sharedPref.getInt(
                        "visibleItemIndex-${type}-${number}",
                        -1
                    )
                )
            }

        HandleScrollEffects(
            visibleItemIndex,
            scrollToAya,
            scrollToVerse,
            ayaList,
            state,
            sharedPref,
            type,
            number,
            handleQuranMenuEvents
        )

        AyaLazyColumn(
            ayaList,
            paddingValues,
            arabicFontSize,
            arabicFont,
            translationFontSize,
            translation,
            downloadAyaAudioFile,
            handleAyaEvents,
            state,
            surah
        )
    }
}

@Composable
fun PlaceholderAyaList(
    paddingValues: PaddingValues,
    arabicFontSize: Float,
    arabicFont: String,
    translationFontSize: Float,
    translation: String,
    downloadAyaAudioFile: (Int, Int, (File?, Exception?, progress: Int, completed: Boolean) -> Unit) -> Unit,
    handleAyaEvents: KFunction1<QuranViewModel.AyaEvent, Unit>,
) {
    val dummyList = remember { generateDummyAyaList() }

    LazyColumn(
        contentPadding = paddingValues,
        state = rememberLazyListState(),
        userScrollEnabled = false
    ) {
        item {
            SurahHeader(
                surah = LocalSurah(
                    number = 0,
                    numberOfAyahs = 0,
                    startAya = 0,
                    name = "",
                    englishName = "",
                    englishNameTranslation = "",
                    revelationType = "",
                    revelationOrder = 0,
                    rukus = 0
                ),
                loading = true,
            )
        }
        items(dummyList.size) { index ->
            AyaListItemUI(
                aya = dummyList[index],
                arabicFontSize = arabicFontSize,
                arabicFont = arabicFont,
                translationFontSize = translationFontSize,
                translation = translation,
                downloadAyaAudioFile = downloadAyaAudioFile,
                handleAyaEvents = handleAyaEvents,
                loading = true
            )
        }
    }
}

@Composable
fun HandleScrollEffects(
    visibleItemIndex: MutableState<Int>,
    scrollToAya: Int?,
    scrollToVerse: LocalAya?,
    ayaList: ArrayList<LocalAya>,
    state: LazyListState,
    sharedPref: SharedPreferences,
    type: String,
    number: Int,
    handleQuranMenuEvents: KFunction1<QuranViewModel.QuranMenuEvents, Unit>
) {
    // Save the last visible item index
    LaunchedEffect(key1 = remember { derivedStateOf { state.firstVisibleItemIndex } }) {
        sharedPref.edit().putInt("visibleItemIndex-${type}-${number}", state.firstVisibleItemIndex)
            .apply()
    }

    // Scroll to the specific Aya or the last visible item
    LaunchedEffect(key1 = scrollToAya, key2 = visibleItemIndex.value) {
        scrollToAya?.let {
            state.animateScrollToItem(it)
        } ?: if (visibleItemIndex.value != -1) {
            state.animateScrollToItem(visibleItemIndex.value)
            visibleItemIndex.value = -1
        } else {
            state.animateScrollToItem(state.firstVisibleItemIndex)
        }
    }

    // Scroll to the verse if specified
    LaunchedEffect(scrollToVerse) {
        scrollToVerse?.let {
            val index = ayaList.indexOfFirst {
                it.ayaNumberInQuran == scrollToVerse.ayaNumberInQuran &&
                        it.suraNumber == scrollToVerse.suraNumber &&
                        it.juzNumber == scrollToVerse.juzNumber
            }
            if (index != -1) {
                state.animateScrollToItem(index)
                handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Scroll_To_Aya(null))
            }
        }
    }
}


private fun generateDummyAyaList(): ArrayList<LocalAya> {
    // Generate a list of dummy Ayas
    val dummyList = ArrayList<LocalAya>()
    for (i in 0..9) {
        dummyList.add(
            LocalAya(
                ayaNumberInQuran = 1,
                ayaArabic = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                translationEnglish = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                translationUrdu = "اللہ کا نام سے، جو بہت مہربان ہے اور جو بہت مہربان ہے",
                audioFileLocation = "https://download.quranicaudio.com/quran/abdulbasitmurattal/001.mp3",
                ayaNumberInSurah = 1,
                bookmark = true,
                favorite = true,
                note = "dsfhsdhsgdfhstghs",
                juzNumber = 1,
                suraNumber = 1,
                ruku = 1,
                sajda = false,
                sajdaType = "",
            )
        )
    }
    return dummyList
}

@Composable
fun AyaLazyColumn(
    ayaList: ArrayList<LocalAya>,
    paddingValues: PaddingValues,
    arabicFontSize: Float,
    arabicFont: String,
    translationFontSize: Float,
    translation: String,
    downloadAyaAudioFile: (Int, Int, (File?, Exception?, progress: Int, completed: Boolean) -> Unit) -> Unit,
    handleAyaEvents: KFunction1<QuranViewModel.AyaEvent, Unit>,
    state: LazyListState,
    surah: LocalSurah
) {
    LazyColumn(
        userScrollEnabled = true,
        contentPadding = paddingValues,
        state = state
    ) {
        items(ayaList.size) { index ->
            val aya = ayaList[index]

            // Check for special cases to display Surah Header
            if (isSpecialAya(aya)) {
                handleAyaEvents(QuranViewModel.AyaEvent.getSurahById(aya.suraNumber))
                SurahHeader(surah = surah)
            }

            // Aya list item UI
            AyaListItemUI(
                aya = aya,
                arabicFontSize = arabicFontSize,
                arabicFont = arabicFont,
                translationFontSize = translationFontSize,
                translation = translation,
                downloadAyaAudioFile = downloadAyaAudioFile,
                handleAyaEvents = handleAyaEvents
            )
        }
    }
}

private fun isSpecialAya(aya: LocalAya): Boolean {
    // Define conditions for special Ayas here
    return aya.ayaNumberInQuran == 0 ||
            aya.ayaArabic == "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ﴿١﴾" ||
            aya.ayaArabic == "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ" ||
            (aya.suraNumber == 9 && aya.ayaNumberInSurah == 1)
}


@Composable
fun AyaListItemUI(
    aya: LocalAya,
    arabicFontSize: Float,
    translationFontSize: Float,
    arabicFont: String,
    translation: String,
    loading: Boolean = false,
    downloadAyaAudioFile: (Int, Int, (File?, Exception?, progress: Int, completed: Boolean) -> Unit) -> Unit,
    handleAyaEvents: KFunction1<QuranViewModel.AyaEvent, Unit>
) {
    // Initialization
    val mediaPlayer = rememberMediaPlayer()
    val error = remember { mutableStateOf("") }
    val isBookmarkedVerse = remember { mutableStateOf(aya.bookmark) }
    val isFavored = remember { mutableStateOf(aya.favorite) }
    val hasNote = remember { mutableStateOf(aya.note.isNotEmpty()) }
    val hasAudio = remember { mutableStateOf(aya.audioFileLocation.isNotEmpty()) }
    val noteContent = remember { mutableStateOf(aya.note) }
    val fileToBePlayed = remember { mutableStateOf<File?>(null) }

    // Audio Player State
    val (isPlaying, isPaused, isStopped, duration, isDownloaded, progressOfDownload, downloadInProgress) = rememberAudioPlayerState()

    // Handle MediaPlayer Lifecycle
    DisposableMediaPlayerEffect(mediaPlayer)

    // Other UI States
    val showNoteDialog = remember { mutableStateOf(false) }

    // Prepare and Play Functions
    val prepareMediaPlayer = {
        try {
            mediaPlayer.stop()
            //reset the media player
            mediaPlayer.reset()
            val file = File(aya.audioFileLocation)
            val uri = Uri.fromFile(file)
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.setDataSource(uri.toString())
            mediaPlayer.prepare()
        } catch (e: Exception) {
            error.value = e.message.toString()
        }
    }
    val playFile = {
        try {
            //if the file isnull and there is no audio playing then prepare the media player and play the file
            //else just start the current file that is playing
            if (!isPaused.value) {
                prepareMediaPlayer()
                mediaPlayer.start()
                duration.value = mediaPlayer.duration
                isPlaying.value = true
                isPaused.value = false
                isStopped.value = false
            } else {
                mediaPlayer.start()
                duration.value = mediaPlayer.duration
                isPlaying.value = true
                isPaused.value = false
                isStopped.value = false
            }
        } catch (e: Exception) {
            error.value = e.message.toString()
        }
    }
    val pauseFile = {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                isPlaying.value = false
                isPaused.value = true
                isStopped.value = false
            }
        } catch (e: Exception) {
            error.value = e.message.toString()
        }
    }
    val stopFile = {
        try {
            if (!isStopped.value) {
                mediaPlayer.stop()
                mediaPlayer.reset()
                isPlaying.value = false
                isPaused.value = false
                isStopped.value = true
            }
        } catch (e: Exception) {
            error.value = e.message.toString()
        }
    }

    // Download Callback and Function
    val downloadCallback = getDownloadCallback(
        aya,
        handleAyaEvents,
        error,
        fileToBePlayed,
        isDownloaded,
        downloadInProgress,
        progressOfDownload
    )
    val downloadFile =
        { downloadAyaAudioFile(aya.suraNumber, aya.ayaNumberInSurah, downloadCallback) }

    // UI Rendering
    AyaCard(
        aya = aya,
        arabicFontSize = arabicFontSize,
        translationFontSize = translationFontSize,
        arabicFont = arabicFont,
        translation = translation,
        isPlaying = isPlaying,
        isPaused = isPaused,
        isStopped = isStopped,
        duration = duration,
        isDownloaded = isDownloaded,
        hasAudio = hasAudio,
        playFile = playFile,
        pauseFile = pauseFile,
        stopFile = stopFile,
        loading = loading,
        error = error,
        downloadInProgress = downloadInProgress,
        isBookmarkedVerse = isBookmarkedVerse,
        noteContent = noteContent,
        isFavored = isFavored,
        hasNote = hasNote,
        showNoteDialog = showNoteDialog,
        handleAyaEvents = handleAyaEvents,
        downloadFile = downloadFile
    )
}

@Composable
fun rememberMediaPlayer(): MediaPlayer {
    // The rememberSaveable function is used to remember the MediaPlayer instance
    // across configuration changes and process death. However, MediaPlayer cannot be directly
    // saved and restored, so we use it with a key and manually manage the MediaPlayer instance.
    val mediaPlayer = remember {
        MediaPlayer()
    }

    // Dispose the MediaPlayer when this composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    // Return the remembered MediaPlayer instance
    return mediaPlayer
}

@Composable
fun rememberAudioPlayerState(): AudioPlayerState {
    // State for tracking if the audio is playing
    val isPlaying = remember { mutableStateOf(false) }

    // State for tracking if the audio is paused
    val isPaused = remember { mutableStateOf(false) }

    // State for tracking if the audio is stopped
    val isStopped = remember { mutableStateOf(false) }

    // State for tracking the duration of the audio
    val duration = remember { mutableStateOf(0) }

    // State for tracking if the audio is downloaded
    val isDownloaded = remember { mutableStateOf(false) }

    // State for tracking the progress of the audio download
    val progressOfDownload = remember { mutableStateOf(0f) }

    // State for tracking if the download is in progress
    val downloadInProgress = remember { mutableStateOf(false) }

    return AudioPlayerState(
        isPlaying,
        isPaused,
        isStopped,
        duration,
        isDownloaded,
        progressOfDownload,
        downloadInProgress
    )
}

data class AudioPlayerState(
    val isPlaying: MutableState<Boolean>,
    val isPaused: MutableState<Boolean>,
    val isStopped: MutableState<Boolean>,
    val duration: MutableState<Int>,
    val isDownloaded: MutableState<Boolean>,
    val progressOfDownload: MutableState<Float>,
    val downloadInProgress: MutableState<Boolean>
)

@Composable
fun DisposableMediaPlayerEffect(mediaPlayer: MediaPlayer) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // DisposableEffect to handle MediaPlayer lifecycle
    DisposableEffect(lifecycleOwner) {
        // Create an observer for lifecycle events
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Pause or stop the MediaPlayer if needed
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    // Reset or stop the MediaPlayer if needed
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    // Release the MediaPlayer resources
                    mediaPlayer.release()
                }

                else -> { /* Handle other lifecycle events if needed */
                }
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // OnDispose block to remove the observer and release resources
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mediaPlayer.release()
        }
    }
}

fun getDownloadCallback(
    aya: LocalAya,
    handleAyaEvents: (QuranViewModel.AyaEvent) -> Unit,
    error: MutableState<String>,
    fileToBePlayed: MutableState<File?>,
    isDownloaded: MutableState<Boolean>,
    downloadInProgress: MutableState<Boolean>,
    progressOfDownload: MutableState<Float>
): (File?, Exception?, Int, Boolean) -> Unit {

    return { file, exception, progress, completed ->
        when {
            exception != null -> {
                // Handle any error that occurs during the download
                error.value = exception.message ?: "Unknown error"
                downloadInProgress.value = false
                isDownloaded.value = false
                progressOfDownload.value = 0f
                fileToBePlayed.value = null
            }

            completed -> {
                // Handle the completion of the download
                downloadInProgress.value = false
                isDownloaded.value = true
                progressOfDownload.value = 100f
                fileToBePlayed.value = file
                aya.audioFileLocation = file?.absolutePath ?: ""
                handleAyaEvents(
                    QuranViewModel.AyaEvent.addAudioToAya(
                        aya.suraNumber,
                        aya.ayaNumberInSurah,
                        aya.audioFileLocation
                    )
                )
            }

            else -> {
                // Update the progress of the download
                downloadInProgress.value = true
                isDownloaded.value = false
                progressOfDownload.value = progress.toFloat()
                fileToBePlayed.value = null
            }
        }
    }
}


@Composable
fun AyaCard(
    aya: LocalAya,
    arabicFontSize: Float,
    translationFontSize: Float,
    arabicFont: String,
    translation: String,
    isPlaying: MutableState<Boolean>,
    isPaused: MutableState<Boolean>,
    isStopped: MutableState<Boolean>,
    duration: MutableState<Int>,
    isDownloaded: MutableState<Boolean>,
    hasAudio: MutableState<Boolean>,
    playFile: () -> Unit,
    pauseFile: () -> Unit,
    stopFile: () -> Unit,
    loading: Boolean,
    error: MutableState<String>,
    downloadInProgress: MutableState<Boolean>,
    isBookmarkedVerse: MutableState<Boolean>,
    isFavored: MutableState<Boolean>,
    hasNote: MutableState<Boolean>,
    showNoteDialog: MutableState<Boolean>,
    handleAyaEvents: (QuranViewModel.AyaEvent) -> Unit,
    downloadFile: () -> Unit,
    noteContent: MutableState<String>
) {
    // Error Dialog
    if (error.value.isNotEmpty()) {
        Dialog(onDismissRequest = { error.value = "" }) {
            BannerLarge(
                title = "Error",
                isOpen = remember { mutableStateOf(true) },
                variant = BannerVariant.Error,
                showFor = BannerDuration.FOREVER.value,
                message = error.value,
                onDismiss = { error.value = "" }
            )
        }
    }

    // Download Progress Dialog
    if (downloadInProgress.value) {
        AlertDialogNimaz(
            icon = painterResource(id = R.drawable.download_icon),
            topDivider = false,
            bottomDivider = false,
            contentHeight = 100.dp,
            contentDescription = "Downloading Audio",
            title = "Downloading Audio",
            contentToShow = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(50.dp),
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                }
            },
            onDismissRequest = { },
            showDismissButton = false,
            confirmButtonText = "Cancel",
            showConfirmButton = false,
            onConfirm = { },
            onDismiss = { }
        )
    }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    if (aya.ayaNumberInSurah != 0) {
        ElevatedCard(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .scale(scale),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Features Section
                if (aya.ayaNumberInSurah != 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        AyatFeatures(
                            isBookMarkedVerse = isBookmarkedVerse,
                            isFavouredVerse = isFavored,
                            hasNote = hasNote,
                            handleEvents = handleAyaEvents,
                            aya = aya,
                            showNoteDialog = showNoteDialog,
                            noteContent = noteContent,
                            downloadFile = downloadFile,
                            isLoading = loading,
                            isPlaying = isPlaying,
                            isPaused = isPaused,
                            isStopped = isStopped,
                            playFile = playFile,
                            pauseFile = pauseFile,
                            stopFile = stopFile,
                            isDownloaded = isDownloaded,
                            hasAudio = hasAudio,
                        )
                    }
                }

                // Content Section
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Arabic Text
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            SelectionContainer {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = aya.ayaArabic.cleanTextFromBackslash(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontSize = if (arabicFontSize == 0.0f) 24.sp else arabicFontSize.sp,
                                        fontFamily = when (arabicFont) {
                                            "Default" -> utmaniQuranFont
                                            "Quranme" -> quranFont
                                            "Hidayat" -> hidayat
                                            "Amiri" -> amiri
                                            "IndoPak" -> almajeed
                                            else -> utmaniQuranFont
                                        },
                                        textAlign = if (aya.ayaNumberInSurah != 0) TextAlign.Justify else TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .placeholder(
                                                visible = loading,
                                                highlight = PlaceholderHighlight.shimmer()
                                            )
                                    )
                                }
                            }
                        }

                        // Translation Text
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                if (translation == "Urdu") {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = "${aya.translationUrdu.cleanTextFromBackslash()} ۔",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontSize = if (translationFontSize == 0.0f) 16.sp else translationFontSize.sp,
                                            fontFamily = urduFont,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            textAlign = if (aya.ayaNumberInSurah != 0) TextAlign.Justify else TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .placeholder(
                                                    visible = loading,
                                                    highlight = PlaceholderHighlight.shimmer()
                                                )
                                        )
                                    }
                                }
                                if (translation == "English") {
                                    Text(
                                        text = aya.translationEnglish.cleanTextFromBackslash(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = englishQuranTranslation,
                                        fontSize = if (translationFontSize == 0.0f) 16.sp else translationFontSize.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        textAlign = if (aya.ayaNumberInSurah != 0) TextAlign.Justify else TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .placeholder(
                                                visible = loading,
                                                highlight = PlaceholderHighlight.shimmer()
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun String.cleanTextFromBackslash(): String {
    return this
        .replace("\\\"", "\"")  // Handle escaped quotes first
        .replace("\\\\", "\\")  // Then handle double backslashes
        .replace("\\n", "\n")   // Handle newlines
        .replace("\\t", "\t")   // Handle tabs
        .replace("\\", "")      // Finally remove any remaining single backslashes
}