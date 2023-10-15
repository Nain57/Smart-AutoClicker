/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.activity

import android.content.DialogInterface
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.activity.list.ScenarioListFragment
import com.buzbuz.smartautoclicker.activity.list.ScenarioListUiState
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Entry point activity for the application.
 * Shown when the user clicks on the launcher icon for the application, this activity will displays the list of
 * available scenarios, if any.
 */
class ScenarioActivity : AppCompatActivity(), ScenarioListFragment.Listener, PermissionsDialogFragment.Listener {

    /** ViewModel providing the click scenarios data to the UI. */
    private val scenarioViewModel: ScenarioViewModel by viewModels()

    /** The result launcher for the projection permission dialog. */
    private lateinit var projectionActivityResult: ActivityResultLauncher<Intent>

    /** Scenario clicked by the user. */
    private var requestedItem: ScenarioListUiState.Item? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scenario)

        scenarioViewModel.stopScenario()

        projectionActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                Toast.makeText(this, R.string.toast_denied_screen_sharing_permission, Toast.LENGTH_SHORT).show()
            } else {
                (requestedItem as? ScenarioListUiState.Item.Valid.Smart)?.let { item ->
                    val started = scenarioViewModel.loadScenario(this, result.resultCode, result.data!!, item.scenario)

                    if (started) finish()
                    else Toast.makeText(this, R.string.toast_denied_foreground_permission, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun startScenario(item: ScenarioListUiState.Item) {
        requestedItem = item

        if (!scenarioViewModel.arePermissionsGranted()) {
            showPermissionsDialog()
            return
        }

        when (val scenario = item.scenario) {
            is DumbScenario -> scenarioViewModel.loadDumbScenario(scenario)
            is Scenario -> showMediaProjectionWarning()
        }
    }

    override fun onPermissionsGranted() {
        requestedItem?.let { item ->
            when (val scenario = item.scenario) {
                is DumbScenario -> scenarioViewModel.loadDumbScenario(scenario)
                is Scenario -> showMediaProjectionWarning()
                else -> Unit
            }
        }
    }

    private fun showPermissionsDialog() {
        PermissionsDialogFragment.newInstance().show(
            supportFragmentManager,
            PermissionsDialogFragment.FRAGMENT_TAG_PERMISSION_DIALOG,
        )
    }

    /** Show the media projection start warning. */
    private fun showMediaProjectionWarning() {
        ContextCompat.getSystemService(this, MediaProjectionManager::class.java)
            ?.let { projectionManager ->
            // The component name defined in com.android.internal.R.string.config_mediaProjectionPermissionDialogComponent
            // specifying the dialog to start to request the permission is invalid on some devices (Chinese Honor6X Android 10).
            // There is nothing to do in those cases, the app can't be used.
            try {
                projectionActivityResult.launch(projectionManager.createScreenCaptureIntent())
            } catch (npe: NullPointerException) {
                showUnsupportedDeviceDialog()
            }
        }
    }

    /**
     * Some devices messes up too much with Android.
     * Display a dialog in those cases and stop the application.
     */
    private fun showUnsupportedDeviceDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.message_error_screen_capture_permission_dialog_not_found)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }
}
