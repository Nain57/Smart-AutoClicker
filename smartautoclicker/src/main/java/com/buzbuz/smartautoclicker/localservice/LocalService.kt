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
package com.buzbuz.smartautoclicker.localservice

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.view.KeyEvent

import com.buzbuz.smartautoclicker.activity.ScenarioActivity
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.SmartActionExecutor
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.engine.DumbEngine
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.MainMenu
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.DumbMainMenu
import com.buzbuz.smartautoclicker.feature.notifications.service.ServiceNotificationController
import com.buzbuz.smartautoclicker.feature.notifications.service.ServiceNotificationListener
import com.buzbuz.smartautoclicker.feature.qstile.domain.QSTileRepository
import com.buzbuz.smartautoclicker.feature.revenue.IRevenueRepository
import com.buzbuz.smartautoclicker.feature.revenue.UserBillingState
import com.buzbuz.smartautoclicker.feature.smart.debugging.domain.DebuggingRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LocalService(
    private val context: Context,
    private val overlayManager: OverlayManager,
    private val displayConfigManager: DisplayConfigManager,
    private val detectionRepository: DetectionRepository,
    private val bitmapManager: BitmapRepository,
    private val dumbEngine: DumbEngine,
    private val tileRepository: QSTileRepository,
    private val revenueRepository: IRevenueRepository,
    private val debugRepository: DebuggingRepository,
    private val androidExecutor: SmartActionExecutor,
    private val onStart: (foregroundNotification: Notification?) -> Unit,
    private val onStop: () -> Unit,
) : ILocalService {

    /** Scope for this LocalService. */
    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    /** Coroutine job for the delayed start of engine & ui. */
    private var startJob: Job? = null
    /** Coroutine job for the paywall result upon start from notification. */
    private var paywallResultJob: Job? = null

    /** Controls the notifications for the foreground service. */
    private val notificationController: ServiceNotificationController by lazy {
        ServiceNotificationController(
            context = context,
            activityPendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ScenarioActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            ),
            listener = object : ServiceNotificationListener {
                override fun onPlayAndHide() = playAndHide()
                override fun onPauseAndShow() = pauseAndShow()
                override fun onShow() = show()
                override fun onHide() = hide()
                override fun onStop() = stop()
            }
        )
    }

    /** State of this LocalService. */
    private var state: LocalServiceState = LocalServiceState(isStarted = false, isSmartLoaded = false)
    /** True if the overlay is started, false if not. */
    internal val isStarted: Boolean
        get() = state.isStarted

    init {
        combine(dumbEngine.isRunning, detectionRepository.detectionState) { dumbIsRunning, smartState ->
            dumbIsRunning || smartState == DetectionState.DETECTING
        }.onEach { isRunning ->
            notificationController.updateNotificationState(context, isRunning, overlayManager.isStackHidden())
        }.launchIn(serviceScope)

        overlayManager.onVisibilityChangedListener = {
            notificationController.updateNotificationState(
                context,
                dumbEngine.isRunning.value || detectionRepository.isRunning(),
                overlayManager.isStackHidden()
            )
        }
    }

    override fun startDumbScenario(dumbScenario: DumbScenario) {
        if (state.isStarted) return
        state = LocalServiceState(isStarted = true, isSmartLoaded = false)
        onStart(null)

        displayConfigManager.startMonitoring(context)
        tileRepository.setTileScenario(scenarioId = dumbScenario.id.databaseId, isSmart = false)
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
        state = LocalServiceState(isStarted = true, isSmartLoaded = true)

        onStart(notificationController.createNotification(context, scenario.name))

        displayConfigManager.startMonitoring(context)
        tileRepository.setTileScenario(scenarioId = scenario.id.databaseId, isSmart = true)
        startJob = serviceScope.launch {
            val mainMenu = MainMenu { stop() }

            detectionRepository.apply {
                setScenarioId(scenario.id)
                setExecutor(androidExecutor)
                setProjectionErrorHandler { mainMenu.onMediaProjectionLost() }
            }

            overlayManager.navigateTo(
                context = context,
                newOverlay = mainMenu,
            )

            detectionRepository.startScreenRecord(
                context = context,
                resultCode = resultCode,
                data = data,
            )
        }
    }

    override fun stop() {
        if (!isStarted) return
        state = LocalServiceState(isStarted = false, isSmartLoaded = false)

        serviceScope.launch {
            startJob?.join()
            startJob = null

            dumbEngine.release()
            overlayManager.closeAll(context)
            detectionRepository.stopScreenRecord()
            displayConfigManager.stopMonitoring(context)
            bitmapManager.releaseCache()

            onStop()
            notificationController.destroyNotification(context)
        }
    }

    override fun release() {
        serviceScope.cancel()
    }

    internal fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        return overlayManager.propagateKeyEvent(event)
    }

    private fun playAndHide() {
        serviceScope.launch {
            overlayManager.hideAll()

            if (state.isSmartLoaded && !detectionRepository.isRunning()) {
                if (revenueRepository.userBillingState.value == UserBillingState.AD_REQUESTED) startPaywall()
                else startSmartScenario()
            } else if (!state.isSmartLoaded && !dumbEngine.isRunning.value) {
                dumbEngine.startDumbScenario()
            }
        }
    }

    private fun startPaywall() {
        revenueRepository.startPaywallUiFlow(context)

        paywallResultJob = combine(revenueRepository.isBillingFlowInProgress, revenueRepository.userBillingState) { inProgress, state ->
            if (inProgress) return@combine

            if (state != UserBillingState.AD_REQUESTED) startSmartScenario()
            paywallResultJob?.cancel()
            paywallResultJob = null
        }.launchIn(serviceScope)
    }

    private fun startSmartScenario() {
        serviceScope.launch {
            detectionRepository.startDetection(
                context,
                debugRepository.getDebugDetectionListenerIfNeeded(context),
                revenueRepository.consumeTrial(),
            )
        }
    }

    private fun pauseAndShow() {
        serviceScope.launch {
            when {
                dumbEngine.isRunning.value -> dumbEngine.stopDumbScenario()
                detectionRepository.isRunning() -> detectionRepository.stopDetection()
            }

            overlayManager.restoreVisibility()
        }
    }

    private fun hide() {
        overlayManager.hideAll()
    }

    private fun show() {
        overlayManager.restoreVisibility()
    }
}

private data class LocalServiceState(
    val isStarted: Boolean,
    val isSmartLoaded: Boolean
)