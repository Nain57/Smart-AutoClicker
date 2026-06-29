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
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.display.recorder.MediaProjectionRequest
import com.buzbuz.smartautoclicker.core.ui.errors.createNoMediaProjectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RestartMediaProjectionActivity : AppCompatActivity() {

    companion object {

        fun getStartIntent(context: Context): Intent =
            Intent(context, RestartMediaProjectionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private val viewModel: RestartMediaProjectionViewModel by viewModels()

    @Inject lateinit var overlayManager: OverlayManager

    /** The result launcher for the projection permission dialog. */
    private val mediaProjectionRequest: MediaProjectionRequest = MediaProjectionRequest()

    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transparent)

        mediaProjectionRequest.registerForActivityResult(this)
        mediaProjectionRequest.showMediaProjectionWarning(
            context = this,
            forceEntireScreen = viewModel.isEntireScreenCaptureForced(),
            onSuccess = { resultCode, data -> restartScreenRecord(resultCode, data) },
            onFailure = { showProjectionDeniedToast() },
            onError = { showUnsupportedDeviceDialog() },
        )
    }

    private fun finishActivity() {
        dialog?.dismiss()
        dialog = null

        overlayManager.navigateUp(this)
        finish()
    }

    private fun restartScreenRecord(resultCode: Int, data: Intent) {
        Log.i(TAG, "Media projection permission granted, restart recording")
        viewModel.restartScreenRecord(resultCode, data)
        finishActivity()
    }

    private fun showProjectionDeniedToast() {
        Log.i(TAG, "Media projection permission rejected")
        Toast.makeText(this, R.string.toast_denied_screen_sharing_permission, Toast.LENGTH_SHORT).show()
        finishActivity()
    }

    private fun showUnsupportedDeviceDialog() {
        createNoMediaProjectionDialog { finishActivity() }.show()
    }
}

private const val TAG = "RequestMediaProjectionActivity"