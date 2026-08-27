package com.arshadshah.nimaz.core.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.text.StringProvider
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The copy behind every extended worship reminder, in both of the shapes it is asked for.
 *
 * This object is read from two sides — the notification builds title and body from a `Context`,
 * and the Home "Next Worship" card resolves the same strings through a `StringProvider` because a
 * ViewModel must not hold one. Two parallel `when`s over eleven reminder types is exactly the
 * shape that drifts, and the file's own KDoc records it happening once already: *"the first cut of
 * this dropped the subKey branch and always returned bodyRes(type), which silently gave every
 * Arafah/Ashura reminder the Arafah body."* A user fasting on Ashura got told about Arafah, on a
 * notification, once a year.
 *
 * So the two things worth pinning are: every type resolves to real, distinct copy, and the two
 * overloads never disagree.
 */
@RunWith(RobolectricTestRunner::class)
class WorshipReminderContentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** The seam, backed by the same resources the `Context` overload reads. */
    private val strings = object : StringProvider {
        override fun get(id: Int, vararg args: Any): String = context.getString(id, *args)
        override fun quantity(id: Int, count: Int, vararg args: Any): String =
            context.resources.getQuantityString(id, count, *args)
    }

    @Test
    fun `every reminder type has a name, an arabic name, a title and a body`() {
        WorshipReminderType.entries.forEach { type ->
            assertThat(WorshipReminderContent.name(context, type)).isNotEmpty()
            assertThat(WorshipReminderContent.arabic(context, type)).isNotEmpty()
            assertThat(WorshipReminderContent.title(context, type)).isNotEmpty()
            assertThat(WorshipReminderContent.body(context, type)).isNotEmpty()
        }
    }

    @Test
    fun `no two reminder types share a name`() {
        // Eleven arms of a `when`, hand-mapped to eleven resource ids. A copy-paste that pointed
        // two types at one id would name two different reminders the same thing in the list.
        val names = WorshipReminderType.entries.map { WorshipReminderContent.name(context, it) }

        assertThat(names.toSet()).hasSize(WorshipReminderType.entries.size)
    }

    @Test
    fun `no two reminder types share a body`() {
        val bodies = WorshipReminderType.entries.map { WorshipReminderContent.body(context, it) }

        assertThat(bodies.toSet()).hasSize(WorshipReminderType.entries.size)
    }

    @Test
    fun `the monday-thursday fast titles itself for the day it fires on`() {
        val monday = WorshipReminderContent.title(
            context, WorshipReminderType.MONDAY_THURSDAY_FAST, subKey = "monday",
        )
        val thursday = WorshipReminderContent.title(
            context, WorshipReminderType.MONDAY_THURSDAY_FAST, subKey = "thursday",
        )

        assertThat(monday).isNotEqualTo(thursday)
    }

    @Test
    fun `an unknown or absent subKey falls back to monday`() {
        // `if (subKey == "thursday")` — everything else is Monday, including null. A test that
        // only passed the two known keys would not notice an inverted condition.
        val monday = WorshipReminderContent.title(
            context, WorshipReminderType.MONDAY_THURSDAY_FAST, subKey = "monday",
        )

        assertThat(WorshipReminderContent.title(context, WorshipReminderType.MONDAY_THURSDAY_FAST))
            .isEqualTo(monday)
        assertThat(
            WorshipReminderContent.title(
                context, WorshipReminderType.MONDAY_THURSDAY_FAST, subKey = "saturday",
            )
        ).isEqualTo(monday)
    }

    @Test
    fun `arafah and ashura get their own title and their own body`() {
        // The regression the file documents: the body is the half that was dropped, so it is
        // asserted as deliberately as the title.
        val arafahTitle = WorshipReminderContent.title(
            context, WorshipReminderType.ARAFAH_ASHURA_FAST, subKey = "arafah",
        )
        val ashuraTitle = WorshipReminderContent.title(
            context, WorshipReminderType.ARAFAH_ASHURA_FAST, subKey = "ashura",
        )
        val arafahBody = WorshipReminderContent.body(
            context, WorshipReminderType.ARAFAH_ASHURA_FAST, subKey = "arafah",
        )
        val ashuraBody = WorshipReminderContent.body(
            context, WorshipReminderType.ARAFAH_ASHURA_FAST, subKey = "ashura",
        )

        assertThat(arafahTitle).isNotEqualTo(ashuraTitle)
        assertThat(arafahBody).isNotEqualTo(ashuraBody)
    }

    @Test
    fun `arafah is the default when no subKey is given`() {
        assertThat(WorshipReminderContent.body(context, WorshipReminderType.ARAFAH_ASHURA_FAST))
            .isEqualTo(
                WorshipReminderContent.body(
                    context, WorshipReminderType.ARAFAH_ASHURA_FAST, subKey = "arafah",
                )
            )
    }

    @Test
    fun `a reminder with no variants ignores a subKey rather than changing copy`() {
        // Tahajjud has one title. Passing a subKey must fall to the `else` arm, not to a
        // Mon/Thu-shaped branch that happens to compile.
        assertThat(
            WorshipReminderContent.title(context, WorshipReminderType.TAHAJJUD, subKey = "thursday")
        ).isEqualTo(WorshipReminderContent.title(context, WorshipReminderType.TAHAJJUD))
    }

    @Test
    fun `the StringProvider overloads say exactly what the Context overloads say`() {
        // The two paths exist so a ViewModel need not hold a Context, and the whole risk is that
        // they drift. Comparing them against the same resources is the only assertion that keeps
        // both `when`s honest as reminder types are added.
        WorshipReminderType.entries.forEach { type ->
            assertThat(WorshipReminderContent.name(strings, type))
                .isEqualTo(WorshipReminderContent.name(context, type))
            assertThat(WorshipReminderContent.arabic(strings, type))
                .isEqualTo(WorshipReminderContent.arabic(context, type))
            listOf(null, "monday", "thursday", "arafah", "ashura").forEach { subKey ->
                assertThat(WorshipReminderContent.title(strings, type, subKey))
                    .isEqualTo(WorshipReminderContent.title(context, type, subKey))
                assertThat(WorshipReminderContent.body(strings, type, subKey))
                    .isEqualTo(WorshipReminderContent.body(context, type, subKey))
            }
        }
    }

    @Test
    fun `nameRes is the id behind name, so a caller interpolating it gets the same word`() {
        // Public precisely so More's night-worship subtitle does not keep a fourth copy of the
        // `when`. If the two ever diverged, the menu and the reminder list would disagree.
        WorshipReminderType.entries.forEach { type ->
            assertThat(context.getString(WorshipReminderContent.nameRes(type)))
                .isEqualTo(WorshipReminderContent.name(context, type))
        }
    }
}
