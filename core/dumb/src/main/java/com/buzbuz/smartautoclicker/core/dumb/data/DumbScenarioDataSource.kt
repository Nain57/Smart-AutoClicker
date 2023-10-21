/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.dumb.data

import android.util.Log

import com.buzbuz.smartautoclicker.core.base.DatabaseListUpdater
import com.buzbuz.smartautoclicker.core.base.extensions.mapList
import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioDao
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.toDomain
import com.buzbuz.smartautoclicker.core.dumb.domain.model.toEntity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.Exception

internal class DumbScenarioDataSource(database: DumbDatabase,) {

    private val dumbScenarioDao: DumbScenarioDao = database.dumbScenarioDao()

    /** Updater for a list of actions. */
    private val dumbActionsUpdater = DatabaseListUpdater<DumbAction, DumbActionEntity>(
        itemPrimaryKeySupplier = { action -> action.id },
        entityPrimaryKeySupplier = { dumbActionEntity -> dumbActionEntity.id },
    )

    val getAllDumbScenarios: Flow<List<DumbScenario>> =
        dumbScenarioDao.getDumbScenariosWithActionsFlow()
            .mapList { it.toDomain() }

    suspend fun getDumbScenario(dbId: Long): DumbScenario? =
        dumbScenarioDao.getDumbScenariosWithAction(dbId)
            ?.toDomain()

    fun getDumbScenarioFlow(dbId: Long): Flow<DumbScenario?> =
        dumbScenarioDao.getDumbScenariosWithActionFlow(dbId)
            .map { it?.toDomain() }

    fun getAllDumbActionsExcept(scenarioDbId: Long): Flow<List<DumbAction>> =
        dumbScenarioDao.getAllDumbActionsExcept(scenarioDbId)
            .mapList { it.toDomain() }

    suspend fun addDumbScenario(scenario: DumbScenario) {
        Log.d(TAG, "Add dumb scenario $scenario")

        updateDumbScenarioActions(
            scenarioDbId = dumbScenarioDao.addDumbScenario(scenario.toEntity()),
            actions = scenario.dumbActions,
        )
    }

    suspend fun addDumbScenarioCopy(scenarioWithActions: DumbScenarioWithActions): Long? {
        Log.d(TAG, "Add dumb scenario to copy ${scenarioWithActions.scenario}")

        return try {
            val scenarioId = dumbScenarioDao.addDumbScenario(
                scenarioWithActions.scenario.copy(id = DATABASE_ID_INSERTION)
            )

            dumbScenarioDao.addDumbActions(
                scenarioWithActions.dumbActions.map { dumbAction ->
                    dumbAction.copy(
                        id = DATABASE_ID_INSERTION,
                        dumbScenarioId = scenarioId,
                    )
                }
            )

            scenarioId
        } catch (ex: Exception) {
            Log.e(TAG, "Error while inserting scenario copy", ex)
            null
        }
    }

    suspend fun updateDumbScenario(scenario: DumbScenario) {
        Log.d(TAG, "Update dumb scenario $scenario")
        val scenarioEntity = scenario.toEntity()

        dumbScenarioDao.updateDumbScenario(scenarioEntity)
        updateDumbScenarioActions(scenarioEntity.id, scenario.dumbActions)
    }

    private suspend fun updateDumbScenarioActions(scenarioDbId: Long, actions: List<DumbAction>) {
        dumbActionsUpdater.refreshUpdateValues(
            currentEntities = dumbScenarioDao.getDumbActions(scenarioDbId),
            newItems = actions,
            toEntity = { index, action ->
                action.toEntity(
                    scenarioDbId = scenarioDbId,
                    priority = index,
                )
            }
        )

        Log.d(TAG, "Dumb actions updater: $dumbActionsUpdater")

        dumbScenarioDao.apply {
            addDumbActions(dumbActionsUpdater.toBeAdded)
            updateDumbActions(dumbActionsUpdater.toBeUpdated)
            deleteDumbActions(dumbActionsUpdater.toBeRemoved)
        }
    }

    suspend fun deleteDumbScenario(scenario: DumbScenario) {
        Log.d(TAG, "Delete dumb scenario $scenario")

        dumbScenarioDao.deleteDumbScenario(scenario.id.databaseId)
    }
}

private const val TAG = "DumbScenarioDataSource"