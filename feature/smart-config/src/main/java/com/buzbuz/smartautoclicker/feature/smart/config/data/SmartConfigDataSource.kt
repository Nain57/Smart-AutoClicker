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
package com.buzbuz.smartautoclicker.feature.smart.config.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory.create
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartConfigDataSource @Inject internal constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher
) {

    private companion object {
        const val PREFERENCES_FILE_NAME = "smartConfig"

        val KEY_LEGACY_ACTION_UI_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("isLegacyActionUiEnabled")
    }

    private val dataStore: DataStore<Preferences> = create(
        corruptionHandler = ReplaceFileCorruptionHandler { onPreferenceFileCorrupted() },
        migrations = emptyList(),
        scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(PREFERENCES_FILE_NAME) }
    )

    internal fun isLegacyActionUiEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_LEGACY_ACTION_UI_ENABLED] ?: false }

    internal suspend fun toggleLegacyActionUi() =
        dataStore.edit { preferences ->
            preferences[KEY_LEGACY_ACTION_UI_ENABLED] = !(preferences[KEY_LEGACY_ACTION_UI_ENABLED] ?: false)
        }

    private fun onPreferenceFileCorrupted(): Preferences {
        Log.e(PREFERENCES_FILE_NAME, "Preference file is corrupted, resetting preferences")
        return emptyPreferences()
    }
}

