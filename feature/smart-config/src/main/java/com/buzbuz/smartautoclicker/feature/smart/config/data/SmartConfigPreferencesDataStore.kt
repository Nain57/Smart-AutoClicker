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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.smartConfigPrefsDataStore: DataStore<Preferences> by preferencesDataStore("smartConfig")

internal fun DataStore<Preferences>.isLegacyActionUiEnabled(): Flow<Boolean> =
    data.map { preferences -> preferences[KEY_LEGACY_ACTION_UI_ENABLED] ?: false }

internal suspend fun DataStore<Preferences>.toggleLegacyActionUi() =
    edit { preferences ->
        preferences[KEY_LEGACY_ACTION_UI_ENABLED] = !(preferences[KEY_LEGACY_ACTION_UI_ENABLED] ?: false)
    }

private val KEY_LEGACY_ACTION_UI_ENABLED: Preferences.Key<Boolean> =
    booleanPreferencesKey("isLegacyActionUiEnabled")
