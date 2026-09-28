package com.arshadshah.nimaz.data.qaida

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaJourneyStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var store: QaidaJourneyStore
    @Before fun clean() { store = QaidaJourneyStore(context); store.reset(); store.updateSettings(QaidaLearningSettings()) }
    @Test fun `needs practice becomes due after ten minutes and survives recreation`() {
        store.record(4, 42, false, 1000)
        val restored = QaidaJourneyStore(context)
        assertThat(restored.dueCellIds(4, 600999)).isEmpty()
        assertThat(restored.dueCellIds(4, 601000)).containsExactly(42)
        assertThat(restored.dueLessonIds(601000)).containsExactly(4)
        assertThat(restored.resumeCell(4)).isEqualTo(42)
    }
    @Test fun `confidence grows interval and another attempt resets it`() {
        store.record(4, 42, true, 0)
        assertThat(store.dueCellIds(4, 86400000)).containsExactly(42)
        store.record(4, 42, true, 86400000)
        assertThat(store.dueCellIds(4, 2 * 86400000L)).isEmpty()
        assertThat(store.dueCellIds(4, 4 * 86400000L)).containsExactly(42)
        store.record(4, 42, false, 4 * 86400000L)
        assertThat(store.dueCellIds(4, 4 * 86400000L + 600000)).containsExactly(42)
    }
    @Test fun `reset clears review and resume without touching other app preferences`() {
        store.record(4, 42, false, 0); store.reset()
        assertThat(store.resumeCell(4)).isNull()
        assertThat(store.dueLessonIds(Long.MAX_VALUE)).isEmpty()
    }
    @Test fun `daily encouragement counts unique activity and starts fresh on a new day`() {
        store.recordActivity(1, 0); store.recordActivity(1, 0); store.recordActivity(2, 0)
        assertThat(store.todayCount(0)).isEqualTo(2)
        assertThat(QaidaJourneyStore(context).todayCount(0)).isEqualTo(2)
        assertThat(store.todayCount(2 * 86400000L)).isEqualTo(0)
        store.recordActivity(3, 2 * 86400000L)
        assertThat(store.todayCount(2 * 86400000L)).isEqualTo(1)
    }

    @Test fun `learning preferences survive recreation and progress reset`() {
        store.updateSettings(QaidaLearningSettings(showTransliteration = false, slowPlayback = true))
        store.record(4, 42, false, 0)
        val restored = QaidaJourneyStore(context)
        assertThat(restored.settings.value).isEqualTo(QaidaLearningSettings(false, true))
        restored.reset()
        assertThat(restored.settings.value).isEqualTo(QaidaLearningSettings(false, true))
        assertThat(QaidaJourneyStore(context).settings.value).isEqualTo(QaidaLearningSettings(false, true))
        assertThat(restored.dueLessonIds(Long.MAX_VALUE)).isEmpty()
    }

}
