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
package com.buzbuz.smartautoclicker

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.view.KeyEvent

import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.engine.DumbEngine
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.floatingmenu.ui.MainMenu
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.DumbMainMenu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.PrintWriter

class LocalService(
    private val context: Context,
    private val androidExecutor: AndroidExecutor,
    private val onStart: (isSmart: Boolean, name: String) -> Unit,
    private val onStop: () -> Unit,
) : SmartAutoClickerService.ILocalService {

    /** Scope for this LocalService. */
    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    /** Coroutine job for the delayed start of engine & ui. */
    private var startJob: Job? = null

    /** Manages the overlays for the application. */
    private var overlayManager: OverlayManager? = null
    /** The metrics of the device screen. */
    private var displayMetrics: DisplayMetrics? = null

    /** The dumb repository controlling the dumb engine. */
    private var dumbEngine: DumbEngine? = null
    /** The smart engine for the detection. */
    private var detectionRepository: DetectionRepository? = null

    /** True if the overlay is started, false if not. */
    private var isStarted: Boolean = false

    override fun startDumbScenario(dumbScenario: DumbScenario) {
        if (isStarted) return
        isStarted = true
        onStart(false, dumbScenario.name)

        initDisplayMetrics(context)
        startJob = serviceScope.launch {
            delay(500)

            dumbEngine = DumbEngine.getInstance(context).apply {
                init(androidExecutor, dumbScenario)
            }

            initOverlayManager(
                context = context,
                rootOverlay = DumbMainMenu(dumbScenario.id) { stop() }
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

        initDisplayMetrics(context)
        startJob = serviceScope.launch {
            delay(500)

            detectionRepository = DetectionRepository.getDetectionRepository(context).apply {
                setScenarioId(scenario.id)
                startScreenRecord(
                    context = context,
                    resultCode = resultCode,
                    data = data,
                    androidExecutor = androidExecutor,
                )
            }

            initOverlayManager(
                context = context,
                rootOverlay = MainMenu { stop() }
            )
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        return overlayManager?.propagateKeyEvent(event) ?: false
    }

    override fun stop() {
        if (!isStarted) return
        isStarted = false

        serviceScope.launch {
            startJob?.join()
            startJob = null

            overlayManager?.closeAll(context)
            overlayManager = null

            detectionRepository?.stopScreenRecord()
            detectionRepository = null

            displayMetrics?.stopMonitoring(context)
            displayMetrics = null

            onStop()
        }
    }

    override fun release() {
        serviceScope.cancel()
    }

    private fun initDisplayMetrics(context: Context) {
        displayMetrics = DisplayMetrics.getInstance(context).apply {
            startMonitoring(context)
        }
    }

    private fun initOverlayManager(context: Context, rootOverlay: Overlay) {
        overlayManager = OverlayManager.getInstance(context).apply {
            navigateTo(
                context = context,
                newOverlay = rootOverlay,
            )
        }
    }

    fun dump(writer: PrintWriter) {
        writer.apply {
            writer.println("* UI:")
            val prefix = "\t"
            overlayManager?.dump(writer, prefix) ?: writer.println("$prefix None")
        }
    }
}