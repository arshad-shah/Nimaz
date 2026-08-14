package com.arshadshah.nimaz.presentation.model

import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone

/**
 * What the UI shows for one prayer on one day.
 *
 * [NOT_RECORDED] is the reason this type exists. It is **not** a stored [PrayerStatus] and never
 * becomes one on its own: the app used to rewrite every unlogged past prayer to `missed` at
 * midnight, so a user who simply had not opened the app was told they had missed prayers, and
 * those fabricated rows fed the qada list. A prayer nobody logged is a prayer nobody logged.
 *
 * Lives in `presentation/model` (not `screens/prayer`) so that organisms such as
 * [com.arshadshah.nimaz.presentation.components.organisms.HomePrayerCard] can share the same
 * status pill atom without importing from a screen package.
 */
enum class PrayerDisplayStatus {
    PRAYED,
    LATE,
    QADA,
    MISSED,
    NOT_RECORDED,
    UPCOMING,
}

/** Whether the obligation was fulfilled — on time, late, or made up. */
fun PrayerDisplayStatus.isDone(): Boolean = when (this) {
    PrayerDisplayStatus.PRAYED, PrayerDisplayStatus.LATE, PrayerDisplayStatus.QADA -> true
    PrayerDisplayStatus.MISSED, PrayerDisplayStatus.NOT_RECORDED, PrayerDisplayStatus.UPCOMING -> false
}

/** The semantic colour this status paints in. */
fun PrayerDisplayStatus.tone(): NimazTone = when (this) {
    PrayerDisplayStatus.PRAYED -> NimazTone.SUCCESS
    PrayerDisplayStatus.LATE -> NimazTone.ACCENT
    PrayerDisplayStatus.QADA -> NimazTone.PROMINENT
    PrayerDisplayStatus.MISSED -> NimazTone.ERROR
    PrayerDisplayStatus.NOT_RECORDED -> NimazTone.WARNING
    PrayerDisplayStatus.UPCOMING -> NimazTone.MUTED
}

/**
 * Disc or ring.
 *
 * [NOT_RECORDED] is the only ring: a hollow marker is how the design system says "this is an
 * absence of information", which a filled dot in any colour cannot distinguish from a fact.
 */
fun PrayerDisplayStatus.dotStyle(): NimazStatusDotStyle = when (this) {
    PrayerDisplayStatus.NOT_RECORDED -> NimazStatusDotStyle.OUTLINED
    else -> NimazStatusDotStyle.FILLED
}

/**
 * Maps a stored [PrayerStatus] + whether the prayer time has passed to the richer UI-only
 * [PrayerDisplayStatus].  The separation exists because [NOT_RECORDED] has no backing store
 * value: a prayer nobody logged is distinct from one the app marked MISSED.
 */
fun PrayerStatus.toDisplayStatus(isPassed: Boolean): PrayerDisplayStatus = when (this) {
    PrayerStatus.PRAYED    -> PrayerDisplayStatus.PRAYED
    PrayerStatus.LATE      -> PrayerDisplayStatus.LATE
    PrayerStatus.QADA      -> PrayerDisplayStatus.QADA
    PrayerStatus.MISSED    -> PrayerDisplayStatus.MISSED
    PrayerStatus.PENDING,
    PrayerStatus.NOT_PRAYED -> if (isPassed) PrayerDisplayStatus.NOT_RECORDED
                               else          PrayerDisplayStatus.UPCOMING
}
