package com.arshadshah.nimaz.services

import com.arshadshah.nimaz.repositories.Location
import com.arshadshah.nimaz.testUtils.AndroidLogMock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.lang.reflect.Field

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class LocationStateManagerTest {

    private lateinit var locationStateManager: LocationStateManager
    private lateinit var testScope: TestScope

    @Mock
    private lateinit var mockLocation: Location

    @Before
    fun setUp() {
        // Set up Android Log mocking to prevent "Method not mocked" exceptions
        AndroidLogMock.setupLogMock()

        testScope = TestScope(UnconfinedTestDispatcher())
        locationStateManager = LocationStateManager()
    }

    @After
    fun tearDown() {
        // Clean up mocks after each test
        AndroidLogMock.tearDownLogMock()
    }

    // Helper method to access the private activeLocationRequest field
    private fun getActiveLocationRequest(): LocationStateManager.LocationRequest? {
        val field: Field = LocationStateManager::class.java.getDeclaredField("activeLocationRequest")
        field.isAccessible = true
        return field.get(locationStateManager) as? LocationStateManager.LocationRequest
    }

    // Helper method to set the private activeLocationRequest field
    private fun setActiveLocationRequest(request: LocationStateManager.LocationRequest?) {
        val field: Field = LocationStateManager::class.java.getDeclaredField("activeLocationRequest")
        field.isAccessible = true
        field.set(locationStateManager, request)
    }

    // Helper method to create an expired request
    private fun createExpiredRequest(requestId: String): LocationStateManager.LocationRequest {
        return LocationStateManager.LocationRequest(
            requestId,
            System.currentTimeMillis() - LocationStateManager.REQUEST_TIMEOUT_MS - 1000
        )
    }

    @Test
    fun `getLocationState initial state`() = testScope.runTest {
        // Verify that getLocationState() initially returns LocationState.Idle
        val initialState = locationStateManager.locationState.value
        assertTrue(initialState is LocationStateManager.LocationState.Idle)
    }

    @Test
    fun `requestLocation new request`() = testScope.runTest {
        // Verify that requestLocation() creates a new request when no request is active
        val requestId = "test_request_1"
        val result = locationStateManager.requestLocation(requestId)

        // Verify the result
        assertEquals(requestId, result.id)

        // Verify internal state
        val activeRequest = getActiveLocationRequest()
        assertNotNull(activeRequest)
        assertEquals(requestId, activeRequest?.id)
    }

    @Test
    fun `requestLocation existing active request`() = testScope.runTest {
        // Set up an existing active request
        val existingRequestId = "existing_request"
        val existingRequest = LocationStateManager.LocationRequest(existingRequestId)
        setActiveLocationRequest(existingRequest)

        // Call requestLocation again
        val result = locationStateManager.requestLocation("new_request")

        // Verify that the existing request is returned
        assertEquals(existingRequestId, result.id)
        assertEquals(existingRequest, result)
    }

    @Test
    fun `requestLocation expired request`() = testScope.runTest {
        // Set up an expired request
        val expiredRequestId = "expired_request"
        val expiredRequest = createExpiredRequest(expiredRequestId)
        setActiveLocationRequest(expiredRequest)

        // Call requestLocation with a new ID
        val newRequestId = "new_request"
        val result = locationStateManager.requestLocation(newRequestId)

        // Verify that a new request is created
        assertNotEquals(expiredRequestId, result.id)
        assertEquals(newRequestId, result.id)

        // Verify internal state
        val activeRequest = getActiveLocationRequest()
        assertEquals(newRequestId, activeRequest?.id)
    }

    @Test
    fun `requestLocation multiple requests concurrency`() = testScope.runTest {
        // We're using UnconfinedTestDispatcher, so we'll simulate concurrency
        // by making multiple calls in sequence

        // First request
        val request1 = locationStateManager.requestLocation("request1")
        assertEquals("request1", request1.id)

        // Second request (should return the active request from first call)
        val request2 = locationStateManager.requestLocation("request2")
        assertEquals("request1", request2.id)

        // Let's set the first request as expired and try again
        setActiveLocationRequest(createExpiredRequest("request1"))

        // Third request (should create a new one since the previous is expired)
        val request3 = locationStateManager.requestLocation("request3")
        assertEquals("request3", request3.id)
    }

    @Test
    fun `updateLocationState loading`() = testScope.runTest {
        // First create a request
        val request = locationStateManager.requestLocation("test-request-id")

        // Call updateLocationState with Loading
        locationStateManager.updateLocationState(LocationStateManager.LocationState.Loading)

        // Verify the state is updated
        val currentState = locationStateManager.locationState.value
        assertTrue(currentState is LocationStateManager.LocationState.Loading)

        // Verify the request is NOT cleaned up
        assertNotNull(locationStateManager.getActiveLocationRequest())
    }
    @Test
    fun `updateLocationState success`() = testScope.runTest {
        // Set up an active request
        setActiveLocationRequest(LocationStateManager.LocationRequest("test_request"))

        // Call updateLocationState with Success
        val successState = LocationStateManager.LocationState.Success(mockLocation)
        locationStateManager.updateLocationState(successState)

        // Verify the state is updated
        val currentState = locationStateManager.locationState.value
        assertTrue(currentState is LocationStateManager.LocationState.Success)
        assertEquals(mockLocation, (currentState as LocationStateManager.LocationState.Success).location)

        // Verify cleanupRequest was called (activeLocationRequest should be null)
        assertNull(getActiveLocationRequest())
    }

    @Test
    fun `updateLocationState error`() = testScope.runTest {
        // Set up an active request
        setActiveLocationRequest(LocationStateManager.LocationRequest("test_request"))

        // For our more robust implementation, we need to ensure retry behavior doesn't
        // interfere with the test. We'll modify the maxRetries of the current request.
        val field: Field = LocationStateManager.LocationRequest::class.java.getDeclaredField("maxRetries")
        field.isAccessible = true
        field.set(getActiveLocationRequest(), 0)

        // Call updateLocationState with Error
        val errorMessage = "Location not available"
        val errorState = LocationStateManager.LocationState.Error(errorMessage)
        locationStateManager.updateLocationState(errorState)

        // Verify the state is updated
        val currentState = locationStateManager.locationState.value
        assertTrue(currentState is LocationStateManager.LocationState.Error)
        assertEquals(errorMessage, (currentState as LocationStateManager.LocationState.Error).message)

        // Verify cleanupRequest was called (activeLocationRequest should be null)
        assertNull(getActiveLocationRequest())
    }

    @Test
    fun `updateLocationState error with retry`() = testScope.runTest {
        // Set up an active request
        val testRequest = LocationStateManager.LocationRequest("test_request")
        setActiveLocationRequest(testRequest)

        // Set maxRetries to 2 for testing
        val maxRetriesField = LocationStateManager.LocationRequest::class.java.getDeclaredField("maxRetries")
        maxRetriesField.isAccessible = true
        maxRetriesField.set(testRequest, 2)

        // Call updateLocationState with Error the first time
        val errorMessage = "Location not available"
        val errorState = LocationStateManager.LocationState.Error(errorMessage)
        locationStateManager.updateLocationState(errorState)

        // After first error, state should be Loading (retry)
        var currentState = locationStateManager.locationState.value
        assertTrue("Expected Loading state for first retry",
            currentState is LocationStateManager.LocationState.Loading)

        // Request should still be active
        assertNotNull("Request should be active during retry", getActiveLocationRequest())

        // Get retry count to verify it's been incremented
        val retryCountField = LocationStateManager.LocationRequest::class.java.getDeclaredField("retryCount")
        retryCountField.isAccessible = true
        var retryCount = retryCountField.get(getActiveLocationRequest()) as Int
        assertEquals("Retry count should be 1 after first retry", 1, retryCount)

        // Call updateLocationState with Error a second time
        locationStateManager.updateLocationState(errorState)

        // After second error, state should still be Loading (second retry)
        currentState = locationStateManager.locationState.value
        assertTrue("Expected Loading state for second retry",
            currentState is LocationStateManager.LocationState.Loading)

        // Request should still be active
        assertNotNull("Request should be active during second retry", getActiveLocationRequest())

        // Verify retry count is 2
        retryCount = retryCountField.get(getActiveLocationRequest()) as Int
        assertEquals("Retry count should be 2 after second retry", 2, retryCount)

        // Call updateLocationState with Error a third time
        locationStateManager.updateLocationState(errorState)

        // After third error (exceeding max retries), state should be Error
        currentState = locationStateManager.locationState.value
        assertTrue("Expected Error state after max retries exhausted",
            currentState is LocationStateManager.LocationState.Error)
        assertEquals("Error message should match", errorMessage,
            (currentState as LocationStateManager.LocationState.Error).message)

        // Request should be cleaned up
        assertNull("Request should be cleaned up after max retries", getActiveLocationRequest())
    }

    @Test
    fun `updateLocationState idle`() = testScope.runTest {
        // Set up an active request
        setActiveLocationRequest(LocationStateManager.LocationRequest("test_request"))

        // First set a non-idle state
        locationStateManager.updateLocationState(LocationStateManager.LocationState.Loading)

        // Then call updateLocationState with Idle
        locationStateManager.updateLocationState(LocationStateManager.LocationState.Idle)

        // Verify the state is updated
        val currentState = locationStateManager.locationState.value
        assertTrue(currentState is LocationStateManager.LocationState.Idle)

        // Verify cleanupRequest was called (activeLocationRequest should be null)
        assertNull(getActiveLocationRequest())
    }

    @Test
    fun `updateLocationState concurrent updates`() = testScope.runTest {
        // With UnconfinedTestDispatcher, we'll simulate concurrent updates
        // by making multiple calls in sequence

        // Set up a sequence of state updates
        locationStateManager.updateLocationState(LocationStateManager.LocationState.Loading)
        locationStateManager.updateLocationState(LocationStateManager.LocationState.Error("error"))
        locationStateManager.updateLocationState(LocationStateManager.LocationState.Success(mockLocation))
        locationStateManager.updateLocationState(LocationStateManager.LocationState.Idle)

        // Verify the final state is as expected
        val finalState = locationStateManager.locationState.value
        assertTrue(finalState is LocationStateManager.LocationState.Idle)
    }

    @Test
    fun `updateLocationState same state`() = testScope.runTest {
        // Set initial state to Loading
        locationStateManager.updateLocationState(LocationStateManager.LocationState.Loading)

        // Set up an active request
        val request = LocationStateManager.LocationRequest("test_request")
        setActiveLocationRequest(request)

        // Call updateLocationState with the same Loading state again
        locationStateManager.updateLocationState(LocationStateManager.LocationState.Loading)

        // Verify the state remains Loading
        val currentState = locationStateManager.locationState.value
        assertTrue(currentState is LocationStateManager.LocationState.Loading)

        // Verify the active request is still present (not cleaned up)
        val activeRequest = getActiveLocationRequest()
        assertNotNull(activeRequest)
        assertEquals(request.id, activeRequest?.id)
    }

    @Test
    fun `cleanupRequest clears active request`() = testScope.runTest {
        // Set up an active request
        setActiveLocationRequest(LocationStateManager.LocationRequest("test_request"))

        // Call cleanupRequest
        locationStateManager.cleanupRequest()

        // Verify the request is cleared
        assertNull(getActiveLocationRequest())
    }

    @Test
    fun `requestLocation ID uniqueness`() = testScope.runTest {
        // Call requestLocation with a unique ID
        val uniqueId = "uniqueId123"
        val request = locationStateManager.requestLocation(uniqueId)

        // Verify the request ID matches what was passed
        assertEquals(uniqueId, request.id)

        // Verify the active request has the same ID
        val activeRequest = getActiveLocationRequest()
        assertEquals(uniqueId, activeRequest?.id)
    }

    @Test
    fun `requestLocation timestamp`() = testScope.runTest {
        // Record the current time
        val beforeTime = System.currentTimeMillis()

        // Call requestLocation
        val request = locationStateManager.requestLocation("test_request")

        // Record after time
        val afterTime = System.currentTimeMillis()

        // Verify timestamp is between before and after times
        assertTrue(request.timestamp >= beforeTime)
        assertTrue(request.timestamp <= afterTime)
    }

    @Test
    fun `LocationRequest expiration`() = testScope.runTest {
        // Create a fresh request
        val freshRequest = LocationStateManager.LocationRequest("fresh_request")

        // Verify it's not expired
        assertFalse(freshRequest.isExpired)

        // Create an expired request
        val expiredRequest = createExpiredRequest("expired_request")

        // Verify it's expired
        assertTrue(expiredRequest.isExpired)
    }

    @Test
    fun `requestLocation ID empty`() = testScope.runTest {
        // Call requestLocation with an empty ID
        val request = locationStateManager.requestLocation("")

        // Verify it works as expected
        assertEquals("", request.id)

        // Verify internal state
        val activeRequest = getActiveLocationRequest()
        assertNotNull(activeRequest)
        assertEquals("", activeRequest?.id)
    }

    @Test
    fun `requestLocation ID special characters`() = testScope.runTest {
        // Call requestLocation with a special characters ID
        val specialId = "@#$%^&*!+-/\\."
        val request = locationStateManager.requestLocation(specialId)

        // Verify it works as expected
        assertEquals(specialId, request.id)

        // Verify internal state
        val activeRequest = getActiveLocationRequest()
        assertNotNull(activeRequest)
        assertEquals(specialId, activeRequest?.id)
    }

    @Test
    fun `requestLocation ID null`() = testScope.runTest {
        try {
            // Call requestLocation with null - this should cause an exception
            // Note: Kotlin has null safety so this is only testable by reflection or
            // if the method was explicitly designed to handle null values
            locationStateManager.requestLocation(null as String)

            // If we get here, the method didn't throw an exception, so the test fails
            assertTrue("Expected NullPointerException was not thrown", false)
        } catch (e: NullPointerException) {
            // Expected behavior - test passes
            assertTrue(true)
        }
    }

    @Test
    fun `updateLocationState empty error message`() = testScope.runTest {
        // Call updateLocationState with Error and empty message
        val errorState = LocationStateManager.LocationState.Error("")
        locationStateManager.updateLocationState(errorState)

        // Verify the state is updated correctly
        val currentState = locationStateManager.locationState.value
        assertTrue(currentState is LocationStateManager.LocationState.Error)
        assertEquals("", (currentState as LocationStateManager.LocationState.Error).message)
    }

    @Test
    fun `updateLocationState special error message`() = testScope.runTest {
        // Call updateLocationState with Error and special character message
        val specialMessage = "@#$%^&*!+-/\\."
        val errorState = LocationStateManager.LocationState.Error(specialMessage)
        locationStateManager.updateLocationState(errorState)

        // Verify the state is updated correctly
        val currentState = locationStateManager.locationState.value
        assertTrue(currentState is LocationStateManager.LocationState.Error)
        assertEquals(specialMessage, (currentState as LocationStateManager.LocationState.Error).message)
    }
}