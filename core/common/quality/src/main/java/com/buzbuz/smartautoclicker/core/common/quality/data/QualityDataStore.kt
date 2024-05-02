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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Get the datastore for the quality. */
internal val Context.qualityDataStore: DataStore<Preferences> by preferencesDataStore("quality")

internal val KEY_LAST_SERVICE_START_TIME_MS: Preferences.Key<Long> =
    longPreferencesKey("lastServiceStartTimeMs")
internal val KEY_LAST_SERVICE_FOREGROUND_TIME_MS: Preferences.Key<Long> =
    longPreferencesKey("lastServiceForegroundTimeMs")
internal val KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT: Preferences.Key<Int> =
    intPreferencesKey("accessibilityPermissionLossCount")
internal val KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT: Preferences.Key<Int> =
    intPreferencesKey("accessibilityTroubleshootingDialogDisplayCount")

internal fun DataStore<Preferences>.qualityMetrics(): Flow<QualityMetrics> =
    data.map { preferences ->
        QualityMetrics(
            lastServiceStartTimeMs = preferences[KEY_LAST_SERVICE_START_TIME_MS] ?: INVALID_TIME,
            lastScenarioStartTimeMs = preferences[KEY_LAST_SERVICE_FOREGROUND_TIME_MS] ?: INVALID_TIME,
            accessibilityLossCount = preferences[KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT] ?: 0,
            troubleshootingDisplayCount = preferences[KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT] ?: 0,
        )
    }

internal suspend fun <T> DataStore<Preferences>.put(key: Preferences.Key<T>, value: T) =
    edit { preferences -> preferences[key] = value }

internal suspend fun DataStore<Preferences>.inc(key: Preferences.Key<Int>) =
    edit { preferences -> preferences[key] = (preferences[key] ?: 0) + 1 }

