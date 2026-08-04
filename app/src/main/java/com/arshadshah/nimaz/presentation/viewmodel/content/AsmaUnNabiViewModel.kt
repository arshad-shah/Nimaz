package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.usecase.AsmaUnNabiUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The names of the Prophet ﷺ, as a catalogue. */
typealias AsmaUnNabiListState = CatalogListState<AsmaUnNabi>
typealias AsmaUnNabiDetailState = CatalogDetailState<AsmaUnNabi>

@HiltViewModel
class AsmaUnNabiViewModel @Inject constructor(
    useCases: AsmaUnNabiUseCases,
    telemetry: Telemetry,
) : CatalogViewModel<AsmaUnNabi>(
    source = AsmaUnNabiSource(useCases),
    telemetry = telemetry,
    feature = AppAnalytics.Feature.ASMA_UN_NABI,
)

private class AsmaUnNabiSource(private val useCases: AsmaUnNabiUseCases) : CatalogSource<AsmaUnNabi> {
    override fun all(): Flow<List<AsmaUnNabi>> = useCases.getAllNames()
    override fun favourites(): Flow<List<AsmaUnNabi>> = useCases.getFavorites()
    override suspend fun byId(id: Int): AsmaUnNabi? = useCases.getNameById(id)
    override suspend fun toggleFavourite(id: Int) { useCases.toggleFavorite(id) }
    override fun idOf(item: AsmaUnNabi): Int = item.id

    override fun matches(item: AsmaUnNabi, query: String): Boolean =
        item.nameArabic.contains(query, ignoreCase = true) ||
            item.nameTransliteration.contains(query, ignoreCase = true) ||
            item.nameEnglish.contains(query, ignoreCase = true) ||
            item.meaning.contains(query, ignoreCase = true)
}
