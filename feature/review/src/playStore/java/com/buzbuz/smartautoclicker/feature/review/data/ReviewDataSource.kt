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
package com.buzbuz.smartautoclicker.feature.review.data

import android.content.Context

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.extensions.trim
import com.buzbuz.smartautoclicker.feature.review.engine.session.UserSession
import com.buzbuz.smartautoclicker.feature.review.engine.session.toPreferences
import com.buzbuz.smartautoclicker.feature.review.engine.session.toUserSessions

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class ReviewDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    internal companion object {
        private const val MAX_SESSION_CACHE = 20
        private const val PREFERENCES_FILE_NAME = "review"

        val KEY_LAST_REVIEW_REQUEST_TIMESTAMP: Preferences.Key<Long> =
            longPreferencesKey("lastReviewRequestTimestamp")
        val KEY_LAST_USER_SESSION: Preferences.Key<Set<String>> =
            stringSetPreferencesKey("lastUserSessions")
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(context, ioDispatcher, PREFERENCES_FILE_NAME)


    internal fun lastReviewRequestTimestamp(): Flow<Long?> =
        dataStore.data.map { preferences -> preferences[KEY_LAST_REVIEW_REQUEST_TIMESTAMP] }

    internal suspend fun setLastReviewRequestTimestamp(ts: Long) =
        dataStore.edit { preferences -> preferences[KEY_LAST_REVIEW_REQUEST_TIMESTAMP] = ts }


    internal fun lastUserSessions(): Flow<List<UserSession>> =
        dataStore.data.map { preferences ->
            preferences[KEY_LAST_USER_SESSION]?.toUserSessions() ?: emptyList()
        }

    internal suspend fun addUserSession(userSession: UserSession) {
        dataStore.edit { preferences ->
            val previousSessions = preferences[KEY_LAST_USER_SESSION]
                ?.toUserSessions()
                ?.sortedByDescending { session -> session.timestamp }

            val newSessions = previousSessions
                ?.trim(MAX_SESSION_CACHE - 1)
                ?.toMutableList()
                ?: mutableListOf()

            newSessions.add(userSession)
            preferences[KEY_LAST_USER_SESSION] = newSessions.toPreferences()
        }
    }
}