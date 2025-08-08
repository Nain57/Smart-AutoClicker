
package com.buzbuz.smartautoclicker.scenarios.list.sort

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

import com.buzbuz.smartautoclicker.core.base.PreferencesDataStore
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.getEnum
import com.buzbuz.smartautoclicker.core.base.setEnum

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ScenarioSortConfigRepository @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    private companion object {
        const val PREFERENCES_FILE_NAME = "scenario_sort"

        val KEY_SORT_TYPE: Preferences.Key<String> =
            stringPreferencesKey("sortType")
        val KEY_SORT_INVERTED: Preferences.Key<Boolean> =
            booleanPreferencesKey("sortOrderInverted")
        val KEY_FILTER_SHOW_SMART: Preferences.Key<Boolean> =
            booleanPreferencesKey("filterShowSmart")
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(
            context = context,
            dispatcher = ioDispatcher,
            fileName = PREFERENCES_FILE_NAME,
            migrations = emptyList()
        )

    internal fun getSortConfig(): Flow<ScenarioSortConfig> =
        dataStore.data.map { preferences ->
            ScenarioSortConfig(
                type = preferences.getEnum<ScenarioSortType>(KEY_SORT_TYPE) ?: ScenarioSortType.NAME,
                inverted = preferences[KEY_SORT_INVERTED] ?: false,
                showSmartScenario = preferences[KEY_FILTER_SHOW_SMART] ?: true,
            )
        }

    internal suspend fun setSortType(type: ScenarioSortType) =
        dataStore.edit { preferences ->
            preferences.setEnum(KEY_SORT_TYPE, type)
        }

    internal suspend fun setSortOrder(invertSortOrder: Boolean) =
        dataStore.edit { preferences ->
            preferences[KEY_SORT_INVERTED] = invertSortOrder
        }

    internal suspend fun setShowSmart(show: Boolean) =
        dataStore.edit { preferences ->
            preferences[KEY_FILTER_SHOW_SMART] = show
        }
}