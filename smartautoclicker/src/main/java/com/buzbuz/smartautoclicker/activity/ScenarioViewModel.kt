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
package com.buzbuz.smartautoclicker.activity

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build

import androidx.core.content.PermissionChecker
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.SmartAutoClickerService
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.revenue.IRevenueRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** AndroidViewModel for create/delete/list click scenarios from an LifecycleOwner. */
@HiltViewModel
class ScenarioViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val revenueRepository: IRevenueRepository,
) : ViewModel() {

    /** Callback upon the availability of the [SmartAutoClickerService]. */
    private val serviceConnection: (SmartAutoClickerService.ILocalService?) -> Unit = { localService ->
        clickerService = localService
    }

    /**
     * Reference on the [SmartAutoClickerService].
     * Will be not null only if the Accessibility Service is enabled.
     */
    private var clickerService: SmartAutoClickerService.ILocalService? = null
    /** The Android notification manager. Initialized only if needed.*/
    private val notificationManager: NotificationManager?

    init {
        SmartAutoClickerService.getLocalService(serviceConnection)

        notificationManager =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                context.getSystemService(NotificationManager::class.java)
            else null
    }

    override fun onCleared() {
        SmartAutoClickerService.getLocalService(null)
        super.onCleared()
    }

    fun requestUserConsent(activity: Activity) {
        revenueRepository.startUserConsentRequestUiFlowIfNeeded(activity)
    }

    /**
     * Start the overlay UI and instantiates the detection objects for a given scenario.
     *
     * This requires the media projection permission code and its data intent, they both can be retrieved using the
     * results of the activity intent provided by
     * [android.media.projection.MediaProjectionManager.createScreenCaptureIntent] (this Intent shows the dialog
     * warning about screen recording privacy). Any attempt to call this method without the correct screen capture
     * intent result will leads to a crash.
     *
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param scenario the identifier of the scenario of clicks to be used for detection.
     */
    fun loadSmartScenario(context: Context, resultCode: Int, data: Intent, scenario: Scenario): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val foregroundPermission = PermissionChecker.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE)
            if (foregroundPermission != PermissionChecker.PERMISSION_GRANTED) return false
        }

        clickerService?.startSmartScenario(resultCode, data, scenario)
        return true
    }

    fun loadDumbScenario(context: Context, scenario: DumbScenario): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val foregroundPermission = PermissionChecker.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE)
            if (foregroundPermission != PermissionChecker.PERMISSION_GRANTED) return false
        }

        clickerService?.startDumbScenario(scenario)
        return true
    }

    /** Stop the overlay UI and release all associated resources. */
    fun stopScenario() {
        clickerService?.stop()
    }
}

