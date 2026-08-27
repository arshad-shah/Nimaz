package com.arshadshah.nimaz.core.monitoring

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The analytics catalogue, held to Firebase's own limits.
 *
 * This is the rare test whose failure mode is *silence*. Firebase drops an event whose name is
 * over 40 characters, a parameter whose name is over 40, or a user property whose name is over
 * 24 — it does not reject the call, log a warning, or fail a build. The event simply never
 * appears in any report, and the first anyone knows is a dashboard that reads zero for something
 * users do daily. `AppAnalytics`' own KDoc gives the catalogue as the reason these constants
 * exist ("so event and parameter names stay consistent across call sites and within Firebase's
 * naming limits") — this is what makes that claim true rather than aspirational.
 *
 * Duplicates are the other half. Two constants with the same value are two call sites reporting
 * into one bucket, which is worse than either being missing: the number is plausible and wrong.
 *
 * Read reflectively rather than listed, deliberately — a list would have to be updated by the
 * same person adding the constant, which is the step that gets skipped.
 */
class AppAnalyticsCatalogTest {

    /** Every `const val` on an `object`, as name-to-value. */
    private fun constantsOf(type: Class<*>): Map<String, String> =
        type.declaredFields
            .filter { it.type == String::class.java }
            .associate { field ->
                field.isAccessible = true
                field.name to (field.get(null) as String)
            }

    private val events = constantsOf(AppAnalytics.Event::class.java)
    private val params = constantsOf(AppAnalytics.Param::class.java)
    private val userProperties = constantsOf(AppAnalytics.UserProperty::class.java)

    @Test
    fun `the catalogue is not empty, so a reflection change cannot make this vacuous`() {
        assertThat(events).isNotEmpty()
        assertThat(params).isNotEmpty()
        assertThat(userProperties).isNotEmpty()
    }

    // ---- Firebase's hard limits ----

    @Test
    fun `no event name is long enough for Firebase to drop it`() {
        events.forEach { (constant, value) ->
            assertWithMessage("Event.$constant = '$value'")
                .that(value.length)
                .isAtMost(FIREBASE_EVENT_NAME_MAX)
        }
    }

    @Test
    fun `no parameter name is long enough for Firebase to drop it`() {
        params.forEach { (constant, value) ->
            assertWithMessage("Param.$constant = '$value'")
                .that(value.length)
                .isAtMost(FIREBASE_PARAM_NAME_MAX)
        }
    }

    @Test
    fun `no user property name is long enough for Firebase to drop it`() {
        // 24, not 40 — the tightest limit of the three, and the easiest to walk into.
        userProperties.forEach { (constant, value) ->
            assertWithMessage("UserProperty.$constant = '$value'")
                .that(value.length)
                .isAtMost(FIREBASE_USER_PROPERTY_NAME_MAX)
        }
    }

    // ---- Shape ----

    @Test
    fun `every name is snake_case, which is what Firebase accepts`() {
        (events + params + userProperties).forEach { (constant, value) ->
            assertWithMessage("'$value' (from $constant)")
                .that(value.matches(SNAKE_CASE))
                .isTrue()
        }
    }

    @Test
    fun `no name is blank`() {
        (events + params + userProperties).forEach { (constant, value) ->
            assertWithMessage(constant).that(value).isNotEmpty()
        }
    }

    // ---- Distinctness ----

    @Test
    fun `no two events report into the same bucket`() {
        // A plausible, wrong number is worse than a missing one.
        assertThat(events.values.toSet()).hasSize(events.size)
    }

    @Test
    fun `no two parameters share a name`() {
        assertThat(params.values.toSet()).hasSize(params.size)
    }

    @Test
    fun `no two user properties share a name`() {
        assertThat(userProperties.values.toSet()).hasSize(userProperties.size)
    }

    private companion object {
        const val FIREBASE_EVENT_NAME_MAX = 40
        const val FIREBASE_PARAM_NAME_MAX = 40
        const val FIREBASE_USER_PROPERTY_NAME_MAX = 24
        val SNAKE_CASE = Regex("^[a-z][a-z0-9_]*$")
    }
}
