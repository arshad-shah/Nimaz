package com.arshadshah.nimaz.presentation

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The presentation layer reaches monitoring through the injected [Telemetry] seam, never
 * through the `AppAnalytics` / `CrashReporter` objects.
 *
 * `ARCHITECTURE.md` §6.1 has said so since the seam was introduced — *"Calling `AppAnalytics.*`
 * or `CrashReporter.*` directly from a ViewModel is a deviation"* — and nothing checked it.
 * When this test was written the rule was being broken at **22 call sites in six files**, which
 * is roughly what an unenforced convention is worth after a few months.
 *
 * It matters for one concrete reason rather than tidiness: those objects hold a static
 * `Context` and no-op when Firebase is absent, so a unit test cannot observe them. Every call
 * that goes around the seam is an event no test can assert was sent — and this codebase has
 * already shipped two analytics defects that survived precisely that blind spot: an onboarding
 * funnel that fired zero times, and a `logFeatureUsed` on a branch no screen could reach. Both
 * are repaired now — the point is how long each lasted while unassertable.
 *
 * ## What is allowed
 *
 * The **catalog** is not a call. `AppAnalytics.Feature.QURAN` and `AppAnalytics.Action.OPEN_DETAIL`
 * are constants, and §6.1 requires passing them rather than string literals, so a scan that
 * flagged them would be telling callers to violate the neighbouring rule. Only an invocation —
 * `AppAnalytics.something(` with a lower-case initial — is a deviation.
 *
 * `PerfMonitor` is deliberately **not** on the forbidden list even though it is the same shape
 * of object: the seam now carries `Telemetry.trace`, so presentation code has a route, but the
 * object remains legitimate for the `:app` entry points that have no injection point.
 *
 * `:feature:widget` is out of scope for the same reason, and finding that out is why this scan
 * is worth having. It reported eleven `CrashReporter` calls in the widget module that a
 * by-hand classification had waved through as "workers". They are not workers — they are a
 * Glance `object`, an abstract `GlanceStateDefinition`, and `GlanceAppWidget` subclasses that
 * the framework instantiates from a manifest receiver. **None declares `@Inject`**, checked
 * rather than assumed, so none has a constructor the seam could arrive through: precisely the
 * "callers with no injection point" §6.1 exempts. Scoping the guard is the correct answer here;
 * had even one of them been `@Inject`-constructed, migrating it would have been.
 */
class MonitoringSeamGuardTest {

    private companion object {
        /**
         * An invocation, not a constant reference: `AppAnalytics.logEvent(`, never
         * `AppAnalytics.Event.SCREEN_VIEW`. Kotlin members are lower-camel and the catalog
         * objects are upper-camel, so the initial character separates them cleanly, and the
         * `(` requires an actual call rather than a function reference in a doc comment.
         */
        val FORBIDDEN = Regex("""\b(AppAnalytics|CrashReporter)\.([a-z]\w*)\s*\(""")

        /**
         * Files that legitimately hold the production wiring. §6.1 keeps the objects "as the
         * production binding and for callers with no injection point", and inside the
         * presentation roots that is exactly one file.
         */
        val ALLOWED = setOf("FirebaseTelemetry.kt")

        /**
         * The scan has to see real presentation code, not an empty list.
         *
         * **400 against a measured 488.** The first draft of this constant said 700, on the
         * reasoning that fourteen roots "hold well over a thousand files". They hold 488, and
         * the floor failed the build on its very first run — catching the guess rather than a
         * defect, which is the cheapest possible way to learn that a floor was invented instead
         * of measured. Re-derive it (`PresentationSourceRoots.sources().size`) rather than
         * nudging it when it next trips.
         *
         * This project has repeatedly shipped guards that passed because they scanned nothing —
         * `MaterialTextFieldGuardTest` asserted only that its directory *existed*, and
         * `AnalyticsReachabilityTest` filtered missing roots away and scanned a directory that
         * had never been there. The floor is the difference between "found no violations" and
         * "looked nowhere".
         */
        const val MINIMUM_FILES = 400

        /**
         * The roots where an injection point exists — [PresentationSourceRoots.ALL] minus
         * `:feature:widget`, per the class KDoc.
         *
         * Derived by subtraction and then size-checked, so the exemption cannot quietly grow:
         * excluding a second module to make a red build green is the way a guard stops guarding,
         * and this epic has fifteen instances of that failure to learn from. If this assertion
         * fails, the question is which module lost its injection points — not what number makes
         * it pass.
         */
        val SEAM_SCOPED: List<String> =
            PresentationSourceRoots.ALL.filterNot { it.contains("/feature/widget/") }
    }

    /** Comments are blanked first: a §6.1 quotation in KDoc is documentation, not a call. */
    private fun String.withoutComments(): String =
        replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("""//.*"""), "")

    @Test
    fun `presentation code reaches monitoring only through the Telemetry seam`() {
        PresentationSourceRoots.assertAllExist(PresentationSourceRoots.ALL)
        assertThat(SEAM_SCOPED).hasSize(PresentationSourceRoots.ALL.size - 1)

        val files = PresentationSourceRoots.sources(SEAM_SCOPED)
        assertThat(files.size).isAtLeast(MINIMUM_FILES)

        val offenders = files
            .filterNot { it.name in ALLOWED }
            .mapNotNull { file ->
                val hits = FORBIDDEN.findAll(file.readText().withoutComments())
                    .map { it.groupValues[1] + "." + it.groupValues[2] + "()" }
                    .toList()
                if (hits.isEmpty()) null else "${file.path}: ${hits.joinToString()}"
            }

        assertThat(offenders).isEmpty()
    }
}
