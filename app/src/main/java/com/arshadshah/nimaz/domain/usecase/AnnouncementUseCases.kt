package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementAction
import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AnnouncementUseCases(
    val observeActiveAnnouncement: ObserveActiveAnnouncementUseCase,
    val setAnnouncement: SetAnnouncementUseCase,
    val dismissAnnouncement: DismissAnnouncementUseCase,
    val resolveAnnouncementRoute: ResolveAnnouncementRouteUseCase,
)

/**
 * Emits the current announcement only if it is not dismissed (repository
 * concern), not expired, and the installed [currentVersionCode] falls inside
 * its `[minVersionCode, maxVersionCode]` window — otherwise emits null.
 */
class ObserveActiveAnnouncementUseCase(
    private val repository: AnnouncementRepository,
    private val currentVersionCode: Int,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    operator fun invoke(): Flow<Announcement?> =
        repository.observeCurrentAnnouncement().map { announcement ->
            announcement?.takeIf { it.isActiveFor(currentVersionCode, nowMillis()) }
        }
}

class SetAnnouncementUseCase(private val repository: AnnouncementRepository) {
    suspend operator fun invoke(announcement: Announcement) =
        repository.setAnnouncement(announcement)
}

class DismissAnnouncementUseCase(private val repository: AnnouncementRepository) {
    suspend operator fun invoke(id: String) = repository.dismiss(id)
}

/**
 * Classifies an announcement's `route` payload value into a validated
 * [AnnouncementAction]. Feature keys are checked against the navigation
 * allowlist via the injected [isKnownFeatureKey] predicate (wired to
 * `announcementRoute(key) != null` in `core/navigation`) so old app versions
 * safely no-op on keys they don't know.
 *
 * The predicate is a `(String) -> Boolean` rather than a resolver returning a
 * navigation destination: validating the key is a domain concern, naming the
 * destination is not, and nothing outside domain ever read the resolved value —
 * the navigation edge resolves the key itself.
 */
class ResolveAnnouncementRouteUseCase(
    private val isKnownFeatureKey: (String) -> Boolean,
) {
    operator fun invoke(route: String?): AnnouncementAction {
        val value = route?.trim().orEmpty()
        return when {
            value.isEmpty() -> AnnouncementAction.None
            value.startsWith("https://") -> AnnouncementAction.OpenUrl(value)
            isKnownFeatureKey(value) -> AnnouncementAction.NavigateToFeature(value)
            else -> AnnouncementAction.None
        }
    }
}
