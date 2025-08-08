
package com.buzbuz.smartautoclicker.feature.qstile.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

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
class QsTileConfigDataSource @Inject internal constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher
) {

    private companion object {
        const val PREFERENCES_FILE_NAME = "qsTile"

        val KEY_SCENARIO_DATABASE_ID: Preferences.Key<Long> =
            longPreferencesKey("scenarioDbId")
        val KEY_IS_SMART_SCENARIO: Preferences.Key<Boolean> =
            booleanPreferencesKey("isSmartScenario")
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(context, ioDispatcher, PREFERENCES_FILE_NAME)

    internal fun getQSTileScenarioInfo(): Flow<QSTileScenarioInfo?> =
        dataStore.data.map { preferences ->
            val scenarioDbId = preferences[KEY_SCENARIO_DATABASE_ID]
            val isSmartScenario = preferences[KEY_IS_SMART_SCENARIO]

            if (scenarioDbId == null || isSmartScenario == null) null
            else QSTileScenarioInfo(scenarioDbId, isSmartScenario)
        }

    internal suspend fun putQSTileScenarioInfo(scenarioInfo: QSTileScenarioInfo) =
        dataStore.edit { preferences ->
            preferences[KEY_SCENARIO_DATABASE_ID] = scenarioInfo.id
            preferences[KEY_IS_SMART_SCENARIO] = scenarioInfo.isSmart
        }
}
