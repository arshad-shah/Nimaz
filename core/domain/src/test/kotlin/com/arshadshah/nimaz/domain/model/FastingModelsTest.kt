package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FastingModelsTest {

    // ── FastType.fromString ─────────────────────────────────────────

    @Test
    fun `FastType fromString parses primary names`() {
        assertThat(FastType.fromString("ramadan")).isEqualTo(FastType.RAMADAN)
        assertThat(FastType.fromString("voluntary")).isEqualTo(FastType.VOLUNTARY)
        assertThat(FastType.fromString("makeup")).isEqualTo(FastType.MAKEUP)
        assertThat(FastType.fromString("expiation")).isEqualTo(FastType.EXPIATION)
        assertThat(FastType.fromString("vow")).isEqualTo(FastType.VOW)
    }

    @Test
    fun `FastType fromString parses aliases`() {
        assertThat(FastType.fromString("nafl")).isEqualTo(FastType.VOLUNTARY)
        assertThat(FastType.fromString("qada")).isEqualTo(FastType.MAKEUP)
        assertThat(FastType.fromString("kaffarah")).isEqualTo(FastType.EXPIATION)
        assertThat(FastType.fromString("nadhr")).isEqualTo(FastType.VOW)
    }

    @Test
    fun `FastType fromString is case insensitive`() {
        assertThat(FastType.fromString("RAMADAN")).isEqualTo(FastType.RAMADAN)
        assertThat(FastType.fromString("Ramadan")).isEqualTo(FastType.RAMADAN)
    }

    @Test
    fun `FastType fromString defaults to VOLUNTARY for unknown`() {
        assertThat(FastType.fromString("unknown")).isEqualTo(FastType.VOLUNTARY)
        assertThat(FastType.fromString("")).isEqualTo(FastType.VOLUNTARY)
    }

    @Test
    fun `FastType displayName returns readable names`() {
        assertThat(FastType.RAMADAN.displayName()).isEqualTo("Ramadan")
        assertThat(FastType.MAKEUP.displayName()).isEqualTo("Makeup (Qada)")
        assertThat(FastType.EXPIATION.displayName()).isEqualTo("Expiation (Kaffarah)")
        assertThat(FastType.VOW.displayName()).isEqualTo("Vow (Nadhr)")
    }

    // ── FastStatus.fromString ───────────────────────────────────────

    @Test
    fun `FastStatus fromString parses primary names`() {
        assertThat(FastStatus.fromString("fasted")).isEqualTo(FastStatus.FASTED)
        assertThat(FastStatus.fromString("not_fasted")).isEqualTo(FastStatus.NOT_FASTED)
        assertThat(FastStatus.fromString("exempted")).isEqualTo(FastStatus.EXEMPTED)
        assertThat(FastStatus.fromString("makeup_due")).isEqualTo(FastStatus.MAKEUP_DUE)
    }

    @Test
    fun `FastStatus fromString parses aliases`() {
        assertThat(FastStatus.fromString("notfasted")).isEqualTo(FastStatus.NOT_FASTED)
        assertThat(FastStatus.fromString("makeupdue")).isEqualTo(FastStatus.MAKEUP_DUE)
    }

    @Test
    fun `FastStatus fromString defaults to NOT_FASTED for unknown`() {
        assertThat(FastStatus.fromString("unknown")).isEqualTo(FastStatus.NOT_FASTED)
    }

    // ── ExemptionReason.fromString ──────────────────────────────────

    @Test
    fun `ExemptionReason fromString parses primary names`() {
        assertThat(ExemptionReason.fromString("travel")).isEqualTo(ExemptionReason.TRAVEL)
        assertThat(ExemptionReason.fromString("illness")).isEqualTo(ExemptionReason.ILLNESS)
        assertThat(ExemptionReason.fromString("menstruation")).isEqualTo(ExemptionReason.MENSTRUATION)
        assertThat(ExemptionReason.fromString("pregnancy")).isEqualTo(ExemptionReason.PREGNANCY)
        assertThat(ExemptionReason.fromString("breastfeeding")).isEqualTo(ExemptionReason.BREASTFEEDING)
        assertThat(ExemptionReason.fromString("elderly")).isEqualTo(ExemptionReason.ELDERLY)
        assertThat(ExemptionReason.fromString("other")).isEqualTo(ExemptionReason.OTHER)
    }

    @Test
    fun `ExemptionReason fromString parses aliases`() {
        assertThat(ExemptionReason.fromString("sick")).isEqualTo(ExemptionReason.ILLNESS)
        assertThat(ExemptionReason.fromString("period")).isEqualTo(ExemptionReason.MENSTRUATION)
        assertThat(ExemptionReason.fromString("old_age")).isEqualTo(ExemptionReason.ELDERLY)
    }

    @Test
    fun `ExemptionReason fromString returns null for null or unknown`() {
        assertThat(ExemptionReason.fromString(null)).isNull()
        assertThat(ExemptionReason.fromString("unknown")).isNull()
    }

    @Test
    fun `ExemptionReason displayName returns readable names`() {
        assertThat(ExemptionReason.TRAVEL.displayName()).isEqualTo("Travel")
        assertThat(ExemptionReason.ILLNESS.displayName()).isEqualTo("Illness")
    }

    // ── MakeupFastStatus.fromString ─────────────────────────────────

    @Test
    fun `MakeupFastStatus fromString parses primary names`() {
        assertThat(MakeupFastStatus.fromString("pending")).isEqualTo(MakeupFastStatus.PENDING)
        assertThat(MakeupFastStatus.fromString("completed")).isEqualTo(MakeupFastStatus.COMPLETED)
        assertThat(MakeupFastStatus.fromString("fidya_paid")).isEqualTo(MakeupFastStatus.FIDYA_PAID)
    }

    @Test
    fun `MakeupFastStatus fromString parses aliases`() {
        assertThat(MakeupFastStatus.fromString("fidyapaid")).isEqualTo(MakeupFastStatus.FIDYA_PAID)
    }

    @Test
    fun `MakeupFastStatus fromString defaults to PENDING for unknown`() {
        assertThat(MakeupFastStatus.fromString("unknown")).isEqualTo(MakeupFastStatus.PENDING)
    }

    // ── Data class construction ─────────────────────────────────────

    @Test
    fun `FastRecord can be constructed with all fields`() {
        val record = FastRecord(
            id = 1, date = 1000L, hijriDate = "1/9/1446",
            hijriMonth = 9, hijriYear = 1446,
            fastType = FastType.RAMADAN, status = FastStatus.FASTED,
            exemptionReason = null, suhoorTime = null, iftarTime = null,
            note = "test", createdAt = 1000L, updatedAt = 1000L
        )
        assertThat(record.id).isEqualTo(1)
        assertThat(record.fastType).isEqualTo(FastType.RAMADAN)
        assertThat(record.status).isEqualTo(FastStatus.FASTED)
    }

    @Test
    fun `MakeupFast can be constructed with all fields`() {
        val makeup = MakeupFast(
            id = 1, originalDate = 1000L, originalHijriDate = "1/9/1446",
            reason = "Travel", status = MakeupFastStatus.PENDING,
            completedDate = null, fidyaAmount = null,
            note = null, createdAt = 1000L, updatedAt = 1000L
        )
        assertThat(makeup.status).isEqualTo(MakeupFastStatus.PENDING)
    }

    @Test
    fun `FastingStats can be constructed with all fields`() {
        val stats = FastingStats(
            totalFasted = 20, ramadanFasted = 15, voluntaryFasted = 5,
            pendingMakeupCount = 3, totalFidyaPaid = 150.0,
            currentStreak = 7, startDate = 1000L, endDate = 2000L
        )
        assertThat(stats.totalFasted).isEqualTo(20)
        assertThat(stats.currentStreak).isEqualTo(7)
    }
}
