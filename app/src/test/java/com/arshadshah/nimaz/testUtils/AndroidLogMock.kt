package com.arshadshah.nimaz.testUtils

import android.util.Log
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito

/**
 * A utility class to handle mocking of Android's Log class for unit tests.
 * This prevents "Method not mocked" exceptions when running tests.
 */
object AndroidLogMock {

    private var mockLog: MockedStatic<Log>? = null

    /**
     * Set up mocks for Android Log class.
     * Must be called in @Before setup method.
     */
    fun setupLogMock() {
        if (mockLog == null) {
            mockLog = Mockito.mockStatic(Log::class.java)

            // Mock all commonly used Log methods
            mockLog?.`when`<Int> {
                Log.d(anyString(), anyString())
            }?.thenReturn(0)

            mockLog?.`when`<Int> {
                Log.d(anyString(), anyString(), Mockito.any())
            }?.thenReturn(0)

            mockLog?.`when`<Int> {
                Log.i(anyString(), anyString())
            }?.thenReturn(0)

            mockLog?.`when`<Int> {
                Log.e(anyString(), anyString())
            }?.thenReturn(0)

            mockLog?.`when`<Int> {
                Log.e(anyString(), anyString(), Mockito.any())
            }?.thenReturn(0)

            mockLog?.`when`<Int> {
                Log.w(anyString(), anyString())
            }?.thenReturn(0)

            mockLog?.`when`<Int> {
                Log.v(anyString(), anyString())
            }?.thenReturn(0)
        }
    }

    /**
     * Clean up the Android Log mocks.
     * Must be called in @After teardown method.
     */
    fun tearDownLogMock() {
        mockLog?.close()
        mockLog = null
    }
}