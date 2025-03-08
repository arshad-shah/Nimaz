package com.arshadshah.nimaz.services

import android.app.Activity
import com.arshadshah.nimaz.repositories.UpdateManager
import com.arshadshah.nimaz.repositories.UpdateState
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateService @Inject constructor(
    private val updateManager: UpdateManager
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val updateState: StateFlow<UpdateState> = updateManager.updateState

    fun checkForUpdate(
        updateType: Int = AppUpdateType.IMMEDIATE,
        onResult: (Result<Boolean>) -> Unit
    ) {
        serviceScope.launch {
            onResult(updateManager.checkForUpdate(updateType))
        }
    }

    fun startUpdateFlow(
        activity: Activity,
        requestCode: Int,
        updateType: Int = AppUpdateType.IMMEDIATE,
        onResult: (Result<Unit>) -> Unit
    ) {
        serviceScope.launch {
            onResult(updateManager.startUpdateFlow(activity, requestCode, updateType))
        }
    }

    fun registerInstallStateListener(onStateUpdate: (InstallStatus) -> Unit) {
        updateManager.registerInstallStateListener(onStateUpdate)
    }

    fun unregisterInstallStateListener() {
        updateManager.unregisterInstallStateListener()
    }
}