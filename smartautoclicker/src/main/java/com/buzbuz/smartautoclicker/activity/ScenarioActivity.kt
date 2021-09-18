/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.activity

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.SmartAutoClickerService
import com.buzbuz.smartautoclicker.database.domain.Scenario

/**
 * Entry point activity for the application.
 * Shown when the user clicks on the launcher icon for the application, this activity will displays the list of
 * available scenarios, if any. Upon selection of a scenario, if a permission is missing, the
 * [PermissionsDialogFragment] will be shown. Once all permissions are granted, the media projection start notification
 * is shown and if the user accept it, this activity is automatically closed, and the overlay menu will is shown.
 */
class ScenarioActivity : AppCompatActivity(), ScenarioListFragment.OnScenarioClickedListener,
    PermissionsDialogFragment.PermissionDialogListener {

    /** ViewModel providing the click scenarios data to the UI. */
    private val scenarioViewModel: ScenarioViewModel by viewModels()

    /** Starts the media projection permission dialog and handle the result. */
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    /** Scenario clicked by the user. */
    private var requestedScenario: Scenario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scenario)
        supportActionBar?.title = resources.getString(R.string.activity_scenario_title)
        scenarioViewModel.stopScenario()

        screenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != RESULT_OK) {
                Toast.makeText(this, "User denied screen sharing permission", Toast.LENGTH_SHORT).show()
            } else {
                scenarioViewModel.loadScenario(it.resultCode, it.data!!, requestedScenario!!)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SmartAutoClickerService.getLocalService(null)
    }

    override fun onClicked(scenario: Scenario) {
        requestedScenario = scenario

        if (!scenarioViewModel.arePermissionsGranted()) {
            PermissionsDialogFragment.newInstance().show(supportFragmentManager, "fragment_edit_name")
            return
        }

        onPermissionsGranted()
    }

    override fun onPermissionsGranted() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}
