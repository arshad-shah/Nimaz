package com.arshadshah.nimaz.core.util

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpdateAvailable : UpdateState

    /**
     * The user requested an update and we're fetching the AppUpdateInfo / bringing
     * up the Play confirmation dialog. This can take several seconds on slow
     * connections, so it's surfaced as a distinct loading state to give immediate
     * feedback that the tap was registered.
     */
    data object Starting : UpdateState
    data object Downloading : UpdateState
    data class Downloaded(val completeUpdate: () -> Unit) : UpdateState
    data object NoUpdateAvailable : UpdateState
    data class Error(val message: String) : UpdateState
}

class InAppUpdateManager(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    // Launcher for the Play update confirmation dialog. Must be registered by the
    // host Activity before it is STARTED (see MainActivity), so it's injected
    // rather than created here.
    private var updateFlowLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.PENDING,
            InstallStatus.DOWNLOADING -> {
                _updateState.value = UpdateState.Downloading
            }

            InstallStatus.DOWNLOADED -> {
                _updateState.value = UpdateState.Downloaded {
                    appUpdateManager.completeUpdate()
                }
            }

            InstallStatus.FAILED -> {
                _updateState.value = UpdateState.Error("Update download failed")
            }

            InstallStatus.CANCELED -> {
                // User cancelled the download — return to an actionable state.
                _updateState.value = UpdateState.UpdateAvailable
            }

            else -> {}
        }
    }

    fun setUpdateFlowLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        updateFlowLauncher = launcher
    }

    fun checkForUpdate() {
        _updateState.value = UpdateState.Checking
        appUpdateManager.registerListener(installStateListener)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                _updateState.value = UpdateState.UpdateAvailable
            } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _updateState.value = UpdateState.Downloaded {
                    appUpdateManager.completeUpdate()
                }
            } else {
                _updateState.value = UpdateState.NoUpdateAvailable
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Update check failed", e)
            CrashReporter.recordException(e)
            AppAnalytics.logError("in_app_update", e.javaClass.simpleName, e.message)
            _updateState.value = UpdateState.Error(e.message ?: "Update check failed")
        }
    }

    fun startUpdate() {
        // Reflect the tap immediately. Fetching AppUpdateInfo and showing the Play
        // dialog is asynchronous and can take a few seconds on slow connections;
        // without this the banner/button would appear unresponsive ("nothing
        // happened") even though the flow is in progress.
        _updateState.value = UpdateState.Starting

        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            val launcher = updateFlowLauncher
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    _updateState.value = UpdateState.Downloaded {
                        appUpdateManager.completeUpdate()
                    }
                }

                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                        launcher != null -> {
                    val launched = appUpdateManager.startUpdateFlowForResult(
                        info,
                        launcher,
                        AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)
                    )
                    if (!launched) {
                        // Couldn't launch the dialog — restore the actionable state.
                        _updateState.value = UpdateState.UpdateAvailable
                    }
                    // Otherwise the install listener / onUpdateFlowResult drive the
                    // next state once the user responds to the dialog.
                }

                else -> {
                    _updateState.value = UpdateState.NoUpdateAvailable
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Start update failed", e)
            CrashReporter.recordException(e)
            AppAnalytics.logError("in_app_update", e.javaClass.simpleName, e.message)
            _updateState.value = UpdateState.Error(e.message ?: "Update failed to start")
        }
    }

    /**
     * Result of the Play update confirmation dialog, forwarded by the host
     * Activity. When the user dismisses or cancels the dialog we fall back to
     * [UpdateState.UpdateAvailable] so the banner/button becomes interactive again
     * instead of being stuck on the [UpdateState.Starting] spinner. On a confirmed
     * update the install listener takes over and drives the download states.
     */
    fun onUpdateFlowResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK && _updateState.value == UpdateState.Starting) {
            _updateState.value = UpdateState.UpdateAvailable
        }
    }

    fun checkForStalledUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _updateState.value = UpdateState.Downloaded {
                    appUpdateManager.completeUpdate()
                }
            }
        }
    }

    fun cleanup() {
        appUpdateManager.unregisterListener(installStateListener)
    }

    companion object {
        private const val TAG = "InAppUpdateManager"
    }
}
