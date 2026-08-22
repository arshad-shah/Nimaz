package com.arshadshah.nimaz.domain.model

/**
 * What the Quran recitation is doing, as the reader sees it.
 *
 * Declared inside `QuranAudioManager.kt` until PR 19 of #551, which is where it had to leave: the
 * manager stays in `:app` (see [com.arshadshah.nimaz.domain.repository.QuranPlayback]) while the
 * ViewModel that observes this moved to `:feature:quran`, and a state type cannot be private to
 * the side that produces it.

 * Pure data — every field is a primitive or a `:core:domain` model, which is why it could move
 * here rather than to `:core:data`. Nothing about ExoPlayer reaches this far.
 */
data class AudioState(
    val isPlaying: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val currentAyahId: Int = 0,
    val currentSurahNumber: Int = 0,
    // Total playlist duration and position (across all ayahs)
    val duration: Long = 0L,
    val position: Long = 0L,
    val currentTitle: String = "",
    val currentSubtitle: String? = null,
    val reciterName: String = QuranReciter.DEFAULT.displayName,
    val isActive: Boolean = false,
    val error: String? = null,
    // Playlist progress for surah-level tracking
    val currentAyahIndex: Int = 0,
    val totalAyahs: Int = 0,
    // Download progress for batch downloads
    val downloadedCount: Int = 0,
    val totalToDownload: Int = 0,
    val isPreparing: Boolean = false,
    /** What to go back and say again — off, a verse N times, a range, or the whole surah. */
    val repeat: RecitationRepeat = RecitationRepeat.Off,
    /** Playback rate. Deliberately not persisted — see [QuranAudioManager.setSpeed]. */
    val speed: RecitationSpeed = RecitationSpeed.DEFAULT,
    /**
     * Whether the reader follows the recitation: scrolling the verse list, or turning the
     * mushaf page, to keep the verse being recited on screen.
     */
    val followAlong: Boolean = false,
) {
    // Calculate surah progress as percentage (0.0 to 1.0)
    val surahProgress: Float
        get() = if (duration > 0) position.toFloat() / duration else 0f
}

