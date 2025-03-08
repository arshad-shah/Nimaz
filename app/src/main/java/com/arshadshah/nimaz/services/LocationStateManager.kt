package com.arshadshah.nimaz.services

import com.arshadshah.nimaz.repositories.Location
import com.arshadshah.nimaz.viewModel.ViewModelLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationStateManager @Inject constructor() {
    companion object {
        private const val TAG = "Nimaz: LocationStateManager"
        const val REQUEST_TIMEOUT_MS = 60_000L // 1 minute timeout
        private const val REQUEST_CLEANUP_INTERVAL_MS =
            15_000L // Check for expired requests every 15 seconds
    }

    private val mutex = Mutex()
    private var activeLocationRequest: LocationRequest? = null
    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    init {
        // Start periodic cleanup of expired requests
        startPeriodicCleanup()
    }

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

        // Internal properties for robustness - not exposed in public API
        internal var retryCount: Int = 0
        internal val maxRetries: Int = 3
        internal val canRetry: Boolean
            get() = retryCount < maxRetries

        internal fun incrementRetry() {
            retryCount++
        }
    }

    // Keep the same public API
    suspend fun requestLocation(requestId: String): LocationRequest = mutex.withLock {
        try {
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
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Error creating location request: ${e.message}", e)
            // Still using the original API, so we create a new request even on error
            return createNewRequest(requestId)
        }
    }

    // Keep the same public API
    suspend fun updateLocationState(state: LocationState) = mutex.withLock {
        try {
            ViewModelLogger.d(TAG, "📍 Updating location state: $state")

            when (state) {
                is LocationState.Loading -> {
                    _locationState.value = state
                    // Don't clean up the request for Loading state
                }

                is LocationState.Success -> {
                    _locationState.value = state
                    cleanupRequest()
                }

                is LocationState.Error -> {
                    // Check if we can retry the request
                    val request = activeLocationRequest
                    if (request != null && request.canRetry) {
                        request.incrementRetry()
                        ViewModelLogger.d(
                            TAG,
                            "🔄 Retrying location request: ${request.id}, attempt ${request.retryCount}"
                        )
                        _locationState.value = LocationState.Loading
                    } else {
                        _locationState.value = state
                        cleanupRequest()
                    }
                }

                is LocationState.Idle -> {
                    _locationState.value = state
                    cleanupRequest()
                }
            }
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Error updating location state: ${e.message}", e)
            // Use original Error state API
            _locationState.value =
                LocationState.Error("Failed to update location state: ${e.message}")
            cleanupRequest()
        }
    }

    // Private helper methods
    fun cleanupRequest() {
        ViewModelLogger.d(TAG, "🧹 Cleaning up location request")
        activeLocationRequest = null
    }

    private fun createNewRequest(requestId: String): LocationRequest {
        val newRequest = LocationRequest(requestId)
        activeLocationRequest = newRequest
        ViewModelLogger.d(TAG, "✨ New location request created: $requestId")
        return newRequest
    }

    /**
     * Starts a periodic cleanup job that will check for expired requests and clean them up.
     * This helps ensure resources aren't held indefinitely if updateLocationState isn't called.
     */
    private fun startPeriodicCleanup() {
        managerScope.launch {
            while (true) {
                withTimeoutOrNull(REQUEST_CLEANUP_INTERVAL_MS) {
                    // Periodically check for and clean up expired requests
                    val request = activeLocationRequest
                    if (request?.isExpired == true) {
                        mutex.withLock {
                            ViewModelLogger.d(
                                TAG,
                                "⏰ Periodic cleanup: Expired request ${request.id}"
                            )
                            _locationState.value = LocationState.Error("Location request timed out")
                            cleanupRequest()
                        }
                    }
                    kotlinx.coroutines.delay(REQUEST_CLEANUP_INTERVAL_MS)
                }
            }
        }
    }

    // ADDED FOR TESTING - allows tests to access the active request
    internal fun getActiveLocationRequest(): LocationRequest? = activeLocationRequest
}