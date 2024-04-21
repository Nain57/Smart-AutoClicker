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
package com.buzbuz.smartautoclicker

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.view.KeyEvent

import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.bitmaps.IBitmapManager
import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.engine.DumbEngine
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.smart.config.ui.MainMenu
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.DumbMainMenu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocalService(
    private val context: Context,
    private val overlayManager: OverlayManager,
    private val displayMetrics: DisplayMetrics,
    private val detectionRepository: DetectionRepository,
    private val bitmapManager: IBitmapManager,
    private val dumbEngine: DumbEngine,
    private val androidExecutor: AndroidExecutor,
    private val onStart: (isSmart: Boolean, name: String) -> Unit,
    private val onStop: () -> Unit,
) : SmartAutoClickerService.ILocalService {

    /** Scope for this LocalService. */
    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    /** Coroutine job for the delayed start of engine & ui. */
    private var startJob: Job? = null
    /** True if the overlay is started, false if not. */
    internal var isStarted: Boolean = false

    override fun startDumbScenario(dumbScenario: DumbScenario) {
        if (isStarted) return
        isStarted = true
        onStart(false, dumbScenario.name)

        displayMetrics.startMonitoring(context)
        startJob = serviceScope.launch {
            delay(500)

            dumbEngine.init(androidExecutor, dumbScenario)

            overlayManager.navigateTo(
                context = context,
                newOverlay = DumbMainMenu(dumbScenario.id) { stop() },
            )
        }
    }

    /**
     * Start the overlay UI and instantiates the detection objects.
     *
     * This requires the media projection permission code and its data intent, they both can be retrieved using the
     * results of the activity intent provided by [MediaProjectionManager.createScreenCaptureIntent] (this Intent
     * shows the dialog warning about screen recording privacy). Any attempt to call this method without the
     * correct screen capture intent result will leads to a crash.
     *
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param scenario the identifier of the scenario of clicks to be used for detection.
     */
    override fun startSmartScenario(resultCode: Int, data: Intent, scenario: Scenario) {
        if (isStarted) return
        isStarted = true
        onStart(true, scenario.name)

        displayMetrics.startMonitoring(context)
        startJob = serviceScope.launch {
            delay(500)

            detectionRepository.setScenarioId(scenario.id)

            overlayManager.navigateTo(
                context = context,
                newOverlay = MainMenu { stop() },
            )

            // If we start too quickly, there is a chance of crash because the service isn't in foreground state yet
            // That's not really an issue as the user just clicked the permission button and the activity is closing
            delay(500)
            detectionRepository.startScreenRecord(
                context = context,
                resultCode = resultCode,
                data = data,
                androidExecutor = androidExecutor,
            )
        }
    }

    override fun stop() {
        if (!isStarted) return
        isStarted = false

        serviceScope.launch {
            startJob?.join()
            startJob = null

            dumbEngine.release()
            overlayManager.closeAll(context)
            detectionRepository.stopScreenRecord()
            displayMetrics.stopMonitoring(context)
            bitmapManager.releaseCache()

            onStop()
        }
    }

    override fun release() {
        serviceScope.cancel()
    }

    fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        return overlayManager.propagateKeyEvent(event)
    }

    fun toggleOverlaysVisibility() {
        overlayManager.apply {
            if (isStackHidden()) {
                restoreVisibility()
            } else {
                hideAll()
            }
        }
    }
}