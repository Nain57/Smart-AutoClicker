/*
 * Copyright (C) 2020 Nain57
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
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.database.ClickScenario
import com.buzbuz.smartautoclicker.model.ScenarioViewModel
import com.buzbuz.smartautoclicker.SmartAutoClickerService

/**
 * Entry point activity for the application.
 * Shown when the user clicks on the launcher icon for the application, this activity will displays the list of
 * available scenarios, if any. Upon selection of a scenario, if a permission is missing, the
 * [PermissionsDialogFragment] will be shown. Once all permissions are granted, the media projection start notification
 * is shown and if the user accept it, this activity is automatically closed, and the overlay menu will is shown.
 */
class ScenarioActivity : AppCompatActivity(), ScenarioListFragment.OnScenarioClickedListener,
    PermissionsDialogFragment.PermissionDialogListener {

    companion object {
        /** Tag for logs. */
        const val TAG = "MainActivity"
        /** Permission request code for the screen sharing. */
        const val SCREEN_SHARING_PERMISSION_REQUEST_CODE = 1234
    }

    /** ViewModel providing the click scenarios data to the UI. */
    private val scenarioViewModel: ScenarioViewModel by viewModels()

    /** Scenario clicked by the user. */
    private var requestedScenario: ClickScenario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scenario)
        supportActionBar?.title = resources.getString(R.string.activity_scenario_title)
        scenarioViewModel.stopScenario()
    }

    override fun onDestroy() {
        super.onDestroy()
        SmartAutoClickerService.getLocalService(null)
    }

    override fun onClicked(scenario: ClickScenario) {
        requestedScenario = scenario

        if (!scenarioViewModel.arePermissionsGranted()) {
            PermissionsDialogFragment.newInstance().show(supportFragmentManager, "fragment_edit_name")
            return
        }

        onPermissionsGranted()
    }

    override fun onPermissionsGranted() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            SCREEN_SHARING_PERMISSION_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SCREEN_SHARING_PERMISSION_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "User denied screen sharing permission", Toast.LENGTH_SHORT).show()
                return
            }

            scenarioViewModel.loadScenario(resultCode, data!!, requestedScenario!!)
            finish()
        } else {
            Log.e(TAG, "Invalid request code: $requestCode")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
