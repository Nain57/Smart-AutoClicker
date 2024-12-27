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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
class QsTileConfigDataSource @Inject internal constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher
) {

    private companion object {
        const val PREFERENCES_FILE_NAME = "qsTile"

        val KEY_SCENARIO_DATABASE_ID: Preferences.Key<Long> =
            longPreferencesKey("scenarioDbId")
        val KEY_IS_SMART_SCENARIO: Preferences.Key<Boolean> =
            booleanPreferencesKey("isSmartScenario")
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(context, ioDispatcher, PREFERENCES_FILE_NAME)

    internal fun getQSTileScenarioInfo(): Flow<QSTileScenarioInfo?> =
        dataStore.data.map { preferences ->
            val scenarioDbId = preferences[KEY_SCENARIO_DATABASE_ID]
            val isSmartScenario = preferences[KEY_IS_SMART_SCENARIO]

            if (scenarioDbId == null || isSmartScenario == null) null
            else QSTileScenarioInfo(scenarioDbId, isSmartScenario)
        }

    internal suspend fun putQSTileScenarioInfo(scenarioInfo: QSTileScenarioInfo) =
        dataStore.edit { preferences ->
            preferences[KEY_SCENARIO_DATABASE_ID] = scenarioInfo.id
            preferences[KEY_IS_SMART_SCENARIO] = scenarioInfo.isSmart
        }
}
