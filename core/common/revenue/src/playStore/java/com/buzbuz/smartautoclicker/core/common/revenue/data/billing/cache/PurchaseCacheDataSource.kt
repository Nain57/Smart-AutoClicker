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
package com.buzbuz.smartautoclicker.core.common.revenue.data.billing.cache

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class PurchaseCacheDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : PurchaseCache {

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(context, ioDispatcher, PREFERENCES_FILE_NAME)

    override suspend fun get(): CachedPurchase? {
        val preferences = dataStore.data.first()
        return CachedPurchase(
            originalJson = preferences[KEY_ORIGINAL_JSON] ?: return null,
            signature = preferences[KEY_SIGNATURE] ?: return null,
            missingSinceMs = preferences[KEY_MISSING_SINCE],
        )
    }

    override suspend fun save(originalJson: String, signature: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ORIGINAL_JSON] = originalJson
            preferences[KEY_SIGNATURE] = signature
            preferences.remove(KEY_MISSING_SINCE)
        }
    }

    override suspend fun setMissingSince(timestampMs: Long) {
        dataStore.edit { preferences -> preferences[KEY_MISSING_SINCE] = timestampMs }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }
}

private const val PREFERENCES_FILE_NAME = "billing_cache"
private val KEY_ORIGINAL_JSON: Preferences.Key<String> = stringPreferencesKey("purchaseOriginalJson")
private val KEY_SIGNATURE: Preferences.Key<String> = stringPreferencesKey("purchaseSignature")
private val KEY_MISSING_SINCE: Preferences.Key<Long> = longPreferencesKey("purchaseMissingSince")
