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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

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

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(
            context = context,
            dispatcher = ioDispatcher,
            fileName = PREFERENCES_FILE_NAME,
            migrations = listOf(LegacySettingsMigration(context, ioDispatcher))
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
}