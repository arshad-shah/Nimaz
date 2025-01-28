package com.arshadshah.nimaz.services

import com.arshadshah.nimaz.repositories.Location
import com.arshadshah.nimaz.viewModel.ViewModelLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationStateManager @Inject constructor() {
    companion object {
        private const val TAG = "Nimaz: LocationStateManager"
        private const val REQUEST_TIMEOUT_MS = 60_000L // 1 minute timeout
    }

    private val mutex = Mutex()
    private var activeLocationRequest: LocationRequest? = null

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    // Keep the original LocationState sealed class exactly as it was
    sealed class LocationState {
        data object Idle : LocationState()
        data object Loading : LocationState()
        data class Success(val location: Location) : LocationState()
        data class Error(val message: String) : LocationState()
    }

    // Keep the same LocationRequest data class
    data class LocationRequest(
        val id: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        // Internal helper property
        val isExpired: Boolean
            get() = System.currentTimeMillis() - timestamp > REQUEST_TIMEOUT_MS
    }

    // Keep the same public API
    suspend fun requestLocation(requestId: String): LocationRequest = mutex.withLock {
        activeLocationRequest?.let { existingRequest ->
            // Check for request timeout
            if (existingRequest.isExpired) {
                ViewModelLogger.d(TAG, "⏰ Existing request expired: ${existingRequest.id}")
                // Clean up expired request
                cleanupRequest()
                // Create new request since the old one expired
                return createNewRequest(requestId)
            }
            ViewModelLogger.d(TAG, "🔄 Location request in progress: ${existingRequest.id}")
            return existingRequest
        }

        return createNewRequest(requestId)
    }

    // Keep the same public API
    suspend fun updateLocationState(state: LocationState) = mutex.withLock {
        ViewModelLogger.d(TAG, "📍 Updating location state: $state")

        when (state) {
            is LocationState.Loading -> {
                _locationState.value = state
            }

            is LocationState.Success -> {
                _locationState.value = state
                cleanupRequest()
            }

            is LocationState.Error -> {
                _locationState.value = state
                cleanupRequest()
            }

            is LocationState.Idle -> {
                _locationState.value = state
                cleanupRequest()
            }
        }
    }

    // Private helper methods
    fun cleanupRequest() {
        ViewModelLogger.d(TAG, "🧹 Cleaning up location request")
        activeLocationRequest = null
        //set to idle state
        _locationState.value = LocationState.Idle
    }

    private fun createNewRequest(requestId: String): LocationRequest {
        val newRequest = LocationRequest(requestId)
        activeLocationRequest = newRequest
        ViewModelLogger.d(TAG, "✨ New location request created: $requestId")
        return newRequest
    }
}