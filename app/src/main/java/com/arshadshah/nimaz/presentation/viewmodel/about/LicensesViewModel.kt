package com.arshadshah.nimaz.presentation.viewmodel.about

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.LicenseFamily
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.domain.usecase.licenses.LicensesUseCases
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
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
            is LicensesEvent.Search ->
                _listState.update { it.copy(query = event.query).regrouped() }

            is LicensesEvent.SelectFamily ->
                _listState.update { it.copy(selectedFamily = event.family).regrouped() }

            LicensesEvent.ToggleGrouping -> _listState.update {
                val next = when (it.grouping) {
                    LicenseGrouping.BY_LICENCE -> LicenseGrouping.ALPHABETICAL
                    LicenseGrouping.ALPHABETICAL -> LicenseGrouping.BY_LICENCE
                }
                it.copy(grouping = next).regrouped()
            }

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
            _listState.update {
                it.copy(
                    libraries = libraries,
                    isLoading = false,
                    familyCounts = libraries.familyCounts(),
                ).regrouped()
            }
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

/**
 * Re-derives [LicensesListUiState.sections] from the list, query, filter and grouping.
 *
 * A pure function over the state rather than a `combine` of four flows: the inputs all live in
 * one `MutableStateFlow` already, and every caller changes exactly one of them, so there is no
 * ordering to coordinate — only one place that must not be forgotten, which is why the state
 * class does not expose a way to change those fields without going through here.
 */
internal fun LicensesListUiState.regrouped(): LicensesListUiState {
    val matching = libraries
        .filter { selectedFamily == null || it.family == selectedFamily }
        .filter { it.matches(query) }

    val sections = when (grouping) {
        LicenseGrouping.BY_LICENCE -> matching
            .groupBy { it.family }
            // Largest family first — Nimaz is overwhelmingly Apache-2.0, and an alphabetical
            // family order would open the screen on whichever tiny section sorts first.
            .entries
            .sortedWith(compareByDescending<Map.Entry<LicenseFamily, List<OpenSourceLibrary>>> {
                it.value.size
            }.thenBy { it.key.ordinal })
            .map { (family, libraries) ->
                LicenseSection(family = family, letter = null, libraries = libraries.sortedByName())
            }

        LicenseGrouping.ALPHABETICAL -> matching
            .groupBy { it.name.initial() }
            .entries
            .sortedBy { it.key }
            .map { (letter, libraries) ->
                LicenseSection(family = null, letter = letter, libraries = libraries.sortedByName())
            }
    }

    return copy(sections = sections)
}

/** Counts every family present in the full list, largest first. */
internal fun List<OpenSourceLibrary>.familyCounts(): List<LicenseFamilyCount> =
    groupingBy { it.family }
        .eachCount()
        .map { (family, count) -> LicenseFamilyCount(family, count) }
        .sortedWith(compareByDescending<LicenseFamilyCount> { it.count }.thenBy { it.family.ordinal })

/**
 * Whether a library answers to [query].
 *
 * Matches the name, the author and the Maven coordinate. The coordinate is included because
 * the displayed name is AboutLibraries' human one ("Compose UI") while the thing a developer
 * knows is `androidx.compose.ui:ui`, and searching for the latter finding nothing reads as a
 * broken search.
 */
private fun OpenSourceLibrary.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val needle = query.trim()
    return name.contains(needle, ignoreCase = true) ||
            author?.contains(needle, ignoreCase = true) == true ||
            coordinate.contains(needle, ignoreCase = true)
}

private fun List<OpenSourceLibrary>.sortedByName(): List<OpenSourceLibrary> =
    sortedBy { it.name.lowercase() }

/** The section letter for a name; anything not starting with a letter lands under "#". */
private fun String.initial(): String =
    firstOrNull()?.takeIf { it.isLetter() }?.uppercase() ?: "#"
