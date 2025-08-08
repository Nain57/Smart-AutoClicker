
package com.buzbuz.smartautoclicker.core.settings.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.workarounds.isImpactedByInputBlock

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

        val KEY_IS_FILTER_SCENARIO_UI_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("isFilterScenarioUiEnabled")
        val KEY_IS_LEGACY_ACTION_UI: Preferences.Key<Boolean> =
            booleanPreferencesKey("isLegacyActionUiEnabled")
        val KEY_IS_LEGACY_NOTIFICATION_UI: Preferences.Key<Boolean> =
            booleanPreferencesKey("isLegacyNotificationUiEnabled")
        val KEY_FORCE_ENTIRE_SCREEN: Preferences.Key<Boolean> =
            booleanPreferencesKey("forceEntireScreen")
        val KEY_INPUT_BLOCK_WORKAROUND: Preferences.Key<Boolean> =
            booleanPreferencesKey("inputBlockWorkaround")
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(
            context = context,
            dispatcher = ioDispatcher,
            fileName = PREFERENCES_FILE_NAME,
            migrations = listOf(LegacySettingsMigration(context, ioDispatcher))
        )

    internal fun isFilterScenarioUiEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_FILTER_SCENARIO_UI_ENABLED] ?: true }

    internal suspend fun toggleFilterScenarioUi() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_FILTER_SCENARIO_UI_ENABLED] = !(preferences[KEY_IS_FILTER_SCENARIO_UI_ENABLED] ?: true)
        }

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

    internal fun isEntireScreenCaptureForced(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_FORCE_ENTIRE_SCREEN] ?: false }

    internal suspend fun toggleForceEntireScreenCapture() =
        dataStore.edit { preferences ->
            preferences[KEY_FORCE_ENTIRE_SCREEN] = !(preferences[KEY_FORCE_ENTIRE_SCREEN] ?: false)
        }

    internal fun isInputBlockWorkaroundEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_INPUT_BLOCK_WORKAROUND] ?: false }

    internal suspend fun toggleInputBlockWorkaround() {
        if (!isImpactedByInputBlock()) return
        dataStore.edit { preferences ->
            preferences[KEY_INPUT_BLOCK_WORKAROUND] = !(preferences[KEY_INPUT_BLOCK_WORKAROUND] ?: false)
        }
    }
}