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
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.view.KeyEvent

import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.common.accessibility.domain.LocalAccessibilityService
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.engine.DumbEngine
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository
import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.mainmenu.MainMenu
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.DumbMainMenu
import com.buzbuz.smartautoclicker.feature.notifications.ServiceNotificationController
import com.buzbuz.smartautoclicker.feature.notifications.ServiceNotificationListener
import com.buzbuz.smartautoclicker.feature.revenue.IRevenueRepository
import com.buzbuz.smartautoclicker.feature.revenue.UserBillingState

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalService(
    private val context: Context,
    private val overlayManager: OverlayManager,
    private val appComponentsProvider: AppComponentsProvider,
    private val settingsRepository: SettingsRepository,
    private val smartProcessingRepository: SmartProcessingRepository,
    private val dumbEngine: DumbEngine,
    private val tutorialRepository: TutorialRepository,
    private val revenueRepository: IRevenueRepository,
    private val debuggingRepository: DebuggingRepository,
    private val onStart: (scenarioId: Long, isSmart: Boolean, foregroundNotification: Notification?) -> Unit,
    private val onStop: () -> Unit,
) : LocalAccessibilityService {

    /** Scope for this LocalService. */
    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    /** Coroutine job for the delayed start of engine & ui. */
    private var startJob: Job? = null
    /** Coroutine job for the paywall result upon start from notification. */
    private var paywallResultJob: Job? = null
    private val scenarioChangeMutex = Mutex()
    /** Updated before the delayed Dumb-engine setup, so external launches always see the active selection. */
    private var loadedDumbScenarioId: Long? = null

    /** Controls the notifications for the foreground service. */
    private val notificationController: ServiceNotificationController by lazy {
        ServiceNotificationController(
            context = context,
            appComponentsProvider = appComponentsProvider,
            settingsRepository = settingsRepository,
            listener = object : ServiceNotificationListener {
                override fun onPlay() = play()
                override fun onPause()= pause()
                override fun onShow() = showMenu()
                override fun onHide() = hideMenu()
                override fun onStop() = stopScenario()
            }
        )
    }

    /** State of this LocalService. */
    private var state: LocalServiceState = LocalServiceState(isStarted = false, isSmartLoaded = false)
    /** True if the overlay is started, false if not. */
    internal val isStarted: Boolean
        get() = state.isStarted

    override fun isSmartScreenRecordActive(): Boolean =
        state.isStarted && state.isSmartLoaded && smartProcessingRepository.isScreenRecordActive()

    override fun getSmartScenarioId(): Long? =
        if (state.isSmartLoaded) smartProcessingRepository.getScenarioId()?.databaseId else null

    override fun getDumbScenarioId(): Long? =
        if (state.isStarted && !state.isSmartLoaded) loadedDumbScenarioId else null

    init {
        combine(dumbEngine.isRunning, smartProcessingRepository.detectionState) { dumbIsRunning, smartState ->
            dumbIsRunning || smartState == DetectionState.DETECTING
        }.onEach { isRunning ->
            notificationController.updateNotification(context, isRunning, !overlayManager.isOverlayStackHidden())
        }.launchIn(serviceScope)

        overlayManager.isStackHidden
            .onEach { isStackHidden ->
                notificationController.updateNotification(
                    context,
                    dumbEngine.isRunning.value || smartProcessingRepository.isRunning(),
                    !isStackHidden
                )
            }
            .launchIn(serviceScope)
    }

    override fun launchDumbScenario(dumbScenario: DumbScenario) {
        if (state.isStarted) return
        state = LocalServiceState(isStarted = true, isSmartLoaded = false)
        loadedDumbScenarioId = dumbScenario.id.databaseId
        onStart(dumbScenario.id.databaseId, false, null)

        startJob = serviceScope.launch {
            delay(500)

            dumbEngine.init(dumbScenario)

            overlayManager.navigateTo(
                context = context,
                newOverlay = DumbMainMenu(dumbScenario.id) { stopScenario() },
            )
        }
    }

    /**
     * Start the overlay UI and instantiates the detection objects.
     *
     * This requires the media projection permission code and its data intent, they both can be retrieved using the
     * results of the activity intent provided by [MediaProjectionManager.createScreenCaptureIntent] (this Intent
     * shows the dialog warning about screen recording privacy). Any attempt to call this method without the
     * correct screen capture intent result will lead to a crash.
     *
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param scenario the identifier of the scenario of clicks to be used for detection.
     */
    override fun launchSmartScenario(resultCode: Int, data: Intent, scenario: Scenario) {
        if (isStarted) return
        state = LocalServiceState(isStarted = true, isSmartLoaded = true)

        onStart(
            scenario.id.databaseId,
            true,
            notificationController.createNotification(
                context = context,
                scenarioName = scenario.name,
                isRunning = false,
                isMenuVisible = true
            )
        )

        startJob = serviceScope.launch {
            val mainMenu = MainMenu { stopScenario() }

            smartProcessingRepository.apply {
                setScenarioId(scenario.id, markAsUsed = true)
                setProjectionErrorHandler { mainMenu.onMediaProjectionLost() }
            }

            overlayManager.navigateTo(
                context = context,
                newOverlay = mainMenu,
            )

            smartProcessingRepository.startScreenRecord(
                resultCode = resultCode,
                data = data,
            )
        }
    }

    override fun stopScenario() {
        serviceScope.launch { scenarioChangeMutex.withLock { stopAndWait() } }
    }

    override fun replaceDumbScenario(dumbScenario: DumbScenario) {
        serviceScope.launch {
            scenarioChangeMutex.withLock {
                stopAndWait()
                launchDumbScenario(dumbScenario)
            }
        }
    }

    override fun replaceSmartScenario(resultCode: Int, data: Intent, scenario: Scenario) {
        serviceScope.launch {
            scenarioChangeMutex.withLock {
                stopAndWait()
                launchSmartScenario(resultCode, data, scenario)
            }
        }
    }

    override fun replaceSmartScenarioWithCurrentProjection(scenario: Scenario) {
        serviceScope.launch {
            scenarioChangeMutex.withLock {
                if (!isSmartScreenRecordActive()) return@withLock
                smartProcessingRepository.stopDetection()
                smartProcessingRepository.setScenarioId(scenario.id, markAsUsed = true)
                notificationController.updateScenarioName(context, scenario.name)
                if (overlayManager.isStackHidden.value) overlayManager.restoreVisibility()
            }
        }
    }

    private suspend fun stopAndWait() {
        if (!isStarted) return
        state = LocalServiceState(isStarted = false, isSmartLoaded = false)
        loadedDumbScenarioId = null

        startJob?.join()
        startJob = null

        dumbEngine.release()
        overlayManager.closeAll(context)
        smartProcessingRepository.stopScreenRecord()

        onStop()
        notificationController.destroyNotification()
    }

    override fun release() {
        serviceScope.cancel()
    }

    internal fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        return overlayManager.propagateKeyEvent(event)
    }

    private fun play() {
        serviceScope.launch {
            if (state.isSmartLoaded && !smartProcessingRepository.isRunning()) {
                if (shouldStartPaywall()) startPaywall()
                else startSmartScenario()
            } else if (!state.isSmartLoaded && !dumbEngine.isRunning.value) {
                dumbEngine.startDumbScenario()
            }
        }
    }

    private fun pause() {
        serviceScope.launch {
            when {
                dumbEngine.isRunning.value -> dumbEngine.stopDumbScenario()
                smartProcessingRepository.isRunning() -> smartProcessingRepository.stopDetection()
            }
        }
    }

    private fun shouldStartPaywall(): Boolean =
        revenueRepository.userBillingState.value == UserBillingState.AD_REQUESTED &&
                !tutorialRepository.isTutorialStarted()

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
            smartProcessingRepository.startDetection(
                context = context,
                autoStopDuration = revenueRepository.consumeTrial(),
                liveDebugging = debuggingRepository.isDebugViewEnabled(),
                generateReport = debuggingRepository.isDebugReportEnabled(),
            )
        }
    }

    private fun hideMenu() {
        overlayManager.hideAll()
    }

    private fun showMenu() {
        overlayManager.restoreVisibility()
    }
}

private data class LocalServiceState(
    val isStarted: Boolean,
    val isSmartLoaded: Boolean
)
