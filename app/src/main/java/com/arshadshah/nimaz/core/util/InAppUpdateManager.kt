package com.arshadshah.nimaz.core.util

import android.app.Activity
import android.util.Log
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
    data object Downloading : UpdateState
    data class Downloaded(val completeUpdate: () -> Unit) : UpdateState
    data object NoUpdateAvailable : UpdateState
    data class Error(val message: String) : UpdateState
}

class InAppUpdateManager(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
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

            else -> {}
        }
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
            _updateState.value = UpdateState.Error(e.message ?: "Update check failed")
        }
    }

    fun startUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    activity,
                    AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                    UPDATE_REQUEST_CODE
                )
            }
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
        const val UPDATE_REQUEST_CODE = 1001
    }
}
