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
package com.buzbuz.smartautoclicker.ui.activity

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.clicks.database.ScenarioEntity
import com.buzbuz.smartautoclicker.service.SmartAutoClickerService

/**
 * Entry point activity for the application.
 * Shown when the user clicks on the launcher icon for the application, this activity will displays the permission
 * states, as well as the list of available scenarios, if any. Upon selection of a scenario, this activity will be
 * automatically closed, and the overlay menu will be shown.
 */
class MainActivity : AppCompatActivity(), ScenarioListFragment.OnScenarioClickedListener {

    companion object {
        /** Tag for logs. */
        const val TAG = "MainActivity"
        /** Permission request code for the screen sharing. */
        const val SCREEN_SHARING_PERMISSION_REQUEST_CODE = 1234
    }

    /** View controller for the permissions state. */
    private lateinit var configViewController: ConfigViewController
    /**
     * Reference on the [SmartAutoClickerService].
     * Will be not null only if the Accessibility Service is enabled.
     */
    private var clickerService: SmartAutoClickerService.LocalService? = null
    /** Scenario clicked by the user. */
    private var requestedScenario: ScenarioEntity? = null
    /** Callback upon the availability of the [SmartAutoClickerService]. */
    private val serviceConnection: (SmartAutoClickerService.LocalService?) -> Unit = { localService ->
        localService?.stop()
        clickerService = localService
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SmartAutoClickerService.getLocalService(serviceConnection)

        configViewController = ConfigViewController(findViewById(R.id.layout_config), ::clickerService)
        lifecycle.addObserver(configViewController)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(configViewController)
        SmartAutoClickerService.getLocalService(null)
    }

    override fun onClicked(scenario: ScenarioEntity) {
        if (!configViewController.isConfigurationValid()) {
            // TODO show error
            return
        }

        requestedScenario = scenario
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(),
            SCREEN_SHARING_PERMISSION_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_SHARING_PERMISSION_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "User denied screen sharing permission", Toast.LENGTH_SHORT).show()
                return
            }

            clickerService?.start(resultCode, data!!, requestedScenario!!)
            finish()
        } else {
            Log.e(TAG, "Invalid request code: $requestCode")
        }
    }
}
