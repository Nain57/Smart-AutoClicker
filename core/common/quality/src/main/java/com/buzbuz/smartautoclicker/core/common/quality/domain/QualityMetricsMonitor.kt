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