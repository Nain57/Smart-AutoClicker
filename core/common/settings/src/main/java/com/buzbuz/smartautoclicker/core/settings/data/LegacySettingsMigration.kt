
package com.buzbuz.smartautoclicker.core.settings.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first


internal class LegacySettingsMigration(
    context: Context,
    ioDispatcher: CoroutineDispatcher,
) : DataMigration<Preferences> {

    private val legacyDataStore: PreferencesDataStore =
        PreferencesDataStore(context, ioDispatcher, LEGACY_PREFERENCES_FILE_NAME)

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