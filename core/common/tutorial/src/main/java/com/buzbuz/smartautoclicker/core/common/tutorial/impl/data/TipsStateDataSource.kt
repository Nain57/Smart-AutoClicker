/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.tutorial.impl.data

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tip

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class TipsStateDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    private companion object {
        const val PREFERENCES_FILE_NAME = "TutorialPreferences"

        const val PREFERENCES_KEY_SUFFIX_DONT_SHOW_AGAIN = "_dont_show_again"
        fun Tip.getDontShowAgainKey(): Preferences.Key<Boolean> =
            booleanPreferencesKey(name + PREFERENCES_KEY_SUFFIX_DONT_SHOW_AGAIN)

        const val PREFERENCES_KEY_SUFFIX_TUTORIAL_COMPLETED = "_tutorial_completed"
        fun Tutorial.getCompletionKey(): Preferences.Key<Boolean> =
            booleanPreferencesKey(info.id + PREFERENCES_KEY_SUFFIX_TUTORIAL_COMPLETED)
        fun Preferences.Key<*>.toTutorialId(): String? =
            if (name.endsWith(PREFERENCES_KEY_SUFFIX_TUTORIAL_COMPLETED))
                name.replace(PREFERENCES_KEY_SUFFIX_TUTORIAL_COMPLETED, "")
            else null
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(
            context = context,
            dispatcher = ioDispatcher,
            fileName = PREFERENCES_FILE_NAME,
            migrations = listOf(LegacyPreferencesMigration(context)),
        )

    fun getTipsDontShowAgainValue(tips: Tip): Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[tips.getDontShowAgainKey()] ?: false
        }

    suspend fun setTipsDontShowAgain(tips: Tip) {
        dataStore.edit { preferences ->
            preferences[tips.getDontShowAgainKey()] = true
        }
    }

    fun getTutorialsCompletionState(): Flow<Map<String, Boolean>> =
        dataStore.data.map { preferences ->
            buildMap {
                preferences.asMap().keys.forEach { preferenceKey ->
                    preferenceKey.toTutorialId()?.let { tutorialId ->
                        put(tutorialId, (preferences[preferenceKey] == true))
                    }
                }
            }
        }

    suspend fun setTutorialCompleted(tutorial: Tutorial) {
        dataStore.edit { preferences ->
            preferences[tutorial.getCompletionKey()] = true
        }
    }

    /** Migration from pre 4.0.0 preferences. */
    private class LegacyPreferencesMigration(context: Context) : DataMigration<Preferences> {

        private companion object {
            const val LEGACY_PREFERENCES_FILE_NAME = "TutorialPreferences"
            const val LEGACY_KEY_STOP_VOL_DOWN = "Tutorial_Stop_Volume_Down_dont_show_again"
        }

        private val legacyPrefs by lazy {
            context.getSharedPreferences(LEGACY_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        }

        override suspend fun shouldMigrate(currentData: Preferences): Boolean =
            Tip.STOP_WITH_VOLUME_DOWN.getDontShowAgainKey() !in currentData
                    && legacyPrefs.contains(LEGACY_KEY_STOP_VOL_DOWN)

        override suspend fun migrate(currentData: Preferences): Preferences =
            currentData.toMutablePreferences().apply {
                this[Tip.STOP_WITH_VOLUME_DOWN.getDontShowAgainKey()] =
                    legacyPrefs.getBoolean(LEGACY_KEY_STOP_VOL_DOWN, true)
            }

        override suspend fun cleanUp() {
            legacyPrefs.edit { remove(LEGACY_KEY_STOP_VOL_DOWN) }
        }
    }
}

