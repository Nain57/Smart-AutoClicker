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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers
import com.buzbuz.smartautoclicker.core.common.quality.data.INVALID_TIME
import com.buzbuz.smartautoclicker.core.common.quality.data.KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT
import com.buzbuz.smartautoclicker.core.common.quality.data.KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT
import com.buzbuz.smartautoclicker.core.common.quality.data.KEY_LAST_SERVICE_FOREGROUND_TIME_MS
import com.buzbuz.smartautoclicker.core.common.quality.data.KEY_LAST_SERVICE_START_TIME_MS
import com.buzbuz.smartautoclicker.core.common.quality.data.QualityMetrics
import com.buzbuz.smartautoclicker.core.common.quality.data.inc
import com.buzbuz.smartautoclicker.core.common.quality.data.put
import com.buzbuz.smartautoclicker.core.common.quality.data.qualityMetrics

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
    private val qualityDataStore: DataStore<Preferences>,
    @Dispatcher(HiltCoroutineDispatchers.IO) ioDispatcher: CoroutineDispatcher,
) {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    internal val currentQualityMetrics: Flow<QualityMetrics> = qualityDataStore
        .qualityMetrics()

    internal val startingQualityMetrics: Flow<QualityMetrics> = currentQualityMetrics
        .take(1)
        .shareIn(coroutineScopeIo, SharingStarted.Eagerly)

    fun onServiceConnected() {
        coroutineScopeIo.launch {
            qualityDataStore.edit { preferences ->
                val serviceStartTime = preferences[KEY_LAST_SERVICE_START_TIME_MS] ?: INVALID_TIME
                val lastForegroundStartTime = preferences[KEY_LAST_SERVICE_FOREGROUND_TIME_MS] ?: INVALID_TIME

                if (serviceStartTime != INVALID_TIME && lastForegroundStartTime == INVALID_TIME) {
                    val lossCount = preferences[KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT] ?: 0
                    preferences[KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT] = lossCount + 1
                }

                preferences[KEY_LAST_SERVICE_START_TIME_MS] = System.currentTimeMillis()
            }
        }
    }

    fun onServiceForegroundStart() {
        coroutineScopeIo.launch {
            qualityDataStore.put(KEY_LAST_SERVICE_FOREGROUND_TIME_MS, System.currentTimeMillis())
        }
    }

    fun onServiceForegroundEnd() {
        coroutineScopeIo.launch {
            qualityDataStore.put(KEY_LAST_SERVICE_FOREGROUND_TIME_MS, INVALID_TIME)
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
            qualityDataStore.inc(KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT)
        }
    }
}

private const val TAG = "QualityMonitor"