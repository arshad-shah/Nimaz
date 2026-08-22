package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.AudioState
import com.arshadshah.nimaz.domain.model.RecitationRepeat
import com.arshadshah.nimaz.domain.model.RecitationSpeed
import kotlinx.coroutines.flow.StateFlow

/** One ayah in a recitation playlist. */
data class AyahAudioItem(
    val ayahGlobalId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
)

/**
 * Quran recitation, as the reader drives it.
 *
 * **`QuranAudioManager` stays in `:app`, so this is what crosses the boundary.** The manager is
 * ExoPlayer-backed and inseparable from `QuranAudioService`, which builds a media notification
 * from `R.drawable.ic_stat_nimaz` and a content intent aimed at `MainActivity` — the notification
 * surface `:app` keeps deliberately. `MainActivity` also holds a manager of its own, so the class
 * cannot move down into `:feature:quran` either: it has one consumer above the feature and one
 * inside it.
 *
 * **Thirteen of the manager's thirty-nine members.** The rest are the service's own lifecycle
 * (`bindMediaSession`, download batching, player construction) and were never called from a
 * ViewModel. Keeping the port to what the reader actually uses is what makes it a seam rather
 * than a second copy of the class.
 *
 * The same shape as `AppUpdateController` (PR 14) and `CounterFeedback` (PR 18): the port moves,
 * the Android implementation does not.
 */
interface QuranPlayback {

    val audioState: StateFlow<AudioState>

    fun playSurah(surahNumber: Int, surahName: String, ayahs: List<AyahAudioItem>)

    fun playFromAyah(ayahGlobalId: Int, allAyahs: List<AyahAudioItem>, title: String)

    fun togglePlayPause()

    fun stop()

    fun skipToNext()

    fun skipToPrevious()

    fun seekToTotal(totalPositionMs: Long)

    fun setReciter(reciterId: String?, restartIfPlaying: Boolean = true)

    fun setRepeat(repeat: RecitationRepeat)

    fun setSpeed(speed: RecitationSpeed)

    fun setFollowAlong(enabled: Boolean)

    fun setContinuousPlayback(enabled: Boolean)
}
