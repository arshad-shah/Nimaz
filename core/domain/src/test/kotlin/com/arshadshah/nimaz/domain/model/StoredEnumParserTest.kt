package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * Every `fromString` / `fromKey` / `fromId` parser in `domain/model`, held to two properties.
 *
 * These parsers are the boundary between what is on a device's disk and what the app believes.
 * Thirty-three of them decode a persisted string back into an enum, each one hand-written as a
 * `when` block, and **a missing branch is silent**: the `else` arm returns a plausible default,
 * so a preference the user set reads back as something else and nothing anywhere fails. The KDoc
 * on `RescheduleNotificationsUseCase` records exactly this shipping in `HighLatitudeRule`.
 *
 * So:
 *
 * 1. **Round trip.** Whatever the app writes for an entry must parse back to that same entry.
 *    That is the property a missing `when` branch breaks, and it is checked for every entry of
 *    every enum rather than for a sampled few.
 * 2. **Unknown input.** A value from a newer build, or a corrupted row, must land on the
 *    documented fallback rather than throw — and where the fallback is `null`, it must actually
 *    be `null` rather than a wrong-but-valid enum.
 *
 * [`every parser in domain model is covered here`] keeps the list honest: it reads the source
 * tree and fails when a parser exists that this file does not name. Adding a parser without a
 * test is a red build, not an omission nobody notices.
 */
class StoredEnumParserTest {

    /**
     * One parser under test, with each entry already paired to the string the app persists for
     * it — usually `name`, but `key`, `id`, `wire` and `name.lowercase()` all appear, and naming
     * the wrong one would make the round trip assert nothing.
     */
    private class Parser private constructor(
        val label: String,
        /** Each entry paired with the exact string the app writes for it. */
        val storedForms: List<Pair<String, Any>>,
        val parse: (String) -> Any?,
        /** What an unrecognised stored value must decode to. `null` is a valid answer. */
        val onUnknown: Any?,
        /** What `null` must decode to, or `NOT_NULLABLE` when the parser does not take one. */
        val onNull: Any?,
        val parseNullable: ((String?) -> Any?)?,
    ) {
        companion object {
            /**
             * [serialise] is applied here, while the entry type is still known, so the table can
             * hold parsers over different enums without an unchecked cast at assertion time.
             */
            operator fun <E : Enum<E>> invoke(
                label: String,
                entries: List<E>,
                serialise: (E) -> String,
                parse: (String) -> Any?,
                onUnknown: Any?,
                onNull: Any? = NOT_NULLABLE,
                parseNullable: ((String?) -> Any?)? = null,
            ) = Parser(
                label = label,
                storedForms = entries.map { serialise(it) to (it as Any) },
                parse = parse,
                onUnknown = onUnknown,
                onNull = onNull,
                parseNullable = parseNullable,
            )
        }
    }

    private companion object {
        /** Distinguishes "the parser rejects null" from "the parser maps null to null". */
        val NOT_NULLABLE = Any()
        const val UNKNOWN_INPUT = "a_value_from_a_newer_build"
    }

    private val parsers: List<Parser> = listOf(
        Parser("PrayerName", PrayerName.entries, { it.name }, { PrayerName.fromString(it) }, PrayerName.FAJR),
        Parser("PrayerStatus", PrayerStatus.entries, { it.name }, { PrayerStatus.fromString(it) }, PrayerStatus.PENDING),
        Parser(
            "CalculationMethod", CalculationMethod.entries, { it.name }, { CalculationMethod.fromString(it) },
            CalculationMethod.MUSLIM_WORLD_LEAGUE, CalculationMethod.MUSLIM_WORLD_LEAGUE, { CalculationMethod.fromString(it) },
        ),
        Parser(
            "AsrCalculation", AsrCalculation.entries, { it.name }, { AsrCalculation.fromString(it) },
            AsrCalculation.STANDARD, AsrCalculation.STANDARD, { AsrCalculation.fromString(it) },
        ),
        // The one that shipped a bug. Its fallback is null rather than a rule, precisely so a
        // value it does not understand cannot quietly change when alarms fire.
        Parser(
            "HighLatitudeRule", HighLatitudeRule.entries, { it.name }, { HighLatitudeRule.fromString(it) },
            null, null, { HighLatitudeRule.fromString(it) },
        ),
        Parser("FastType", FastType.entries, { it.name }, { FastType.fromString(it) }, FastType.VOLUNTARY),
        Parser("FastStatus", FastStatus.entries, { it.name }, { FastStatus.fromString(it) }, FastStatus.NOT_FASTED),
        Parser(
            "ExemptionReason", ExemptionReason.entries, { it.name }, { ExemptionReason.fromString(it) },
            null, null, { ExemptionReason.fromString(it) },
        ),
        Parser("MakeupFastStatus", MakeupFastStatus.entries, { it.name }, { MakeupFastStatus.fromString(it) }, MakeupFastStatus.PENDING),
        // Persists through toDbString(), not name — the only enum here that does.
        Parser("KhatamStatus", KhatamStatus.entries, { it.toDbString() }, { KhatamStatus.fromString(it) }, KhatamStatus.ACTIVE),
        Parser("TokenType", TokenType.entries, { it.name }, { TokenType.fromString(it) }, TokenType.LETTER),
        Parser("LineType", LineType.entries, { it.name }, { LineType.fromString(it) }, LineType.EXAMPLE),
        Parser("MakhrajArea", MakhrajArea.entries, { it.name }, { MakhrajArea.fromString(it) }, MakhrajArea.JAWF),
        Parser("LessonStatus", LessonStatus.entries, { it.name }, { LessonStatus.fromString(it) }, LessonStatus.LOCKED),
        Parser(
            "DuaOccasion", DuaOccasion.entries, { it.name }, { DuaOccasion.fromString(it) },
            null, null, { DuaOccasion.fromString(it) },
        ),
        Parser(
            "WorshipReminderType", WorshipReminderType.entries, { it.key }, { WorshipReminderType.fromKey(it) },
            null, null, { WorshipReminderType.fromKey(it) },
        ),
        Parser("MushafLineType", MushafLineType.entries, { it.name }, { MushafLineType.fromString(it) }, MushafLineType.AYAH),
        Parser(
            "MushafScript", MushafScript.entries, { it.name }, { MushafScript.fromName(it) },
            MushafScript.DEFAULT, MushafScript.DEFAULT, { MushafScript.fromName(it) },
        ),
        Parser("RevelationType", RevelationType.entries, { it.name }, { RevelationType.fromString(it) }, RevelationType.MECCAN),
        Parser(
            "SajdaType", SajdaType.entries, { it.name }, { SajdaType.fromString(it) },
            null, null, { SajdaType.fromString(it) },
        ),
        Parser("SurahOverviewGroup", SurahOverviewGroup.entries, { it.wire }, { SurahOverviewGroup.fromWire(it) }, SurahOverviewGroup.OTHER),
        Parser(
            "TopicTree", TopicTree.entries, { it.wire }, { TopicTree.fromWire(it) },
            TopicTree.DEFAULT, TopicTree.DEFAULT, { TopicTree.fromWire(it) },
        ),
        Parser("PinnedShortcut", PinnedShortcut.entries, { it.key }, { PinnedShortcut.fromKey(it) }, null),
        Parser(
            "NisabType", NisabType.entries, { it.name }, { NisabType.fromName(it) },
            NisabType.DEFAULT, NisabType.DEFAULT, { NisabType.fromName(it) },
        ),
        Parser(
            "PrayerAlertStyle", PrayerAlertStyle.entries, { it.name }, { PrayerAlertStyle.fromStorage(it) },
            PrayerAlertStyle.NOTIFICATION, PrayerAlertStyle.NOTIFICATION, { PrayerAlertStyle.fromStorage(it) },
        ),
        Parser(
            "TasbihCategory", TasbihCategory.entries, { it.name }, { TasbihCategory.fromString(it) },
            null, null, { TasbihCategory.fromString(it) },
        ),
        // Asymmetric on purpose: a grade it cannot read is UNKNOWN (the hadith exists, its grading
        // does not parse), but an absent grade is null (nothing was recorded).
        Parser(
            "HadithGrade", HadithGrade.entries, { it.name }, { HadithGrade.fromString(it) },
            HadithGrade.UNKNOWN, null, { HadithGrade.fromString(it) },
        ),
        Parser("IslamicEventType", IslamicEventType.entries, { it.name }, { IslamicEventType.fromString(it) }, IslamicEventType.HISTORICAL),
        Parser(
            "AnnouncementType", AnnouncementType.entries, { it.key }, { AnnouncementType.fromKey(it) },
            null, null, { AnnouncementType.fromKey(it) },
        ),
        Parser(
            "CelebrationEvent", CelebrationEvent.entries, { it.key }, { CelebrationEvent.fromKey(it) },
            CelebrationEvent.GENERIC, CelebrationEvent.GENERIC, { CelebrationEvent.fromKey(it) },
        ),
        Parser(
            "QuranTranslation", QuranTranslation.entries, { it.id }, { QuranTranslation.fromId(it) },
            QuranTranslation.DEFAULT, QuranTranslation.DEFAULT, { QuranTranslation.fromId(it) },
        ),
        Parser(
            "QuranReciter", QuranReciter.entries, { it.id }, { QuranReciter.fromId(it) },
            QuranReciter.DEFAULT, QuranReciter.DEFAULT, { QuranReciter.fromId(it) },
        ),
    )

    @Test
    fun `every entry parses back from what the app persists`() {
        parsers.forEach { parser ->
            parser.storedForms.forEach { (stored, entry) ->
                assertWithMessage(
                    "${parser.label}: a stored \"$stored\" no longer decodes to $entry. " +
                        "A `when` branch is missing, so this value reads back as something else " +
                        "on every device that already has it."
                ).that(parser.parse(stored)).isEqualTo(entry)
            }
        }
    }

    @Test
    fun `an unrecognised stored value lands on the documented fallback`() {
        parsers.forEach { parser ->
            assertWithMessage("${parser.label}: unknown input")
                .that(parser.parse(UNKNOWN_INPUT))
                .isEqualTo(parser.onUnknown)
        }
    }

    @Test
    fun `a nullable parser maps null to its documented default, never to a wrong value`() {
        val nullable = parsers.filter { it.onNull !== NOT_NULLABLE }
        // Not a coincidence worth asserting loosely: more than half of these parsers take a
        // nullable string because the preference they read may never have been written.
        assertThat(nullable).isNotEmpty()

        nullable.forEach { parser ->
            val parse = requireNotNull(parser.parseNullable) {
                "${parser.label} declares a null behaviour but no nullable parse lambda"
            }
            assertWithMessage("${parser.label}: null input")
                .that(parse(null))
                .isEqualTo(parser.onNull)
        }
    }

    @Test
    fun `HijriMonth round-trips through its number`() {
        // The one parser here keyed on an Int rather than a String, so it sits outside the table.
        HijriMonth.entries.forEach { month ->
            assertThat(HijriMonth.fromNumber(month.number)).isEqualTo(month)
        }
        assertThat(HijriMonth.fromNumber(0)).isNull()
        assertThat(HijriMonth.fromNumber(13)).isNull()
    }

    @Test
    fun `every parser in domain model is covered here`() {
        // CWD for :core:domain:test is the module directory.
        val modelDir = File("src/main/kotlin/com/arshadshah/nimaz/domain/model")
        assertWithMessage("domain/model not found from ${File(".").absolutePath}")
            .that(modelDir.isDirectory).isTrue()

        val declaration = Regex("""fun (from[A-Z]\w*)\(""")
        val enclosingEnum = Regex("""enum class (\w+)""")

        val found = modelDir.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            val lines = file.readLines()
            lines.withIndex().mapNotNull { (index, line) ->
                declaration.find(line)?.let {
                    // The enum a companion parser belongs to is the last one declared above it.
                    val owner = lines.take(index).lastOrNull(enclosingEnum::containsMatchIn)
                        ?.let { d -> enclosingEnum.find(d)!!.groupValues[1] }
                    owner ?: "${file.name}:${index + 1}"
                }
            }
        }.toSortedSet()

        val covered = (parsers.map { it.label } + "HijriMonth").toSortedSet()
        assertWithMessage(
            "These parsers in domain/model have no round-trip coverage. Add them to " +
                "StoredEnumParserTest — a `when` branch that silently returns the wrong default " +
                "is the failure mode this file exists to catch."
        ).that(found - covered).isEmpty()
    }
}
