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
        val TS_DIALOG_DISPLAY_AT_PERMISSION_LOSS_COUNTS = listOf(1, 3, 7)
    }

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

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
        if (quality.value != Quality.ExternalIssue) {
            onCompleted()
            return
        }

        coroutineScopeIo.launch {
            val metrics = qualityMetricsMonitor.currentQualityMetrics.first()

            if (metrics.accessibilityLossCount !in 1..TS_DIALOG_DISPLAY_AT_PERMISSION_LOSS_COUNTS.last()) {
                withContext(mainDispatcher) { onCompleted() }
                return@launch
            }

            for (stepDisplayCount in TS_DIALOG_DISPLAY_AT_PERMISSION_LOSS_COUNTS.indices) {
                if (metrics.accessibilityLossCount < TS_DIALOG_DISPLAY_AT_PERMISSION_LOSS_COUNTS[stepDisplayCount]
                    || metrics.troubleshootingDisplayCount > stepDisplayCount) continue

                Log.i(TAG, "Starting troubleshooting dialog, " +
                        "lossCount=${metrics.accessibilityLossCount}; displayCount=${metrics.troubleshootingDisplayCount}")

                qualityMetricsMonitor.onTroubleshootingDisplayed()
                withContext(mainDispatcher) {
                    activity.supportFragmentManager
                        .setFragmentResultListener(FRAGMENT_RESULT_KEY_TROUBLESHOOTING, activity) { _, _ -> onCompleted() }
                    AccessibilityTroubleshootingDialog().show(activity.supportFragmentManager, FRAGMENT_TAG_TROUBLESHOOTING_DIALOG)
                }
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
