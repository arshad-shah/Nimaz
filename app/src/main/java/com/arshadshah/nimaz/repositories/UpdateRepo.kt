package com.arshadshah.nimaz.repositories

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class UpdateRepository(context: Context) {

    private val appUpdateManager = AppUpdateManagerFactory.create(context)

    fun checkForUpdate(doUpdate: Boolean, onUpdateAvailable: (Boolean) -> Unit) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (doUpdate) {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    onUpdateAvailable(true)
                } else {
                    onUpdateAvailable(false)
                }
            } else {
                onUpdateAvailable(
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                )
            }
        }
    }

    fun startUpdateFlowForResult(activity: Activity, requestCode: Int) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if ((appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) || appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    requestCode
                )
            }
        }
    }
}