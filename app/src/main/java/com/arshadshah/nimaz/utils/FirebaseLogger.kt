package com.arshadshah.nimaz.utils

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A robust Firebase Analytics logger with Hilt dependency injection
 */
@Singleton
class FirebaseLogger @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    private var isDebugMode = false

    companion object {
        private const val TAG = "FirebaseLogger"

        // Standard event categories to ensure consistency
        object EventCategory {
            const val LIFECYCLE = "lifecycle"
            const val SCREEN_VIEW = "screen_view"
            const val USER_ACTION = "user_action"
            const val APP_ERROR = "app_error"
            const val PERFORMANCE = "performance"
            const val USER_PROPERTY = "user_property"
        }
    }

    /**
     * Configure analytics settings
     * @param debugMode Set to true to enable detailed logging
     */
    fun configure(debugMode: Boolean = false) {
        try {
            isDebugMode = debugMode

            // Set analytics collection enabled
            firebaseAnalytics.setAnalyticsCollectionEnabled(true)

            if (isDebugMode) {
                Log.d(TAG, "Firebase Analytics configured successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure Firebase Analytics: ${e.message}")
        }
    }

    /**
     * Log an event with parameters
     * @param eventName Name of the event
     * @param params Optional map of parameters
     * @param category Optional event category for better organization
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null, category: String? = null) {
        try {
            val bundle = convertMapToBundle(params)

            // Add category if provided
            category?.let {
                bundle.putString("event_category", it)
            }

            // Standardize event name (replace spaces with underscores, lowercase)
            val standardizedEventName = standardizeEventName(eventName)

            firebaseAnalytics.logEvent(standardizedEventName, bundle)

            if (isDebugMode) {
                Log.d(TAG, "Logged event: $standardizedEventName, params: $params")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging event '$eventName': ${e.message}")
        }
    }

    /**
     * Log a screen view event
     * @param screenName Name of the screen
     * @param screenClass Optional class name of the screen
     */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        val params = mutableMapOf<String, Any>(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName
        )

        screenClass?.let {
            params[FirebaseAnalytics.Param.SCREEN_CLASS] = it
        }

        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params, EventCategory.SCREEN_VIEW)
    }

    /**
     * Log an error event
     * @param errorType Type/category of error
     * @param errorMessage Error message
     * @param errorDetails Additional error details
     */
    fun logError(errorType: String, errorMessage: String, errorDetails: Map<String, Any>? = null) {
        val params = mutableMapOf<String, Any>(
            "error_type" to errorType,
            "error_message" to errorMessage
        )

        errorDetails?.let { params.putAll(it) }

        logEvent("app_error", params, EventCategory.APP_ERROR)
    }

    /**
     * Set a user property
     * @param name Name of the property
     * @param value Value of the property
     */
    fun setUserProperty(name: String, value: String) {
        try {
            val standardizedName = standardizePropertyName(name)
            firebaseAnalytics.setUserProperty(standardizedName, value)

            if (isDebugMode) {
                Log.d(TAG, "Set user property: $standardizedName = $value")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user property '$name': ${e.message}")
        }
    }

    /**
     * Enable or disable debug mode
     * @param enabled True to enable debug mode, false otherwise
     */
    fun setDebugMode(enabled: Boolean) {
        isDebugMode = enabled
    }

    /**
     * Reset the analytics instance (useful for user logout)
     */
    fun reset() {
        firebaseAnalytics.resetAnalyticsData()
        if (isDebugMode) {
            Log.d(TAG, "Analytics data reset")
        }
    }

    /**
     * Convert a Map to a Bundle for Firebase Analytics
     * @param map Map to convert
     * @return Bundle containing the map data
     */
    private fun convertMapToBundle(map: Map<String, Any>?): Bundle {
        val bundle = Bundle()
        map?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putFloat(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                is ArrayList<*> -> {
                    // Handle ArrayList by converting to appropriate array types
                    when {
                        value.isEmpty() -> bundle.putStringArray(key, emptyArray())
                        value[0] is String -> bundle.putStringArray(
                            key,
                            value.filterIsInstance<String>().toTypedArray()
                        )

                        value[0] is Int -> bundle.putIntArray(
                            key,
                            value.filterIsInstance<Int>().toIntArray()
                        )

                        value[0] is Long -> bundle.putLongArray(
                            key,
                            value.filterIsInstance<Long>().toLongArray()
                        )

                        value[0] is Float -> bundle.putFloatArray(
                            key,
                            value.filterIsInstance<Float>().toFloatArray()
                        )

                        value[0] is Double -> bundle.putDoubleArray(
                            key,
                            value.filterIsInstance<Double>().toDoubleArray()
                        )

                        value[0] is Boolean -> {
                            val boolArray = value.filterIsInstance<Boolean>()
                            val booleanArray = BooleanArray(boolArray.size) { i -> boolArray[i] }
                            bundle.putBooleanArray(key, booleanArray)
                        }
                    }
                }

                else -> {
                    if (isDebugMode) {
                        Log.w(
                            TAG,
                            "Unsupported parameter type for key '$key': ${value::class.java.simpleName}"
                        )
                    }
                    bundle.putString(key, value.toString())
                }
            }
        }
        return bundle
    }

    /**
     * Standardize event names to follow Firebase Analytics requirements
     */
    private fun standardizeEventName(name: String): String {
        // Replace spaces and special characters with underscores
        var result = name.replace(Regex("[^a-zA-Z0-9_]"), "_")

        // Convert to lowercase
        result = result.lowercase()

        // Ensure it starts with a letter or underscore
        if (result.isNotEmpty() && !result[0].isLetter() && result[0] != '_') {
            result = "_$result"
        }

        // Limit to 40 characters (Firebase limit)
        if (result.length > 40) {
            result = result.substring(0, 40)
        }

        return result
    }

    /**
     * Standardize property names to follow Firebase Analytics requirements
     */
    private fun standardizePropertyName(name: String): String {
        // Same rules as event names but limited to 24 characters
        var result = name.replace(Regex("[^a-zA-Z0-9_]"), "_")
        result = result.lowercase()

        if (result.isNotEmpty() && !result[0].isLetter() && result[0] != '_') {
            result = "_$result"
        }

        // Limit to 24 characters (Firebase user property limit)
        if (result.length > 24) {
            result = result.substring(0, 24)
        }

        return result
    }
}