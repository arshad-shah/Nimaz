package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The thirteen shipped adhkar.
 *
 * Content, not logic — which is exactly why it needs a test rather than a review. Two
 * consumers in `TasbihRepositoryImpl` read this object and each breaks differently on a typo:
 *
 * - `initializeDefaultPresets()` writes every preset with its declared `id`, so two presets
 *   sharing an id means one silently replaces the other on a fresh install.
 * - `seedMissingDefaults()` filters by **name** against the rows already in the database, so a
 *   renamed preset is seeded a second time next to the row it was meant to be — a duplicate the
 *   user sees, on an upgrade, with no way to tell which is which.
 *
 * Neither is a compile error and neither shows up in a diff review of a 200-line data object.
 */
class DefaultTasbihPresetsTest {

    private val all = DefaultTasbihPresets.allDefaults

    @Test
    fun `all defaults is the two published lists and nothing else`() {
        assertThat(all).hasSize(
            DefaultTasbihPresets.baseDefaults.size + DefaultTasbihPresets.addedDefaults.size
        )
        assertThat(all).containsAtLeastElementsIn(DefaultTasbihPresets.baseDefaults)
        assertThat(all).containsAtLeastElementsIn(DefaultTasbihPresets.addedDefaults)
    }

    /**
     * The five in the prepackaged database, in order. Pinned because `seedMissingDefaults` would
     * re-seed any one of them that were moved into `addedDefaults` by mistake, giving every
     * upgrading install a duplicate.
     */
    @Test
    fun `the base five are the ones baked into the shipped database`() {
        assertThat(DefaultTasbihPresets.baseDefaults.map { it.name }).containsExactly(
            "SubhanAllah",
            "Alhamdulillah",
            "Allahu Akbar",
            "La ilaha illallah",
            "Astaghfirullah",
        ).inOrder()
    }

    @Test
    fun `no two presets share an id`() {
        val byId = all.groupBy { it.id }.filterValues { it.size > 1 }
        assertThat(byId.mapValues { (_, v) -> v.map { it.name } }).isEmpty()
    }

    /** The key `seedMissingDefaults` de-duplicates on. A collision here seeds nothing. */
    @Test
    fun `no two presets share a name`() {
        val byName = all.groupBy { it.name }.filterValues { it.size > 1 }
        assertThat(byName.keys).isEmpty()
    }

    @Test
    fun `no two presets share a display order`() {
        val byOrder = all.groupBy { it.displayOrder }.filterValues { it.size > 1 }
        assertThat(byOrder.mapValues { (_, v) -> v.map { it.name } }).isEmpty()
    }

    @Test
    fun `every preset is marked as a default`() {
        assertThat(all.filterNot { it.isDefault }.map { it.name }).isEmpty()
    }

    @Test
    fun `every preset counts up to something`() {
        assertThat(all.filter { it.targetCount <= 0 }.map { it.name }).isEmpty()
    }

    /**
     * `arabicText`, `transliteration` and `translation` are all nullable on [TasbihPreset] —
     * a user's own preset need not fill them in. A *shipped* one must, and null is the easy
     * omission when a fourteenth adhkar is added by copying a thirteenth.
     */
    @Test
    fun `no preset carries a blank or absent text field`() {
        val blank = all.filter {
            it.name.isBlank() ||
                it.arabicText.isNullOrBlank() ||
                it.transliteration.isNullOrBlank() ||
                it.translation.isNullOrBlank()
        }
        assertThat(blank.map { it.name }).isEmpty()
    }

    /**
     * The Arabic column really holds Arabic. A copy-paste that leaves the transliteration in
     * `arabicText` renders as Latin text in an Arabic-styled view — legible enough to survive a
     * screenshot review, wrong enough to matter.
     */
    @Test
    fun `every preset's arabic text is in Arabic script`() {
        val arabic = '؀'..'ۿ'
        val notArabic = all.filterNot { preset ->
            preset.arabicText.orEmpty().any { it in arabic }
        }
        assertThat(notArabic.map { it.name }).isEmpty()
    }

    @Test
    fun `no preset is filed under the custom category`() {
        // CUSTOM is what a user's own preset gets. A shipped one claiming it would appear in the
        // user's own list and be offered for deletion.
        assertThat(all.filter { it.category == TasbihCategory.CUSTOM }.map { it.name }).isEmpty()
    }

    @Test
    fun `the after-prayer tasbih adds up to the hundred it is meant to`() {
        val afterPrayer = DefaultTasbihPresets.baseDefaults
            .filter { it.category == TasbihCategory.AFTER_PRAYER }

        assertThat(afterPrayer.map { it.name })
            .containsExactly("SubhanAllah", "Alhamdulillah", "Allahu Akbar")
        assertThat(afterPrayer.sumOf { it.targetCount }).isEqualTo(100)
    }

    @Test
    fun `every preset cites a source`() {
        assertThat(all.filter { it.reference.isNullOrBlank() }.map { it.name }).isEmpty()
    }

    /**
     * `createdAt`/`updatedAt` are zero on purpose: these are constants, not rows, and a
     * `System.currentTimeMillis()` in a shipped default would make every install's data differ
     * and every equality check in a test flaky.
     */
    @Test
    fun `no preset carries a wall-clock timestamp`() {
        assertThat(all.filterNot { it.createdAt == 0L && it.updatedAt == 0L }.map { it.name })
            .isEmpty()
    }
}
