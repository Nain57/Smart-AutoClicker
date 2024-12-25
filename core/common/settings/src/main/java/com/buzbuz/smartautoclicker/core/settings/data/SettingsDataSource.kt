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
package com.buzbuz.smartautoclicker.core.settings.data

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
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class SettingsDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    internal companion object {
        const val PREFERENCES_FILE_NAME = "settings"

        val KEY_IS_LEGACY_ACTION_UI: Preferences.Key<Boolean> =
            booleanPreferencesKey("isLegacyActionUiEnabled")
        val KEY_IS_LEGACY_NOTIFICATION_UI: Preferences.Key<Boolean> =
            booleanPreferencesKey("isLegacyNotificationUiEnabled")
    }

    private val coroutineScope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val dataStore: DataStore<Preferences> = create(
        corruptionHandler = ReplaceFileCorruptionHandler { onPreferenceFileCorrupted() },
        migrations = listOf(LegacySettingsMigration(context, coroutineScope)),
        scope = coroutineScope,
        produceFile = { context.preferencesDataStoreFile(PREFERENCES_FILE_NAME) }
    )

    internal fun isLegacyActionUiEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_LEGACY_ACTION_UI] ?: false }

    internal suspend fun toggleLegacyActionUi() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_LEGACY_ACTION_UI] = !(preferences[KEY_IS_LEGACY_ACTION_UI] ?: false)
        }

    internal fun isLegacyNotificationUiEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_LEGACY_NOTIFICATION_UI] ?: false }

    internal suspend fun toggleLegacyNotificationUi() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_LEGACY_NOTIFICATION_UI] = !(preferences[KEY_IS_LEGACY_NOTIFICATION_UI] ?: false)
        }

    private fun onPreferenceFileCorrupted(): Preferences {
        Log.e(PREFERENCES_FILE_NAME, "Preference file is corrupted, resetting preferences")
        return emptyPreferences()
    }
}