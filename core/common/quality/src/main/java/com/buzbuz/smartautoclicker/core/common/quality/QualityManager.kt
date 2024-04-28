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
package com.buzbuz.smartautoclicker.core.common.quality

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QualityManager @Inject constructor(
    private val qualityDataStore: DataStore<Preferences>,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : Dumpable {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    /** Start time in milliseconds of the accessibility service. */
    private var lastServiceStartTimeMs: Flow<Long> =
        qualityDataStore.flowOf(
            key = KEY_LAST_SERVICE_START_TIME_MS,
            default = INVALID_TIME,
        )

    /** Start time of the last user selected scenario in the activity. */
    private var lastServiceForegroundTimeMs: Flow<Long> =
        qualityDataStore.flowOf(
            key = KEY_LAST_SERVICE_FOREGROUND_TIME_MS,
            default = INVALID_TIME,
        )

    /** The quality of the current application session. */
    private val _quality: MutableStateFlow<Quality> = MutableStateFlow(Quality.Unknown)
    val quality: StateFlow<Quality> = _quality

    fun onServiceConnected() {
        coroutineScopeIo.launch {
            startQualityMonitoring()
            qualityDataStore.edit { preferences ->
                preferences[KEY_LAST_SERVICE_START_TIME_MS] = System.currentTimeMillis()
            }
        }
    }

    fun onServiceForegroundStart() {
        coroutineScopeIo.launch {
            qualityDataStore.edit { preferences ->
                preferences[KEY_LAST_SERVICE_FOREGROUND_TIME_MS] = System.currentTimeMillis()
            }
        }
    }

    fun onServiceForegroundEnd() {
        coroutineScopeIo.launch {
            qualityDataStore.edit { preferences ->
                preferences[KEY_LAST_SERVICE_FOREGROUND_TIME_MS] = INVALID_TIME
            }
        }
    }

    fun onServiceUnbind() {
        Log.w(TAG, "Accessibility service is unbound. If you haven't touched the accessibility permission, this " +
                "means your Android device manufacturer does not comply with Android standards and have decided to kill" +
                "Smart AutoClicker."
        )
    }

    private suspend fun startQualityMonitoring() {
        val startingQuality = when {
            // Check if that's not the first time the service is started
            lastServiceStartTimeMs.first() == INVALID_TIME -> Quality.FirstTime

            // Restart is due to a crash
            lastServiceForegroundTimeMs.first() != INVALID_TIME -> {
                Log.i(TAG, "Smart AutoClicker has crashed during it's last session !")
                Quality.Low
            }

            // Restart is due to a permission removal
            else -> {
                Log.i(TAG, "Accessibility service permission was removed !")
                Quality.Medium
            }
        }

        _quality.emit(startingQuality)
        startingQuality.backToHighDelay?.let(::delayToHighQuality)
    }

    private fun delayToHighQuality(delayDuration: Duration) {
        coroutineScopeIo.launch {
            delay(delayDuration)

            Log.i(TAG, "Grace period expired, quality is back to High")
            _quality.emit(Quality.High)
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* QualityManager:")
            append(contentPrefix)
                .append("- lastServiceStartTimeMs=${lastServiceStartTimeMs.dumpWithTimeout()}; ")
                .append("lastScenarioStartTimeMs=${lastServiceForegroundTimeMs.dumpWithTimeout()}; ")
                .append("quality=${_quality.value}; ")
                .println()
        }
    }
}

internal const val INVALID_TIME = -1L
private const val TAG = "QualityManager"