package com.arshadshah.nimaz.data.qaida

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Private on-device review history. No account, network calls or microphone scoring. */
@Singleton
class QaidaJourneyStore @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("qaida_journey_v2", Context.MODE_PRIVATE)
    private val _revision = MutableStateFlow(0)
    val revision = _revision.asStateFlow()
    fun dueLessonIds(now: Long = System.currentTimeMillis()): Set<Int> = preferences.all
        .filterKeys { it.startsWith("cell_") }.values.mapNotNull { raw ->
            runCatching { JSONObject(raw as String) }.getOrNull()
        }.filter { it.has("due") && it.optLong("due") <= now }.mapNotNull { it.optInt("lesson").takeIf { id -> id > 0 } }.toSet()
    fun dueCellIds(lessonId: Int, now: Long = System.currentTimeMillis()): Set<Int> = preferences.all
        .filterKeys { it.startsWith("cell_") }.mapNotNull { (key, raw) ->
            runCatching { JSONObject(raw as String) }.getOrNull()?.takeIf {
                it.optInt("lesson") == lessonId && it.optLong("due") <= now
            }?.let { key.removePrefix("cell_").toIntOrNull() }
        }.toSet()
    fun record(lessonId: Int, cellId: Int, confident: Boolean, now: Long = System.currentTimeMillis()) {
        val key = "cell_$cellId"
        val old = runCatching { JSONObject(preferences.getString(key, "{}")!!) }.getOrDefault(JSONObject())
        val stage = if (confident) (old.optInt("stage") + 1).coerceAtMost(4) else 0
        val delay = QaidaReviewSchedule.delayMillis(stage)
        val value = JSONObject().put("lesson", lessonId).put("stage", stage).put("due", now + delay)
        preferences.edit().putString(key, value.toString()).putInt("resume_$lessonId",cellId).apply()
        recordActivity(cellId, now)
    }
    private fun day(now: Long) = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
    fun todayCount(now: Long = System.currentTimeMillis()): Int =
        if (preferences.getLong("today_day", Long.MIN_VALUE) == day(now))
            preferences.getStringSet("today_cells", emptySet()).orEmpty().size else 0
    fun recordActivity(cellId: Int, now: Long = System.currentTimeMillis()) {
        val ids = if (preferences.getLong("today_day", Long.MIN_VALUE) == day(now))
            preferences.getStringSet("today_cells", emptySet()).orEmpty().toMutableSet() else mutableSetOf()
        ids.add(cellId.toString())
        preferences.edit().putLong("today_day", day(now)).putStringSet("today_cells", ids).apply()
        _revision.value++
    }
    fun resumeCell(lessonId: Int): Int? = preferences.getInt("resume_$lessonId", -1).takeIf { it >= 0 }
    fun setResume(lessonId: Int, cellId: Int) { preferences.edit().putInt("resume_$lessonId",cellId).apply() }
    fun refresh() { _revision.value++ }
    fun reset() { preferences.edit().clear().apply(); _revision.value++ }
}

object QaidaReviewSchedule {
    // Again = ten minutes; growing confidence = 1, 3, 7, then 14 days.
    fun delayMillis(stage: Int): Long = when (stage.coerceIn(0,4)) {
        0 -> 10 * 60_000L
        1 -> 86_400_000L
        2 -> 3 * 86_400_000L
        3 -> 7 * 86_400_000L
        else -> 14 * 86_400_000L
    }
}
