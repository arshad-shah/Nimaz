package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Every hand-written label a `domain/model` enum offers a screen, held to three properties.
 *
 * These functions exist because the alternative is each screen keeping its own copy — the KDoc on
 * `CalculationMethod.shortName` and `AsrCalculation.shortName` says so outright. Centralising
 * them makes one `when` block per enum the single source of a user-facing word, and a `when` over
 * eleven entries written by hand has exactly one interesting failure mode: **two arms returning
 * the same string.** The compiler is happy, every test that asserts on one label is happy, and
 * the settings picker ships with two rows a user cannot tell apart.
 *
 * The properties, then:
 *
 * 1. **Present** — no entry returns an empty label. Blank is worse than wrong: it is a row with
 *    nothing in it.
 * 2. **Distinct** — no two entries share one. This is the copy-paste failure above.
 * 3. **Total** — every entry is asked, so an arm that was never added is a failure here rather
 *    than a `NoWhenBranchMatchedException` in front of a user.
 *
 * The *content* of a label is deliberately not asserted. Wording is copy and changes without
 * being a defect; these three properties do not.
 *
 * `StoredEnumParserTest` is the other half of this pair, over the same enums: it pins what is
 * written to and read from disk, where a wrong answer is silent data loss rather than a
 * confusing picker.
 */
class EnumLabelTest {

    /** One label function, with the entries it must answer for. */
    private class Labels<E : Enum<E>>(
        val label: String,
        val entries: List<E>,
        val of: (E) -> String,
    )

    private val labelled: List<Labels<*>> = listOf(
        Labels("PrayerName.displayName", PrayerName.entries) { it.displayName() },
        Labels("CalculationMethod.displayName", CalculationMethod.entries) { it.displayName() },
        Labels("CalculationMethod.shortName", CalculationMethod.entries) { it.shortName() },
        Labels("AsrCalculation.displayName", AsrCalculation.entries) { it.displayName() },
        Labels("AsrCalculation.shortName", AsrCalculation.entries) { it.shortName() },
        Labels("HijriMonth.displayName", HijriMonth.entries) { it.displayName() },
        Labels("HijriMonth.arabicName", HijriMonth.entries) { it.arabicName() },
        Labels("HadithGrade.displayName", HadithGrade.entries) { it.displayName() },
        Labels("ExemptionReason.displayName", ExemptionReason.entries) { it.displayName() },
        Labels("TasbihCategory.displayName", TasbihCategory.entries) { it.displayName() },
        Labels("NisabType.displayName", NisabType.entries) { it.displayName() },
    )

    @Suppress("UNCHECKED_CAST")
    private fun <E : Enum<E>> Labels<*>.resolve(): List<Pair<String, String>> {
        val typed = this as Labels<E>
        return typed.entries.map { it.name to typed.of(it) }
    }

    @Test
    fun `every entry of every enum has a label`() {
        labelled.forEach { labels ->
            labels.resolve<Nothing>().forEach { (entry, text) ->
                assertWithMessage("${labels.label} for $entry")
                    .that(text)
                    .isNotEmpty()
            }
        }
    }

    @Test
    fun `no two entries of an enum answer with the same label`() {
        // The copy-paste failure: two rows in a picker a user cannot tell apart.
        labelled.forEach { labels ->
            val resolved = labels.resolve<Nothing>()
            assertWithMessage("${labels.label} has a duplicate among ${resolved.map { it.second }}")
                .that(resolved.map { it.second }.toSet())
                .hasSize(resolved.size)
        }
    }

    @Test
    fun `no label is left as the entry's own constant name`() {
        // `SOME_ENTRY` reaching a screen means the arm was never written and something fell
        // through to a `name`-based default.
        labelled.forEach { labels ->
            labels.resolve<Nothing>().forEach { (entry, text) ->
                assertWithMessage("${labels.label} for $entry")
                    .that(text)
                    .isNotEqualTo(entry)
            }
        }
    }

    // ---- The two that carry a number rather than a word ----

    @Test
    fun `each nisab threshold names a real weight, and gold's is the smaller one`() {
        // 7.5 tola of gold against 52.5 of silver. Swapping them does not fail anything; it
        // just tells a user they owe zakat when they do not, or the reverse.
        NisabType.entries.forEach { assertThat(it.weightInGrams()).isGreaterThan(0.0) }

        assertThat(NisabType.GOLD.weightInGrams()).isLessThan(NisabType.SILVER.weightInGrams())
    }

    @Test
    fun `each hijri month knows its own number, and the twelve run in order`() {
        assertThat(HijriMonth.entries.map { it.number }).isInOrder()
        assertThat(HijriMonth.entries.map { it.number }).containsExactlyElementsIn(1..12).inOrder()
    }

    @Test
    fun `a month number outside the calendar resolves to no month`() {
        assertThat(HijriMonth.fromNumber(0)).isNull()
        assertThat(HijriMonth.fromNumber(13)).isNull()
        assertThat(HijriMonth.fromNumber(-1)).isNull()
    }

    @Test
    fun `every month resolves from its own number`() {
        HijriMonth.entries.forEach {
            assertThat(HijriMonth.fromNumber(it.number)).isEqualTo(it)
        }
    }

    @Test
    fun `the arabic month names are actually arabic`() {
        // They are the one place the calendar prints Arabic without a font override, so a
        // transliteration slipping in here is silent.
        HijriMonth.entries.forEach {
            assertWithMessage("${it.name} arabicName")
                .that(it.arabicName().any { ch -> ch.code in 0x0600..0x06FF })
                .isTrue()
        }
    }
}
