package com.arshadshah.nimaz.services

import android.app.Activity
import android.content.Context
import com.arshadshah.nimaz.repositories.UpdateRepository

class UpdateService(context: Context) {

    private val updateRepository = UpdateRepository(context)

    fun checkForUpdate(doUpdate: Boolean, onUpdateAvailable: (Boolean) -> Unit) {
        updateRepository.checkForUpdate(doUpdate, onUpdateAvailable)
    }

    fun startUpdateFlowForResult(activity: Activity, requestCode: Int) {
        updateRepository.startUpdateFlowForResult(activity, requestCode)
    }
}
