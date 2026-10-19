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
package com.buzbuz.smartautoclicker.core.smart.debugging.data

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataMigration
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
internal class DebugConfigurationLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    private companion object {
        const val PREFERENCES_FILE_NAME = "debug_configuration"

        val KEY_DEBUG_VIEW_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("isDebugViewEnabled")
        val KEY_DEBUG_REPORT_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("isDebugReportEnabled")
        val KEY_DEBUG_VIEW_SHOW_EVENT_STATS: Preferences.Key<Boolean> =
            booleanPreferencesKey("debugViewShowEventStats")
        val KEY_DEBUG_VIEW_SHOW_ACTIONS: Preferences.Key<Boolean> =
            booleanPreferencesKey("debugViewShowActions")
        val KEY_DEBUG_VIEW_SHOW_HELD_FINGER: Preferences.Key<Boolean> =
            booleanPreferencesKey("debugViewShowHeldFinger")
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(
            context = context,
            dispatcher = ioDispatcher,
            fileName = PREFERENCES_FILE_NAME,
            migrations = listOf(LegacyPreferencesMigration(context)),
        )

    fun isDebugViewEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_DEBUG_VIEW_ENABLED] ?: false }

    fun isDebugReportEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_DEBUG_REPORT_ENABLED] ?: false }

    suspend fun setDebuggingConfig(debugView: Boolean, debugReport: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DEBUG_VIEW_ENABLED] = debugView
            preferences[KEY_DEBUG_REPORT_ENABLED] = debugReport
        }
    }

    fun isDebugViewEventStatsShown(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_DEBUG_VIEW_SHOW_EVENT_STATS] ?: true }

    suspend fun setDebugViewEventStatsShown(isShown: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_DEBUG_VIEW_SHOW_EVENT_STATS] = isShown }
    }

    fun isDebugViewActionsShown(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_DEBUG_VIEW_SHOW_ACTIONS] ?: true }

    suspend fun setDebugViewActionsShown(isShown: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_DEBUG_VIEW_SHOW_ACTIONS] = isShown }
    }

    fun isDebugViewHeldFingerShown(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_DEBUG_VIEW_SHOW_HELD_FINGER] ?: true }

    suspend fun setDebugViewHeldFingerShown(isShown: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_DEBUG_VIEW_SHOW_HELD_FINGER] = isShown }
    }

    /** Migration from the legacy SharedPreferences. */
    private class LegacyPreferencesMigration(context: Context) : DataMigration<Preferences> {

        private companion object {
            const val LEGACY_PREFERENCES_FILE_NAME = "DebugConfigPreferences"
            const val LEGACY_KEY_DEBUG_VIEW_ENABLED = "Debug_View_Enabled"
            const val LEGACY_KEY_DEBUG_REPORT_ENABLED = "Debug_Report_Enabled"
        }

        private val legacyPrefs by lazy {
            context.getSharedPreferences(LEGACY_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        }

        override suspend fun shouldMigrate(currentData: Preferences): Boolean =
            legacyPrefs.contains(LEGACY_KEY_DEBUG_VIEW_ENABLED) || legacyPrefs.contains(LEGACY_KEY_DEBUG_REPORT_ENABLED)

        override suspend fun migrate(currentData: Preferences): Preferences =
            currentData.toMutablePreferences().apply {
                if (KEY_DEBUG_VIEW_ENABLED !in currentData && legacyPrefs.contains(LEGACY_KEY_DEBUG_VIEW_ENABLED))
                    this[KEY_DEBUG_VIEW_ENABLED] = legacyPrefs.getBoolean(LEGACY_KEY_DEBUG_VIEW_ENABLED, false)
                if (KEY_DEBUG_REPORT_ENABLED !in currentData && legacyPrefs.contains(LEGACY_KEY_DEBUG_REPORT_ENABLED))
                    this[KEY_DEBUG_REPORT_ENABLED] = legacyPrefs.getBoolean(LEGACY_KEY_DEBUG_REPORT_ENABLED, false)
            }

        override suspend fun cleanUp() {
            legacyPrefs.edit {
                remove(LEGACY_KEY_DEBUG_VIEW_ENABLED)
                remove(LEGACY_KEY_DEBUG_REPORT_ENABLED)
            }
        }
    }
}
