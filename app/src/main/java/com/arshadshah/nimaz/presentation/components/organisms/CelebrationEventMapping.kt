package com.arshadshah.nimaz.presentation.components.organisms

import com.arshadshah.nimaz.domain.model.CelebrationEvent

/** Maps a domain occasion to its presentation styling occasion. */
fun CelebrationEvent.toOccasion(): EventOccasion = when (this) {
    CelebrationEvent.EID_AL_FITR -> EventOccasion.EID_AL_FITR
    CelebrationEvent.EID_AL_ADHA -> EventOccasion.EID_AL_ADHA
    CelebrationEvent.RAMADAN_START, CelebrationEvent.RAMADAN_END -> EventOccasion.RAMADAN
    CelebrationEvent.LAYLAT_AL_QADR -> EventOccasion.LAYLAT_AL_QADR
    CelebrationEvent.ARAFAH -> EventOccasion.ARAFAH
    CelebrationEvent.ASHURA -> EventOccasion.ASHURA
    CelebrationEvent.MAWLID -> EventOccasion.MAWLID
    CelebrationEvent.HIJRI_NEW_YEAR -> EventOccasion.HIJRI_NEW_YEAR
    CelebrationEvent.JUMUAH -> EventOccasion.JUMUAH
    CelebrationEvent.GENERIC -> EventOccasion.GENERIC
}
