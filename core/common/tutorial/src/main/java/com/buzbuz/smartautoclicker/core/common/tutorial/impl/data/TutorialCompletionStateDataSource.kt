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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class TutorialCompletionStateDataSource(
    private val dataStore: PreferencesDataStore,
) {

    @Inject constructor(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    ) : this(
        PreferencesDataStore(
            context = context,
            dispatcher = ioDispatcher,
            fileName = PREFERENCES_FILE_NAME,
        )
    )

    companion object {
        private const val PREFERENCES_FILE_NAME = "TutorialCompletionPreferences"

        internal const val PREFERENCES_KEY_SUFFIX_TUTORIAL_COMPLETED = "_tutorial_completed"

        internal fun Tutorial.getCompletionKey(): Preferences.Key<Boolean> =
            booleanPreferencesKey(info.id + PREFERENCES_KEY_SUFFIX_TUTORIAL_COMPLETED)
        internal fun Preferences.Key<*>.toTutorialId(): String? =
            if (name.endsWith(PREFERENCES_KEY_SUFFIX_TUTORIAL_COMPLETED))
                name.replace(PREFERENCES_KEY_SUFFIX_TUTORIAL_COMPLETED, "")
            else null
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
}
