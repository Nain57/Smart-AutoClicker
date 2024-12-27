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
package com.buzbuz.smartautoclicker.core.common.quality.data

import android.content.Context

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QualityDataSource @Inject internal constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher
) {

    private companion object {
        const val PREFERENCES_FILE_NAME = "quality"

        val KEY_LAST_SERVICE_START_TIME_MS: Preferences.Key<Long> =
            longPreferencesKey("lastServiceStartTimeMs")
        val KEY_LAST_SERVICE_FOREGROUND_TIME_MS: Preferences.Key<Long> =
            longPreferencesKey("lastServiceForegroundTimeMs")
        val KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT: Preferences.Key<Int> =
            intPreferencesKey("accessibilityPermissionLossCount")
        val KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT: Preferences.Key<Int> =
            intPreferencesKey("accessibilityTroubleshootingDialogDisplayCount")
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(context, ioDispatcher, PREFERENCES_FILE_NAME)

    internal fun qualityMetrics(): Flow<QualityMetrics> =
        dataStore.data.map { preferences -> preferences.toQualityMetrics() }

    internal suspend fun edit(transform: suspend (current: QualityMetrics) -> QualityMetrics) {
        dataStore.edit { preferences ->
            preferences.putQualityMetrics(transform(preferences.toQualityMetrics()))
        }
    }

    private fun Preferences.toQualityMetrics(): QualityMetrics =
        QualityMetrics(
            lastServiceStartTimeMs = get(KEY_LAST_SERVICE_START_TIME_MS) ?: INVALID_TIME,
            lastScenarioStartTimeMs = get(KEY_LAST_SERVICE_FOREGROUND_TIME_MS) ?: INVALID_TIME,
            accessibilityLossCount = get(KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT) ?: 0,
            troubleshootingDisplayCount = get(KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT) ?: 0,
        )

    private fun MutablePreferences.putQualityMetrics(qualityMetrics: QualityMetrics) {
        set(KEY_LAST_SERVICE_START_TIME_MS, qualityMetrics.lastServiceStartTimeMs)
        set(KEY_LAST_SERVICE_FOREGROUND_TIME_MS, qualityMetrics.lastScenarioStartTimeMs)
        set(KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT, qualityMetrics.accessibilityLossCount)
        set(KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT, qualityMetrics.troubleshootingDisplayCount)
    }
}
