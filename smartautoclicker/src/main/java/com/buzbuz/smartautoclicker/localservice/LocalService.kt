
package com.buzbuz.smartautoclicker.localservice

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.view.KeyEvent

import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.domain.model.SmartActionExecutor
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.MainMenu
import com.buzbuz.smartautoclicker.feature.notifications.service.ServiceNotificationController
import com.buzbuz.smartautoclicker.feature.notifications.service.ServiceNotificationListener
import com.buzbuz.smartautoclicker.feature.smart.debugging.domain.DebuggingRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LocalService(
    private val context: Context,
    private val overlayManager: OverlayManager,
    private val appComponentsProvider: AppComponentsProvider,
    private val settingsRepository: SettingsRepository,
    private val detectionRepository: DetectionRepository,
    private val debugRepository: DebuggingRepository,
    private val androidExecutor: SmartActionExecutor,
    private val onStart: (scenarioId: Long, isSmart: Boolean, foregroundNotification: Notification?) -> Unit,
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
            appComponentsProvider = appComponentsProvider,
            settingsRepository = settingsRepository,
            listener = object : ServiceNotificationListener {
                override fun onPlay() = play()
                override fun onPause()= pause()
                override fun onShow() = showMenu()
                override fun onHide() = hideMenu()
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
        detectionRepository.detectionState
            .map { it == DetectionState.DETECTING }   // running ⇔ detecting
            .distinctUntilChanged()                    // avoid redundant notification updates
            .onEach { isRunning ->
                notificationController.updateNotification(context, isRunning, !overlayManager.isStackHidden())
            }
            .launchIn(serviceScope)

        overlayManager.onVisibilityChangedListener = {
            notificationController.updateNotification(
                context,
                detectionRepository.isRunning(),
                !overlayManager.isStackHidden()
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
            val mainMenu = MainMenu { stop() }

            detectionRepository.apply {
                setScenarioId(scenario.id, markAsUsed = true)
                setExecutor(androidExecutor)
                setProjectionErrorHandler { mainMenu.onMediaProjectionLost() }
            }

            overlayManager.navigateTo(
                context = context,
                newOverlay = mainMenu,
            )

            detectionRepository.startScreenRecord(
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

            overlayManager.closeAll(context)
            detectionRepository.stopScreenRecord()

            onStop()
            notificationController.destroyNotification()
        }
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
            if (state.isSmartLoaded && !detectionRepository.isRunning()) {
                startSmartScenario()
            }
        }
    }

    private fun pause() {
        serviceScope.launch {
            when {
                detectionRepository.isRunning() -> detectionRepository.stopDetection()
            }
        }
    }

    private fun startSmartScenario() {
        serviceScope.launch {
            detectionRepository.startDetection(
                context,
                debugRepository.getDebugDetectionListenerIfNeeded(context),
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