package com.arshadshah.nimaz.presentation.viewmodel.onboarding

sealed interface OnboardingEvent {
    data object CheckOnboardingStatus : OnboardingEvent
    data object CompleteOnboarding : OnboardingEvent
    data class SetCurrentPage(val page: Int) : OnboardingEvent
    data object CheckLocationPermission : OnboardingEvent
    data object CheckNotificationPermission : OnboardingEvent
    data object CheckBatteryOptimization : OnboardingEvent
    data object DetectLocation : OnboardingEvent
    data object DismissError : OnboardingEvent
    data class UpdatePermissionStatus(
        val location: Boolean? = null,
        val notification: Boolean? = null,
        val battery: Boolean? = null
    ) : OnboardingEvent
}
