
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.starters

import android.content.Context
import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.common.permissions.PermissionsController
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification
import com.buzbuz.smartautoclicker.feature.smart.config.R

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RequestNotificationPermissionActivity : AppCompatActivity() {

    companion object {
        fun getStartIntent(context: Context): Intent =
            Intent(context, RequestNotificationPermissionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    @Inject lateinit var permissionController: PermissionsController
    @Inject lateinit var overlayManager: OverlayManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transparent)

        overlayManager.hideAll()
        permissionController.startPermissionsUiFlow(
            this,
            listOf(PermissionPostNotification()),
            onAllGranted = ::finishActivity,
            onMandatoryDenied = ::finishActivity,
        )
    }

    private fun finishActivity() {
        overlayManager.restoreVisibility()
        overlayManager.navigateUp(this)
        finish()
    }
}