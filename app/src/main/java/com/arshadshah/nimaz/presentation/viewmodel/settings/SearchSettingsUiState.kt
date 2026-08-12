package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.domain.model.SearchPreferences

data class SearchSettingsUiState(
    /**
     * How local search behaves. All four of these were compile-time constants, which is how a
     * search for الله came to return exactly 180 results — three sources each capped at a
     * hidden 60 — and read as a defect.
     */
    val search: SearchPreferences = SearchPreferences(),
    val aiEnabled: Boolean = false,
    val historyEnabled: Boolean = false,
    /** Persisted recent questions — shown in the clear-history confirm dialog. */
    val savedQuestions: List<String> = emptyList(),
    val showConsentSheet: Boolean = false,
    /**
     * The consent write failed. The sheet stays up and says so, because the alternative —
     * what shipped — is a switch the user just turned on quietly turning itself back off.
     */
    val consentFailed: Boolean = false,
)
