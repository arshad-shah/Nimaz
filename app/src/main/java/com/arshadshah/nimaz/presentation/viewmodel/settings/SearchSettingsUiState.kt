package com.arshadshah.nimaz.presentation.viewmodel.settings

data class SearchSettingsUiState(
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
