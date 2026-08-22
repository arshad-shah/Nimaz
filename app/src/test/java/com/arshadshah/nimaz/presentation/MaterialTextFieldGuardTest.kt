package com.arshadshah.nimaz.presentation

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import org.junit.Test
import java.io.File

/**
 * Regression guard: no screen builds its own text field out of a Material primitive.
 *
 * The app had a `NimazDropdownField` and a `NimazAmountInput` but no text field, so twelve call
 * sites reached for `OutlinedTextField` — and each one settled the same questions differently.
 * `AddPresetScreen` hand-set a 14dp radius on four fields and styled the Arabic one inline with
 * a `textStyle` *and* an `OutlinedTextFieldDefaults.colors` block. `KhatamFormScreen` put a
 * notched-border floating-label field directly above a label-above-an-outlined-card dropdown.
 * Three sites set `isError` with no message to go with it.
 *
 * A design system that has the component and still allows the primitive gets the drift back one
 * screen at a time, so the primitive is fenced off here rather than only discouraged in review.
 *
 * ## Two source roots, and a floor
 *
 * The design system moved to `:core:ui` in PR 10 of #551 while the screens stayed here, so the
 * rule now spans two modules and the scan has to as well. It very nearly did not: the first test
 * asserts only that its directory *exists*, and `app/.../presentation` still does — it holds the
 * screens and ViewModels. The guard would have gone on passing while silently checking a fraction
 * of what it used to, which is the exact failure #553 added scan floors to `check_docs.py` to
 * prevent. [MINIMUM_FILES] is that floor.
 *
 * The exceptions below are the field family's own implementations — the components that are
 * *supposed* to own a `BasicTextField`. `TextField` and `OutlinedTextField` have no exceptions
 * at all: nothing in the app should be reaching for Material's decorated fields.
 */
class MaterialTextFieldGuardTest {

    private companion object {
        /**
         * Both halves of the presentation layer. CWD for a module's unit tests is the module
         * directory, so `:core:ui` is reached as a sibling. Note the differing source roots:
         * `:app` is still `src/main/java`, the `:core:*` modules are `src/main/kotlin`.
         */
        /**
         * Every module's presentation sources — the fifth test to need this list, which is why it
         * lives in [PresentationSourceRoots] rather than here. Two roots were enough until the
         * feature modules started taking screens: by PR 19 of #551 the `:app` + `:core:ui` pair
         * had fallen below this test's own floor, which is exactly what the floor is for.
         */
        val PRESENTATION_ROOTS = PresentationSourceRoots.ALL

        /** Where the field family itself lives, now that the design system is its own module. */
        const val NIMAZ_TEXT_FIELD =
            "../core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/components/molecules/" +
                "NimazTextField.kt"

        /**
         * A floor, so a moved directory fails this test rather than quietly shrinking it. The two
         * roots hold well over 300 files between them; this sits far enough below that ordinary
         * churn never touches it, and far enough above that losing either root fails.
         */
        const val MINIMUM_FILES = 250
    }

    /** Files allowed to own a raw `BasicTextField` — the family's implementations. */
    private val basicTextFieldOwners = setOf(
        // The text field itself.
        "NimazTextField.kt",
        // The search member: an older, richer implementation of the same shell (clear button,
        // focus border, loading slot, the Ask pill).
        "NimazSearchBar.kt",
        // The stepper's editable value — a field inside a +/- control rather than a form field.
        "NimazNumberStepper.kt",
    )

    @Test
    fun `the scan reaches every presentation source root`() {
        PresentationSourceRoots.assertAllExist(PRESENTATION_ROOTS)

        val scanned = presentationSources().size
        assert(scanned >= MINIMUM_FILES) {
            "scanned only $scanned presentation files across $PRESENTATION_ROOTS — expected at " +
                "least $MINIMUM_FILES. A scan that finds nothing passes every assertion below."
        }
    }

    /**
     * The `.filter { it.isDirectory }` this used to carry was the silent-narrowing bug this epic
     * has now removed from four scans: a root that stops existing is skipped rather than reported,
     * so the guard keeps passing over less and less. [PresentationSourceRoots.assertAllExist]
     * fails instead.
     */
    private fun presentationSources(): List<File> =
        PresentationSourceRoots.sources(PRESENTATION_ROOTS)

    @Test
    fun `no presentation source uses a Material text field primitive`() {

        // `(?<![A-Za-z0-9_.])` so `NimazTextField(` and `OutlinedTextFieldDefaults` do not
        // register as the Material primitives they are named after.
        val forbidden = mapOf(
            "OutlinedTextField" to Regex("""(?<![A-Za-z0-9_.])OutlinedTextField\s*\("""),
            "TextField" to Regex("""(?<![A-Za-z0-9_.])TextField\s*\("""),
            "BasicTextField" to Regex("""(?<![A-Za-z0-9_.])BasicTextField\s*\("""),
        )

        val offenders = mutableListOf<String>()
        presentationSources().forEach { file ->
            val text = file.readText()
            forbidden.forEach { (name, pattern) ->
                if (name == "BasicTextField" && file.name in basicTextFieldOwners) return@forEach
                pattern.findAll(text).forEach { match ->
                    val line = text.substring(0, match.range.first).count { it == '\n' } + 1
                    offenders += "${file.name}:$line  $name("
                }
            }
        }

        assert(offenders.isEmpty()) {
            "Material text field primitive used outside the field family " +
                "(${offenders.size} site(s)). Use NimazTextField / NimazAmountField / " +
                "NimazSearchBar instead:\n" + offenders.joinToString("\n")
        }
    }

    /**
     * The escape hatches the field family deliberately does not have.
     *
     * `shape`, `colors` and `textStyle` on a field parameter list are how the call sites this
     * replaced ended up each choosing their own corner radius and their own Arabic styling. If
     * one of them reappears on [com.arshadshah.nimaz.presentation.components.molecules.NimazTextField],
     * the variant list is missing a member and the fix belongs there, not at the call site.
     */
    @Test
    fun `NimazTextField exposes no styling escape hatches`() {
        val source = File(NIMAZ_TEXT_FIELD)
        assert(source.isFile) { "NimazTextField.kt not found at ${source.absolutePath}" }

        val signature = source.readText()
            .substringAfter("fun NimazTextField(")
            .substringBefore("\n) {")

        val hatches = listOf("shape", "colors", "textStyle", "keyboardOptions")
            .filter { Regex("""^\s*$it\s*:""", RegexOption.MULTILINE).containsMatchIn(signature) }

        assert(hatches.isEmpty()) {
            "NimazTextField grew a styling escape hatch: ${hatches.joinToString()}. " +
                "Add a NimazFieldVariant instead."
        }
    }
}
