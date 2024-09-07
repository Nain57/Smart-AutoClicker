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

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.*
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.AndroidRuntimeException
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

import com.buzbuz.smartautoclicker.SmartAutoClickerService.Companion.LOCAL_SERVICE_INSTANCE
import com.buzbuz.smartautoclicker.SmartAutoClickerService.Companion.getLocalService
import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.extensions.requestFilterKeyEvents
import com.buzbuz.smartautoclicker.core.base.extensions.startForegroundMediaProjectionServiceCompat
import com.buzbuz.smartautoclicker.core.bitmaps.IBitmapManager
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.common.quality.domain.QualityMetricsMonitor
import com.buzbuz.smartautoclicker.core.common.quality.domain.QualityRepository
import com.buzbuz.smartautoclicker.core.display.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.engine.DumbEngine
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.feature.qstile.domain.QSTileActionHandler
import com.buzbuz.smartautoclicker.feature.qstile.domain.QSTileRepository
import com.buzbuz.smartautoclicker.feature.revenue.IRevenueRepository
import com.buzbuz.smartautoclicker.feature.smart.debugging.domain.DebuggingRepository

import dagger.hilt.android.AndroidEntryPoint
import java.io.FileDescriptor
import java.io.PrintWriter
import javax.inject.Inject

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * AccessibilityService implementation for the SmartAutoClicker.
 *
 * Started automatically by Android once the user has defined this service has an accessibility service, it provides
 * an API to start and stop the DetectorEngine correctly in order to display the overlay UI and record the screen for
 * clicks detection.
 * This API is offered through the [LocalService] class, which is instantiated in the [LOCAL_SERVICE_INSTANCE] object.
 * This system is used instead of the usual binder interface because an [AccessibilityService] already has its own
 * binder and it can't be changed. To access this local service, use [getLocalService].
 *
 * We need this service to be an accessibility service in order to inject the detected event on the currently
 * displayed activity. This injection is made by the [dispatchGesture] method, which is called everytime an event has
 * been detected.
 */
@AndroidEntryPoint
class SmartAutoClickerService : AccessibilityService(), AndroidExecutor {

    companion object {

        /** The instance of the [ILocalService], providing access for this service to the Activity. */
        private var LOCAL_SERVICE_INSTANCE: ILocalService? = null
            set(value) {
                field = value
                LOCAL_SERVICE_CALLBACK?.invoke(field)
            }
        /** Callback upon the availability of the [LOCAL_SERVICE_INSTANCE]. */
        private var LOCAL_SERVICE_CALLBACK: ((ILocalService?) -> Unit)? = null
            set(value) {
                field = value
                value?.invoke(LOCAL_SERVICE_INSTANCE)
            }

        /**
         * Static method allowing an activity to register a callback in order to monitor the availability of the
         * [ILocalService]. If the service is already available upon registration, the callback will be immediately
         * called.
         *
         * @param stateCallback the object to be notified upon service availability.
         */
        fun getLocalService(stateCallback: ((ILocalService?) -> Unit)?) {
            LOCAL_SERVICE_CALLBACK = stateCallback
        }

        fun isServiceStarted(): Boolean = LOCAL_SERVICE_INSTANCE != null
    }

    interface ILocalService {
        fun startDumbScenario(dumbScenario: DumbScenario)
        fun startSmartScenario(resultCode: Int, data: Intent, scenario: Scenario)
        fun playAndHide()
        fun pauseAndShow()
        fun show()
        fun hide()
        fun stop()
        fun release()
    }

    private val localService: LocalService?
        get() = LOCAL_SERVICE_INSTANCE as? LocalService

    private val notificationManager: ServiceNotificationManager by lazy {
        ServiceNotificationManager(
            context = this,
            actionsCallback = object : ServiceNotificationCallbacks {
                override fun onPlayAndHide() = LOCAL_SERVICE_INSTANCE?.playAndHide()
                override fun onPauseAndShow() = LOCAL_SERVICE_INSTANCE?.pauseAndShow()
                override fun onShow() = LOCAL_SERVICE_INSTANCE?.show()
                override fun onHide() = LOCAL_SERVICE_INSTANCE?.hide()
                override fun onStop() = LOCAL_SERVICE_INSTANCE?.stop()
            }
        )
    }

    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var displayConfigManager: DisplayConfigManager
    @Inject lateinit var detectionRepository: DetectionRepository
    @Inject lateinit var dumbEngine: DumbEngine
    @Inject lateinit var bitmapManager: IBitmapManager
    @Inject lateinit var qualityRepository: QualityRepository
    @Inject lateinit var qualityMetricsMonitor: QualityMetricsMonitor
    @Inject lateinit var revenueRepository: IRevenueRepository
    @Inject lateinit var tileRepository: QSTileRepository
    @Inject lateinit var debugRepository: DebuggingRepository

    private var currentScenarioName: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        qualityMetricsMonitor.onServiceConnected()
        tileRepository.setTileActionHandler(
            object : QSTileActionHandler {
                override fun isRunning(): Boolean = isServiceStarted()
                override fun startDumbScenario(dumbScenario: DumbScenario) {
                    LOCAL_SERVICE_INSTANCE?.startDumbScenario(dumbScenario)
                }
                override fun startSmartScenario(resultCode: Int, data: Intent, scenario: Scenario) {
                    LOCAL_SERVICE_INSTANCE?.startSmartScenario(resultCode, data, scenario)
                }
                override fun stop() {
                    LOCAL_SERVICE_INSTANCE?.stop()
                }
            }
        )

        LOCAL_SERVICE_INSTANCE = LocalService(
            context = this,
            overlayManager = overlayManager,
            displayConfigManager = displayConfigManager,
            detectionRepository = detectionRepository,
            dumbEngine = dumbEngine,
            tileRepository = tileRepository,
            debugRepository = debugRepository,
            revenueRepository = revenueRepository,
            bitmapManager = bitmapManager,
            androidExecutor = this,
            onStart = { isSmart, name ->
                qualityMetricsMonitor.onServiceForegroundStart()
                currentScenarioName = name
                if (isSmart) {
                    startForegroundMediaProjectionServiceCompat(
                        ServiceNotificationManager.NOTIFICATION_ID,
                        notificationManager.createNotification(this, currentScenarioName),
                    )
                }
                requestFilterKeyEvents(true)
            },
            onStop = {
                qualityMetricsMonitor.onServiceForegroundEnd()
                currentScenarioName = null
                requestFilterKeyEvents(false)
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
                notificationManager.destroyNotification(this)
            },
            onStateChanged = { isRunning, isMenuHidden ->
                notificationManager.updateNotificationState(this, isRunning, isMenuHidden)
            }
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        LOCAL_SERVICE_INSTANCE?.stop()
        LOCAL_SERVICE_INSTANCE?.release()
        LOCAL_SERVICE_INSTANCE = null

        qualityMetricsMonitor.onServiceUnbind()
        return super.onUnbind(intent)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean =
        localService?.onKeyEvent(event) ?: super.onKeyEvent(event)

    override suspend fun executeGesture(gestureDescription: GestureDescription) {
        suspendCoroutine<Unit?> { continuation ->
            try {
                dispatchGesture(
                    gestureDescription,
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) = continuation.resume(null)
                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            Log.w(TAG, "Gesture cancelled: $gestureDescription")
                            continuation.resume(null)
                        }
                    },
                    null,
                )
            } catch (rEx: RuntimeException) {
                Log.w(TAG, "System is not responsive, the user might be spamming gesture too quickly", rEx)
                continuation.resume(null)
            }
        }
    }

    override fun executeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (anfe: ActivityNotFoundException) {
            Log.w(TAG, "Can't start activity, it is not found.")
        } catch (arex: AndroidRuntimeException) {
            Log.w(TAG, "Can't start activity, Intent is invalid: $intent", arex)
        } catch (iaex: IllegalArgumentException) {
            Log.w(TAG, "Can't start activity, Intent contains invalid arguments: $intent")
        } catch (secEx: SecurityException) {
            Log.w(TAG, "Can't start activity with intent $intent, permission is denied by the system")
        }
    }

    override fun executeSendBroadcast(intent: Intent) {
        try {
            sendBroadcast(intent)
        } catch (iaex: IllegalArgumentException) {
            Log.w(TAG, "Can't send broadcast, Intent is invalid: $intent", iaex)
        }
    }

    /**
     * Dump the state of the service via adb.
     * adb shell "dumpsys activity service com.buzbuz.smartautoclicker"
     */
    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        if (writer == null) return

        writer.append("* SmartAutoClickerService:").println()
        writer.append(Dumpable.DUMP_DISPLAY_TAB)
            .append("- isStarted=").append("${(LOCAL_SERVICE_INSTANCE as? LocalService)?.isStarted ?: false}; ")
            .append("scenarioName=").append("$currentScenarioName; ")
            .println()

        displayConfigManager.dump(writer)
        bitmapManager.dump(writer)
        overlayManager.dump(writer)
        detectionRepository.dump(writer)
        dumbEngine.dump(writer)
        qualityRepository.dump(writer)

        revenueRepository.dump(writer)
    }

    override fun onInterrupt() { /* Unused */ }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Unused */ }
}

/** Tag for the logs. */
private const val TAG = "SmartAutoClickerService"