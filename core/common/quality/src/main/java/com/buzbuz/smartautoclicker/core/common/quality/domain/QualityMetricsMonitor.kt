
package com.buzbuz.smartautoclicker.core.common.quality.domain

import android.util.Log

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers
import com.buzbuz.smartautoclicker.core.common.quality.data.INVALID_TIME
import com.buzbuz.smartautoclicker.core.common.quality.data.QualityDataSource
import com.buzbuz.smartautoclicker.core.common.quality.data.QualityMetrics

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QualityMetricsMonitor @Inject constructor(
    private val qualityDataSource: QualityDataSource,
    @Dispatcher(HiltCoroutineDispatchers.IO) ioDispatcher: CoroutineDispatcher,
) {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    internal val currentQualityMetrics: Flow<QualityMetrics> = qualityDataSource
        .qualityMetrics()

    internal val startingQualityMetrics: Flow<QualityMetrics> = currentQualityMetrics
        .take(1)
        .shareIn(coroutineScopeIo, SharingStarted.Eagerly)

    fun onServiceConnected() {
        coroutineScopeIo.launch {
            qualityDataSource.edit { currentMetrics ->

                var newLossCount = currentMetrics.accessibilityLossCount
                if (currentMetrics.lastServiceStartTimeMs != INVALID_TIME
                    && currentMetrics.lastScenarioStartTimeMs == INVALID_TIME) {
                    newLossCount += 1
                }

                currentMetrics.copy(
                    lastServiceStartTimeMs = System.currentTimeMillis(),
                    accessibilityLossCount = newLossCount,
                )
            }
        }
    }

    fun onServiceForegroundStart() {
        coroutineScopeIo.launch {
            qualityDataSource.edit { currentMetrics ->
                currentMetrics.copy(lastScenarioStartTimeMs = System.currentTimeMillis())
            }
        }
    }

    fun onServiceForegroundEnd() {
        coroutineScopeIo.launch {
            qualityDataSource.edit { currentMetrics ->
                currentMetrics.copy(lastScenarioStartTimeMs = INVALID_TIME)
            }
        }
    }

    fun onServiceUnbind() {
        Log.w(TAG, "Accessibility service is unbound. If you haven't touched the accessibility permission, this " +
                "means your Android device manufacturer does not comply with Android standards and have decided to kill" +
                "Smart AutoClicker."
        )
    }

    internal fun onTroubleshootingDisplayed() {
        coroutineScopeIo.launch {
            qualityDataSource.edit { currentMetrics ->
                currentMetrics.copy(troubleshootingDisplayCount = currentMetrics.troubleshootingDisplayCount + 1)
            }
        }
    }
}

private const val TAG = "QualityMonitor"