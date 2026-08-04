package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.domain.model.Prophet
import com.arshadshah.nimaz.domain.usecase.ProphetUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The prophets, as a catalogue. Its search also covers the title and the era. */
typealias ProphetListState = CatalogListState<Prophet>
typealias ProphetDetailState = CatalogDetailState<Prophet>

@HiltViewModel
class ProphetViewModel @Inject constructor(
    useCases: ProphetUseCases,
    telemetry: Telemetry,
) : CatalogViewModel<Prophet>(
    source = ProphetSource(useCases),
    telemetry = telemetry,
    feature = AppAnalytics.Feature.PROPHET,
)

private class ProphetSource(private val useCases: ProphetUseCases) : CatalogSource<Prophet> {
    override fun all(): Flow<List<Prophet>> = useCases.getAllProphets()
    override fun favourites(): Flow<List<Prophet>> = useCases.getFavorites()
    override suspend fun byId(id: Int): Prophet? = useCases.getProphetById(id)
    override suspend fun toggleFavourite(id: Int) { useCases.toggleFavorite(id) }
    override fun idOf(item: Prophet): Int = item.id

    override fun matches(item: Prophet, query: String): Boolean =
        item.nameArabic.contains(query, ignoreCase = true) ||
            item.nameEnglish.contains(query, ignoreCase = true) ||
            item.nameTransliteration.contains(query, ignoreCase = true) ||
            item.titleEnglish.contains(query, ignoreCase = true) ||
            item.era.contains(query, ignoreCase = true)
}
