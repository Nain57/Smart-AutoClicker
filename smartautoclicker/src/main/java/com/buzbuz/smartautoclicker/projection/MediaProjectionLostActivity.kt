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
package com.buzbuz.smartautoclicker.projection

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.base.extensions.showAsOverlay
import com.buzbuz.smartautoclicker.core.display.recorder.showMediaProjectionWarning

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MediaProjectionLostActivity : AppCompatActivity() {

    companion object {

        fun getStartIntent(context: Context): Intent =
            Intent(context, MediaProjectionLostActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    private val viewModel: MediaProjectionLostViewModel by viewModels()

    /** The result launcher for the projection permission dialog. */
    private lateinit var projectionActivityResult: ActivityResultLauncher<Intent>
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_projection_lost)

        projectionActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != RESULT_OK) {
                    finish()
                    return@registerForActivityResult
                }

                Log.i(TAG, "Media projection us running, start scenario")

                viewModel.startSmartScenario(result.resultCode, result.data!!)
                dialog?.dismiss()
                finish()
            }

        dialog = showProjectionLostDialog()
    }

    override fun onStop() {
        super.onStop()
        dialog?.dismiss()
    }

    private fun showProjectionLostDialog(): AlertDialog {
        return MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.message_error_media_projection_lost)
            .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                projectionActivityResult.showMediaProjectionWarning(this) { finish() }
            }
            .setNegativeButton(R.string.no) { _: DialogInterface, _: Int ->
                viewModel.stopApp()
            }
            .create().also { it.showAsOverlay() }
    }
}

private const val TAG = "MediaProjectionLostActivity"