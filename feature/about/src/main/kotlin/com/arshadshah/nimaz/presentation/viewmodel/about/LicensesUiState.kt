package com.arshadshah.nimaz.presentation.viewmodel.about

import com.arshadshah.nimaz.domain.model.LicenseFamily
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.presentation.viewmodel.UiError

/** How the licence list is broken into sections. */
enum class LicenseGrouping {
    /** One section per [LicenseFamily], largest first. */
    BY_LICENCE,

    /** One section per initial letter, A to Z. */
    ALPHABETICAL,
}

/**
 * One rendered section of the list.
 *
 * Carries a [family] when grouped by licence and a [letter] when alphabetical — exactly one
 * is non-null. The screen resolves whichever it gets into a heading, because a family's label
 * is a string resource and the ViewModel has no `Context`.
 */
data class LicenseSection(
    val family: LicenseFamily?,
    val letter: String?,
    val libraries: List<OpenSourceLibrary>,
) {
    val key: String get() = family?.name ?: letter.orEmpty()
}

/** A licence family and how many libraries in the full list carry it. */
data class LicenseFamilyCount(
    val family: LicenseFamily,
    val count: Int,
)

data class LicensesListUiState(
    val libraries: List<OpenSourceLibrary> = emptyList(),
    val query: String = "",
    val selectedFamily: LicenseFamily? = null,
    val grouping: LicenseGrouping = LicenseGrouping.BY_LICENCE,
    /** Counts over the **whole** list, so a chip's number does not change as you filter. */
    val familyCounts: List<LicenseFamilyCount> = emptyList(),
    val sections: List<LicenseSection> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiError? = null,
) {
    /** How many libraries survive the current query and filter. */
    val visibleCount: Int get() = sections.sumOf { it.libraries.size }

    val totalCount: Int get() = libraries.size

    val licenceCount: Int get() = familyCounts.size

    /** True when a query or filter is narrowing the list but nothing matched. */
    val isEmptyResult: Boolean
        get() = !isLoading && error == null &&
                libraries.isNotEmpty() && sections.isEmpty()
}

data class LicenseDetailUiState(
    val library: OpenSourceLibrary? = null,
    val isLoading: Boolean = true,
    val error: UiError? = null,
)

sealed interface LicensesEvent {
    data object LoadLibraries : LicensesEvent
    data class LoadLibrary(val id: Int) : LicensesEvent
    data class Search(val query: String) : LicensesEvent

    /** Null clears the filter — the "All" chip. */
    data class SelectFamily(val family: LicenseFamily?) : LicensesEvent
    data object ToggleGrouping : LicensesEvent
    data object Retry : LicensesEvent
    data object DismissError : LicensesEvent
}
