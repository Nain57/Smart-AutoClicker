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
package com.buzbuz.smartautoclicker.core.common.quality.domain

import android.util.Log
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout
import com.buzbuz.smartautoclicker.core.common.quality.data.INVALID_TIME
import com.buzbuz.smartautoclicker.core.common.quality.data.QualityMetrics
import com.buzbuz.smartautoclicker.core.common.quality.ui.AccessibilityTroubleshootingDialog
import com.buzbuz.smartautoclicker.core.common.quality.ui.AccessibilityTroubleshootingDialog.Companion.FRAGMENT_RESULT_KEY_TROUBLESHOOTING
import com.buzbuz.smartautoclicker.core.common.quality.ui.AccessibilityTroubleshootingDialog.Companion.FRAGMENT_TAG_TROUBLESHOOTING_DIALOG

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QualityRepository @Inject constructor(
    private val qualityMetricsMonitor: QualityMetricsMonitor,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    @Dispatcher(Main) private val mainDispatcher: CoroutineDispatcher,
) : Dumpable {

    private companion object {
        const val TAG = "QualityManager"
        val TS_DIALOG_DISPLAY_AT_LOSS_COUNT = listOf(1, 3, 7)
    }

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    /** Tells if the troubleshooting dialog has already been displayed for this user session. */
    private var isTroubleshootingDialogDisplayed: Boolean = false

    /** The quality at the start of the application session. */
    private val startingQuality: Flow<Quality> = qualityMetricsMonitor.startingQualityMetrics
        .map { metrics -> metrics.toQuality() }

    /** The quality of the current application session. */
    private val _quality: MutableStateFlow<Quality> = MutableStateFlow(Quality.Unknown)
    val quality: StateFlow<Quality> = _quality

    init { monitorQuality(startingQuality, _quality).launchIn(coroutineScopeIo) }

    /**
     * Starts the ui flow for the troubleshooting, if the user needs it.
     * Once the user has dismissed the ui, [onCompleted] will be called. If the ui doesn't needs to be shown,
     * [onCompleted] will be called immediately.
     */
    fun startTroubleshootingUiFlowIfNeeded(activity: AppCompatActivity, onCompleted: () -> Unit) {
        // If the permission has not been removed, or if the dialog has already been displayed, complete.
        if (quality.value != Quality.ExternalIssue || isTroubleshootingDialogDisplayed) {
            onCompleted()
            return
        }

        coroutineScopeIo.launch {
            val metrics = qualityMetricsMonitor.currentQualityMetrics.first()

            TS_DIALOG_DISPLAY_AT_LOSS_COUNT.forEachIndexed { index, triggerLossCount ->
                // Permission has been lost less times than the current threshold, no need for the dialog
                if (metrics.accessibilityLossCount < triggerLossCount) {
                    withContext(mainDispatcher) { onCompleted() }
                    return@launch
                }

                // The dialog has already been displayed for this iteration
                if (metrics.troubleshootingDisplayCount > index) return@forEachIndexed

                Log.i(TAG, "Starting troubleshooting dialog, " +
                        "lossCount=${metrics.accessibilityLossCount}; displayCount=${metrics.troubleshootingDisplayCount}")
                startTroubleshootingDialog(activity, onCompleted)
                return@launch
            }

            withContext(mainDispatcher) { onCompleted() }
        }
    }

    private fun monitorQuality(startingQuality: Flow<Quality>, currentQuality: MutableStateFlow<Quality>) : Flow<Quality> =
        startingQuality.onEach { quality ->
            currentQuality.emit(quality)

            quality.backToHighDelay?.let { backToHighDuration ->
                delay(backToHighDuration)

                Log.i(TAG, "Grace period expired, quality is back to High")
                currentQuality.emit(Quality.High)
            }
        }

    private suspend fun startTroubleshootingDialog(activity: AppCompatActivity, onCompleted: () -> Unit) {
        isTroubleshootingDialogDisplayed = true
        qualityMetricsMonitor.onTroubleshootingDisplayed()

        withContext(mainDispatcher) {
            activity.supportFragmentManager
                .setFragmentResultListener(FRAGMENT_RESULT_KEY_TROUBLESHOOTING, activity) { _, _ -> onCompleted() }
            AccessibilityTroubleshootingDialog().show(activity.supportFragmentManager, FRAGMENT_TAG_TROUBLESHOOTING_DIALOG)
        }
    }

    private fun QualityMetrics.toQuality(): Quality = when {
        // Check if that's not the first time the service is started
        lastServiceStartTimeMs == INVALID_TIME ->
            Quality.FirstTime

        // Restart is due to a crash
        lastScenarioStartTimeMs != INVALID_TIME -> {
            Log.i(TAG, "Smart AutoClicker has crashed during it's last session !")
            Quality.Crashed
        }

        // Restart is due to a permission removal
        else -> {
            Log.i(TAG, "Accessibility service permission was removed !")
            Quality.ExternalIssue
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* QualityManager:")
            append(contentPrefix)
                .append("- currentQualityMetrics=${qualityMetricsMonitor.currentQualityMetrics.dumpWithTimeout()}; ")
                .append("startingQuality=${startingQuality.dumpWithTimeout()}; ")
                .append("quality=${_quality.value}; ")
                .println()
        }
    }
}
