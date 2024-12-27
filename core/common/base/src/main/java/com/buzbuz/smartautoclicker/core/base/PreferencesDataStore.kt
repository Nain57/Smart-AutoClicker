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
package com.buzbuz.smartautoclicker.core.base

import android.content.Context
import android.util.Log

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory.create
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

class PreferencesDataStore(
    context: Context,
    dispatcher: CoroutineDispatcher,
    fileName: String,
    migrations: List<DataMigration<Preferences>> = emptyList(),
    onFileCorrupted: (ex: CorruptionException) -> Preferences = { emptyPreferences() },
) {

    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private val dataStore: DataStore<Preferences> = create(
        scope = coroutineScope,
        produceFile = { context.preferencesDataStoreFile(fileName) },
        migrations = migrations,
        corruptionHandler = ReplaceFileCorruptionHandler { exception ->
            Log.e(TAG, "Preferences file $fileName is corrupted", exception)
            onFileCorrupted(exception)
        }
    )

    val data: Flow<Preferences> =
        dataStore.data

    suspend fun edit(transform: suspend (MutablePreferences) -> Unit): Preferences =
        dataStore.edit(transform)
}

private const val TAG = "PreferencesDataSource"