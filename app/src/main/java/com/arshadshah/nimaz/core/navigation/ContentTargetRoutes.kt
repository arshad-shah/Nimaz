package com.arshadshah.nimaz.core.navigation

import com.arshadshah.nimaz.domain.model.ContentTarget

/**
 * Maps a domain [ContentTarget] onto the navigation destination that shows it.
 *
 * This is the whole reason [ContentTarget] exists: domain says *which verse* or *which hadith*,
 * and the arrow that turns that into a [Route] points inward, from navigation to domain, not the
 * other way round. Call it at the presentation edge (`NavGraph`) — a screen takes the
 * [ContentTarget] and hands it back, so screens stay free of the route graph too.
 */
fun ContentTarget.toRoute(): Route = when (this) {
    is ContentTarget.Ayah -> Route.QuranReader(surah, ayah)
    is ContentTarget.Hadith -> Route.HadithReader(id)
}
