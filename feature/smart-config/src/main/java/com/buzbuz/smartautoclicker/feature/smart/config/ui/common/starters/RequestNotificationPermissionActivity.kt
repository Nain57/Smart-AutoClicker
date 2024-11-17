/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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