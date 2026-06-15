package com.arshadshah.nimaz.core.monitoring

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin wrapper around Firebase Analytics.
 *
 * Like [CrashReporter], every call is guarded so it safely no-ops when Firebase
 * is not initialized (builds without `google-services.json`). Firebase Analytics
 * already auto-collects events such as `app_open` and `session_start`; this
 * helper adds manual screen tracking for the Compose navigation graph and a
 * convenience for logging custom events.
 */
object AppAnalytics {

    fun logScreenView(context: Context, screenName: String) {
        runCatching {
            FirebaseAnalytics.getInstance(context).logEvent(
                FirebaseAnalytics.Event.SCREEN_VIEW,
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                }
            )
        }
    }

    fun logEvent(context: Context, name: String, params: Bundle? = null) {
        runCatching {
            FirebaseAnalytics.getInstance(context).logEvent(name, params)
        }
    }
}
