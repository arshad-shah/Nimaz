package com.arshadshah.nimaz.repositories

import android.app.Activity
import android.content.IntentSender
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Interface defining the contract for handling app updates
 */
interface UpdateManager {
    val updateState: StateFlow<UpdateState>
    suspend fun checkForUpdate(updateType: Int = AppUpdateType.IMMEDIATE): Result<Boolean>
    suspend fun startUpdateFlow(
        activity: Activity,
        requestCode: Int,
        updateType: Int = AppUpdateType.IMMEDIATE
    ): Result<Unit>

    fun registerInstallStateListener(onStateUpdate: (InstallStatus) -> Unit)
    fun unregisterInstallStateListener()
}

/**
 * Repository responsible for handling app updates through the Google Play Core library.
 * Supports both immediate and flexible updates, with state management and error handling.
 *
 * @property appUpdateManager The Google Play Core update manager
 */
class UpdateRepository @Inject constructor(
    private val appUpdateManager: AppUpdateManager
) : UpdateManager {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val updateState: StateFlow<UpdateState> = _updateState

    private var installStateListener: InstallStateUpdatedListener? = null

    override suspend fun checkForUpdate(
        updateType: Int
    ): Result<Boolean> = runCatching {
        _updateState.value = UpdateState.Checking

        suspendCancellableCoroutine { continuation ->
            try {
                appUpdateManager.appUpdateInfo
                    .addOnSuccessListener { appUpdateInfo ->
                        val isUpdateAvailable =
                            appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                                    appUpdateInfo.isUpdateTypeAllowed(updateType)

                        _updateState.value = if (isUpdateAvailable) {
                            UpdateState.Available(appUpdateInfo.availableVersionCode())
                        } else {
                            UpdateState.NotAvailable
                        }

                        continuation.resume(isUpdateAvailable)
                    }
                    .addOnFailureListener { exception ->
                        _updateState.value = UpdateState.Error(exception)
                        continuation.resumeWithException(exception)
                    }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e)
                continuation.resumeWithException(e)
            }
        }
    }

    override suspend fun startUpdateFlow(
        activity: Activity,
        requestCode: Int,
        updateType: Int
    ): Result<Unit> = runCatching {
        _updateState.value = UpdateState.Starting

        suspendCancellableCoroutine { continuation ->
            try {
                appUpdateManager.appUpdateInfo
                    .addOnSuccessListener { appUpdateInfo ->
                        when {
                            appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                                    appUpdateInfo.isUpdateTypeAllowed(updateType) -> {
                                try {
                                    appUpdateManager.startUpdateFlowForResult(
                                        appUpdateInfo,
                                        updateType,
                                        activity,
                                        requestCode
                                    )
                                    _updateState.value = UpdateState.InProgress
                                    continuation.resume(Unit)
                                } catch (e: IntentSender.SendIntentException) {
                                    _updateState.value = UpdateState.Error(e)
                                    continuation.resumeWithException(e)
                                }
                            }

                            appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                                _updateState.value = UpdateState.InProgress
                                continuation.resume(Unit)
                            }

                            else -> {
                                _updateState.value = UpdateState.NotAvailable
                                continuation.resume(Unit)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        _updateState.value = UpdateState.Error(exception)
                        continuation.resumeWithException(exception)
                    }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e)
                continuation.resumeWithException(e)
            }
        }
    }

    override fun registerInstallStateListener(
        onStateUpdate: (InstallStatus) -> Unit
    ) {
        installStateListener = InstallStateUpdatedListener { state ->
            val installStatus = state.installStatus()
            _updateState.value = when (installStatus) {
                InstallStatus.DOWNLOADED -> UpdateState.Downloaded
                InstallStatus.FAILED -> UpdateState.Error(Exception("Installation failed"))
                InstallStatus.CANCELED -> UpdateState.Error(Exception("Installation canceled"))
                InstallStatus.INSTALLED -> UpdateState.Completed
                InstallStatus.DOWNLOADING,
                InstallStatus.PENDING,
                InstallStatus.INSTALLING -> UpdateState.InProgress

                InstallStatus.UNKNOWN -> UpdateState.Error(Exception("Unknown install status"))
                else -> {
                    UpdateState.Idle
                }
            }

        }
        installStateListener?.let { appUpdateManager.registerListener(it) }
    }

    override fun unregisterInstallStateListener() {
        installStateListener?.let { appUpdateManager.unregisterListener(it) }
        installStateListener = null
    }
}

/**
 * Represents the current state of the update process.
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object NotAvailable : UpdateState()
    data class Available(val version: Int) : UpdateState()
    object Starting : UpdateState()
    object InProgress : UpdateState()
    object Downloaded : UpdateState()
    object Completed : UpdateState()
    data class Error(val exception: Throwable) : UpdateState()
}