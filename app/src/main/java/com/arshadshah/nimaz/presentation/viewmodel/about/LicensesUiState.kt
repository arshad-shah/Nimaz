package com.arshadshah.nimaz.presentation.viewmodel.about

import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.presentation.viewmodel.UiError

data class LicensesListUiState(
    val libraries: List<OpenSourceLibrary> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiError? = null,
)

data class LicenseDetailUiState(
    val library: OpenSourceLibrary? = null,
    val isLoading: Boolean = true,
    val error: UiError? = null,
)

sealed interface LicensesEvent {
    data object LoadLibraries : LicensesEvent
    data class LoadLibrary(val id: Int) : LicensesEvent
    data object Retry : LicensesEvent
    data object DismissError : LicensesEvent
}
