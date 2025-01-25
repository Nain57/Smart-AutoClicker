/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.scenarios.list.sort

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.getEnum
import com.buzbuz.smartautoclicker.core.base.setEnum

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ScenarioSortConfigRepository @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    private companion object {
        const val PREFERENCES_FILE_NAME = "scenario_sort"

        val KEY_SORT_TYPE: Preferences.Key<String> =
            stringPreferencesKey("sortType")
        val KEY_SORT_INVERTED: Preferences.Key<Boolean> =
            booleanPreferencesKey("sortOrderInverted")
        val KEY_FILTER_SHOW_SMART: Preferences.Key<Boolean> =
            booleanPreferencesKey("filterShowSmart")
        val KEY_FILTER_SHOW_DUMB: Preferences.Key<Boolean> =
            booleanPreferencesKey("filterShowDumb")
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(
            context = context,
            dispatcher = ioDispatcher,
            fileName = PREFERENCES_FILE_NAME,
            migrations = emptyList()
        )

    internal fun getSortConfig(): Flow<ScenarioSortConfig> =
        dataStore.data.map { preferences ->
            ScenarioSortConfig(
                type = preferences.getEnum<ScenarioSortType>(KEY_SORT_TYPE) ?: ScenarioSortType.NAME,
                inverted = preferences[KEY_SORT_INVERTED] ?: false,
                showSmartScenario = preferences[KEY_FILTER_SHOW_SMART] ?: true,
                showDumbScenario = preferences[KEY_FILTER_SHOW_DUMB] ?: true,
            )
        }

    internal suspend fun setSortType(type: ScenarioSortType) =
        dataStore.edit { preferences ->
            preferences.setEnum(KEY_SORT_TYPE, type)
        }

    internal suspend fun setSortOrder(invertSortOrder: Boolean) =
        dataStore.edit { preferences ->
            preferences[KEY_SORT_INVERTED] = invertSortOrder
        }

    internal suspend fun setShowDumb(show: Boolean) =
        dataStore.edit { preferences ->
            preferences[KEY_FILTER_SHOW_DUMB] = show
        }

    internal suspend fun setShowSmart(show: Boolean) =
        dataStore.edit { preferences ->
            preferences[KEY_FILTER_SHOW_SMART] = show
        }
}