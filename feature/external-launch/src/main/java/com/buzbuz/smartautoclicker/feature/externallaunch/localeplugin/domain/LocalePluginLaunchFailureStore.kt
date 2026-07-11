/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one pending explanation for a direct automation launch which did not open its Activity.
 *
 * A lock-screen launch deliberately uses a notification and must not create this state. The value is
 * tied to its launch attempt so that a late Activity start, or a tap on its fallback notification,
 * can remove only its own warning.
 */
@Singleton
class LocalePluginLaunchFailureStore @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    private val dataStore = PreferencesDataStore(
        context = context,
        dispatcher = ioDispatcher,
        fileName = PREFERENCES_FILE_NAME,
    )

    internal suspend fun markDirectLaunchFailed(requestId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_PENDING_FAILURE_REQUEST_ID] = requestId
        }
    }

    internal suspend fun clearDirectLaunchFailure(requestId: String) {
        dataStore.edit { preferences ->
            if (preferences[KEY_PENDING_FAILURE_REQUEST_ID] == requestId) {
                preferences.remove(KEY_PENDING_FAILURE_REQUEST_ID)
            }
        }
    }

    internal suspend fun markFallbackPending(requestId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_PENDING_FALLBACK_REQUEST_ID] = requestId
        }
    }

    internal suspend fun markLaunchPending(requestId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_PENDING_LAUNCH_REQUEST_ID] = requestId
            preferences[KEY_PENDING_LAUNCH_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    internal suspend fun clearLaunchPending() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_PENDING_LAUNCH_REQUEST_ID)
            preferences.remove(KEY_PENDING_LAUNCH_TIMESTAMP)
        }
    }

    internal suspend fun consumeLaunchPending(requestId: String): Boolean {
        var matches = false
        dataStore.edit { preferences ->
            val requestMatches = preferences[KEY_PENDING_LAUNCH_REQUEST_ID] == requestId
            val timestamp = preferences[KEY_PENDING_LAUNCH_TIMESTAMP]
            val isRecent = timestamp != null &&
                System.currentTimeMillis() - timestamp <= PENDING_LAUNCH_MAX_AGE_MS
            if (requestMatches && isRecent) matches = true
            if (requestMatches) {
                preferences.remove(KEY_PENDING_LAUNCH_REQUEST_ID)
                preferences.remove(KEY_PENDING_LAUNCH_TIMESTAMP)
            }
        }
        return matches
    }

    internal suspend fun clearFallbackPending() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_PENDING_FALLBACK_REQUEST_ID)
        }
    }

    internal suspend fun consumeFallbackPending(requestId: String): Boolean {
        var matches = false
        dataStore.edit { preferences ->
            if (preferences[KEY_PENDING_FALLBACK_REQUEST_ID] == requestId) {
                preferences.remove(KEY_PENDING_FALLBACK_REQUEST_ID)
                matches = true
            }
        }
        return matches
    }

    /**
     * Returns whether Klick'r should explain a failed direct launch when its normal UI is next opened.
     * Reading and clearing are one transaction so the message is shown at most once per failed attempt.
     */
    suspend fun consumePendingDirectLaunchFailure(): Boolean {
        var hasPendingFailure = false
        dataStore.edit { preferences ->
            hasPendingFailure = preferences.remove(KEY_PENDING_FAILURE_REQUEST_ID) != null
        }
        return hasPendingFailure
    }
}

private const val PREFERENCES_FILE_NAME = "locale_plugin_launch_failures"

private val KEY_PENDING_FAILURE_REQUEST_ID: Preferences.Key<String> =
    stringPreferencesKey("pendingDirectLaunchFailureRequestId")

private val KEY_PENDING_FALLBACK_REQUEST_ID: Preferences.Key<String> =
    stringPreferencesKey("pendingFallbackRequestId")

private val KEY_PENDING_LAUNCH_REQUEST_ID: Preferences.Key<String> =
    stringPreferencesKey("pendingLaunchRequestId")

private val KEY_PENDING_LAUNCH_TIMESTAMP: Preferences.Key<Long> =
    longPreferencesKey("pendingLaunchTimestamp")

private const val PENDING_LAUNCH_MAX_AGE_MS = 10_000L
