package com.arshadshah.nimaz.presentation.viewmodel.about

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.usecase.licenses.LicensesUseCases
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorKind
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * The licence list and one library's detail.
 *
 * Two states rather than one, which is the house pattern for a list/detail pair (see
 * `AsmaUlHusnaViewModel`) and not something to consolidate: the screens are separate, and
 * merging them would make either screen's loading flag flicker on the other's work.
 */
@HiltViewModel
class LicensesViewModel @Inject constructor(
    private val useCases: LicensesUseCases,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _listState = MutableStateFlow(LicensesListUiState())
    val listState: StateFlow<LicensesListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(LicenseDetailUiState())
    val detailState: StateFlow<LicenseDetailUiState> = _detailState.asStateFlow()

    fun onEvent(event: LicensesEvent) {
        when (event) {
            LicensesEvent.LoadLibraries -> loadLibraries()
            is LicensesEvent.LoadLibrary -> loadLibrary(event.id)
            // Retry serves the list only. The detail screen's failure is NOT_FOUND — a
            // library missing from the bundled list will be missing next time too, so
            // offering "try again" there would be a lie.
            LicensesEvent.Retry -> loadLibraries()
            LicensesEvent.DismissError -> {
                _listState.update { it.copy(error = null) }
                _detailState.update { it.copy(error = null) }
            }
        }
    }

    private fun loadLibraries() {
        _listState.update { it.copy(isLoading = true, error = null) }
        launchSafely(
            telemetry, DOMAIN, "load_libraries",
            onFailure = { throwable ->
                _listState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.licenses_load_failed,
                            kind = NimazErrorKind.GENERIC,
                            details = throwable.message,
                        ),
                    )
                }
            },
        ) {
            val libraries = useCases.getLibraries()
            _listState.update { it.copy(libraries = libraries, isLoading = false) }
        }
    }

    private fun loadLibrary(id: Int) {
        _detailState.update { it.copy(isLoading = true, error = null) }
        launchSafely(
            telemetry, DOMAIN, "load_library",
            onFailure = { throwable ->
                _detailState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.licenses_load_failed,
                            kind = NimazErrorKind.GENERIC,
                            details = throwable.message,
                        ),
                    )
                }
            },
        ) {
            val library = useCases.getLibrary(id)
            _detailState.update {
                it.copy(
                    library = library,
                    isLoading = false,
                    // A missing id is a real answer, not a thrown failure: nothing went
                    // wrong, the library simply is not there. Reported to the reader,
                    // not to telemetry.
                    error = if (library == null) {
                        UiError(
                            message = R.string.license_detail_library_not_found,
                            kind = NimazErrorKind.NOT_FOUND,
                        )
                    } else {
                        null
                    },
                )
            }
        }
    }

    private companion object {
        const val DOMAIN = "about"
    }
}
