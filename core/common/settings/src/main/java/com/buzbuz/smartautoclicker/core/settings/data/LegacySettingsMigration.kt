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
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory.create
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first


internal class LegacySettingsMigration(
    context: Context,
    ioScope: CoroutineScope,
) : DataMigration<Preferences> {

    private val legacyDataStore: DataStore<Preferences> = create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = emptyList(),
        scope = ioScope,
        produceFile = { context.preferencesDataStoreFile(LEGACY_PREFERENCES_FILE_NAME) },
    )

    // Specify your condition for whether the migration should happen
    override suspend fun shouldMigrate(currentData: Preferences) = true

    // Instruction on how exactly the old data is transformed into new data
    override suspend fun migrate(currentData: Preferences): Preferences {
        val oldData = legacyDataStore.data.first()[KEY_IS_LEGACY_ACTION_UI] ?: return currentData

        val currentMutablePrefs = currentData.toMutablePreferences()
        currentMutablePrefs[SettingsDataSource.KEY_IS_LEGACY_ACTION_UI] = oldData
        return currentMutablePrefs.toPreferences()
    }

    // Once the migration is over, clean up the old storage
    override suspend fun cleanUp() {
        legacyDataStore.edit { it.clear() }
    }

}

private const val LEGACY_PREFERENCES_FILE_NAME = "smartConfig"
val KEY_IS_LEGACY_ACTION_UI: Preferences.Key<Boolean> =
    booleanPreferencesKey("isLegacyActionUiEnabled")

private const val TAG = "LegacySettingsMigration"