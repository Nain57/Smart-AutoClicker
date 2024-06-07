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
package com.buzbuz.smartautoclicker.feature.qstile.data

import android.content.Context

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


/** Get the datastore for the quick setting tile. */
internal val Context.qsTileConfigDataStore: DataStore<Preferences> by preferencesDataStore("qsTile")

internal fun DataStore<Preferences>.getQSTileScenarioInfo(): Flow<QSTileScenarioInfo?> =
    data.map { preferences ->
        val scenarioDbId = preferences[KEY_SCENARIO_DATABASE_ID]
        val isSmartScenario = preferences[KEY_IS_SMART_SCENARIO]

        if (scenarioDbId == null || isSmartScenario == null) null
        else QSTileScenarioInfo(scenarioDbId, isSmartScenario)
    }

internal suspend fun DataStore<Preferences>.putQSTileScenarioInfo(scenarioInfo: QSTileScenarioInfo) =
    edit { preferences ->
        preferences[KEY_SCENARIO_DATABASE_ID] = scenarioInfo.id
        preferences[KEY_IS_SMART_SCENARIO] = scenarioInfo.isSmart
    }

private val KEY_SCENARIO_DATABASE_ID: Preferences.Key<Long> =
    longPreferencesKey("scenarioDbId")
private val KEY_IS_SMART_SCENARIO: Preferences.Key<Boolean> =
    booleanPreferencesKey("isSmartScenario")