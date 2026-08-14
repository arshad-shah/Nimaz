package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.usecase.AsmaUlHusnaUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The ninety-nine names, as a catalogue. See CatalogViewModel for why this is four lines. */
typealias AsmaUlHusnaListState = CatalogListState<AsmaUlHusna>
typealias AsmaUlHusnaDetailState = CatalogDetailState<AsmaUlHusna>

@HiltViewModel
class AsmaUlHusnaViewModel @Inject constructor(
    useCases: AsmaUlHusnaUseCases,
    telemetry: Telemetry,
) : CatalogViewModel<AsmaUlHusna>(
    source = AsmaUlHusnaSource(useCases),
    telemetry = telemetry,
    feature = AppAnalytics.Feature.ASMA_UL_HUSNA,
)

private class AsmaUlHusnaSource(private val useCases: AsmaUlHusnaUseCases) :
    CatalogSource<AsmaUlHusna> {
    override fun all(): Flow<List<AsmaUlHusna>> = useCases.getAllNames()
    override fun favourites(): Flow<List<AsmaUlHusna>> = useCases.getFavorites()
    override suspend fun byId(id: Int): AsmaUlHusna? = useCases.getNameById(id)
    override suspend fun toggleFavourite(id: Int) {
        useCases.toggleFavorite(id)
    }

    override fun idOf(item: AsmaUlHusna): Int = item.id

    override fun matches(item: AsmaUlHusna, query: String): Boolean =
        item.nameArabic.contains(query, ignoreCase = true) ||
                item.nameTransliteration.contains(query, ignoreCase = true) ||
                item.nameEnglish.contains(query, ignoreCase = true) ||
                item.meaning.contains(query, ignoreCase = true)
}
