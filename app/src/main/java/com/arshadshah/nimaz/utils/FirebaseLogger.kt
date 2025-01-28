package com.arshadshah.nimaz.utils

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object FirebaseLogger {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init() {
        if (firebaseAnalytics == null) {
            this.firebaseAnalytics = Firebase.analytics
            Log.d("Nimaz: FirebaseLogger", "Firebase Analytics Initialized")
        }
    }

    fun logEvent(eventName: String, params: Map<String, Any>?) {
        val bundle = Bundle()
        params?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putFloat(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
            }
        }
        firebaseAnalytics?.logEvent(eventName, bundle)
        Log.d("Nimaz: FirebaseLogger", "Logged Event: $eventName")
    }

    fun isInitialized(): Boolean {
        return firebaseAnalytics != null
    }


}