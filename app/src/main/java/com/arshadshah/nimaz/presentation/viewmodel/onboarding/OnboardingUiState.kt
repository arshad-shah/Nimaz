package com.arshadshah.nimaz.presentation.viewmodel.onboarding

data class OnboardingUiState(
    val isLoading: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val currentPage: Int = 0,
    val locationPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val locationDetected: Boolean = false,
    val locationName: String = "",
    val error: String? = null
)
