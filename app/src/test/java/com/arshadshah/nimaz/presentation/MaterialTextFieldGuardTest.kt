package com.arshadshah.nimaz.presentation

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
 * The exceptions below are the field family's own implementations — the components that are
 * *supposed* to own a `BasicTextField`. `TextField` and `OutlinedTextField` have no exceptions
 * at all: nothing in the app should be reaching for Material's decorated fields.
 */
class MaterialTextFieldGuardTest {

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
    fun `no presentation source uses a Material text field primitive`() {
        val dir = File("src/main/java/com/arshadshah/nimaz/presentation")
        assert(dir.isDirectory) { "Presentation source dir not found at ${dir.absolutePath}" }

        // `(?<![A-Za-z0-9_.])` so `NimazTextField(` and `OutlinedTextFieldDefaults` do not
        // register as the Material primitives they are named after.
        val forbidden = mapOf(
            "OutlinedTextField" to Regex("""(?<![A-Za-z0-9_.])OutlinedTextField\s*\("""),
            "TextField" to Regex("""(?<![A-Za-z0-9_.])TextField\s*\("""),
            "BasicTextField" to Regex("""(?<![A-Za-z0-9_.])BasicTextField\s*\("""),
        )

        val offenders = mutableListOf<String>()
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
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
        val source = File(
            "src/main/java/com/arshadshah/nimaz/presentation/components/molecules/" +
                "NimazTextField.kt"
        )
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
