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
package com.buzbuz.smartautoclicker.feature.qstile.ui

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.feature.qstile.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QSTileLauncherActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_SCENARIO_ID =
            "com.buzbuz.smartautoclicker.feature.qstile.ui.EXTRA_SCENARIO_ID"
        private const val EXTRA_IS_SMART_SCENARIO =
            "com.buzbuz.smartautoclicker.feature.qstile.ui.EXTRA_IS_SMART_SCENARIO"

        fun getStartIntent(context: Context, scenarioId: Long, isSmartScenario: Boolean): Intent =
            Intent(context, QSTileLauncherActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_SCENARIO_ID, scenarioId)
                .putExtra(EXTRA_IS_SMART_SCENARIO, isSmartScenario)
    }

    private val viewModel: QSTileLauncherViewModel by viewModels()
    /** The result launcher for the projection permission dialog. */
    private lateinit var projectionActivityResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qstile_launcher)

        val scenarioId = intent?.getLongExtra(EXTRA_SCENARIO_ID, -1)
        val isSmartScenario = intent?.getBooleanExtra(EXTRA_IS_SMART_SCENARIO, false)
        if (scenarioId == null || scenarioId == -1L || isSmartScenario == null) {
            Log.e(TAG, "Invalid start parameter, finish activity")
            finish()
            return
        }

        Log.i(TAG, "Start scenario from tile...")
        if (isSmartScenario) onCreateSmartScenarioLauncher(scenarioId)
        else onCreateDumbScenarioLauncher(scenarioId)
    }

    private fun onCreateDumbScenarioLauncher(scenarioId: Long) {
        viewModel.startPermissionFlowIfNeeded(
            activity = this,
            onMandatoryDenied = ::finish,
            onAllGranted = {
                Log.i(TAG, "All permissions are granted, start scenario")
                viewModel.startDumbScenario(scenarioId)
                finish()
            }
        )
    }

    private fun onCreateSmartScenarioLauncher(scenarioId: Long) {
        projectionActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                finish()
                return@registerForActivityResult
            }

            Log.i(TAG, "Media projection us running, start scenario")

            viewModel.startSmartScenario(result.resultCode, result.data!!, scenarioId)
            finish()
        }

        viewModel.startPermissionFlowIfNeeded(
            activity = this,
            onMandatoryDenied = ::finish,
            onAllGranted = ::showMediaProjectionWarning
        )
    }

    /** Show the media projection start warning. */
    private fun showMediaProjectionWarning() {
        Log.i(TAG, "All permissions are granted, request media projection")

        ContextCompat.getSystemService(this, MediaProjectionManager::class.java)
            ?.let { projectionManager ->
                // The component name defined in com.android.internal.R.string.config_mediaProjectionPermissionDialogComponent
                // specifying the dialog to start to request the permission is invalid on some devices (Chinese Honor6X Android 10).
                // There is nothing to do in those cases, the app can't be used.
                try {
                    projectionActivityResult.launch(projectionManager.createScreenCaptureIntent())
                } catch (ex: Exception) {
                    Log.e(TAG, "Can't start projection permission screen")
                    finish()
                }
            }
    }
}

private const val TAG = "QSTileLauncherActivity"