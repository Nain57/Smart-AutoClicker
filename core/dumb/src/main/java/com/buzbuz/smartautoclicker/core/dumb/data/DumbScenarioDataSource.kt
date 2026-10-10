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
package com.buzbuz.smartautoclicker.core.dumb.data

import android.util.Log

import com.buzbuz.smartautoclicker.core.base.DatabaseListUpdater
import com.buzbuz.smartautoclicker.core.base.extensions.mapList
import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbGestureFingerWithMotions
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioDao
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioStatsEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.toDomain
import com.buzbuz.smartautoclicker.core.dumb.domain.model.toEntity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DumbScenarioDataSource @Inject constructor(
    database: DumbDatabase,
) {

    private val dumbScenarioDao: DumbScenarioDao = database.dumbScenarioDao()

    /** Updater for a list of actions. */
    private val dumbActionsUpdater = DatabaseListUpdater<DumbAction, DumbActionEntity>()

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

    suspend fun addDumbScenarioCopy(scenarioDbId: Long, copyName: String): Long? =
        dumbScenarioDao.getDumbScenariosWithAction(scenarioDbId)?.let { scenarioWithActions ->
            addDumbScenarioCopy(scenarioWithActions, copyName)
        }

    suspend fun addDumbScenarioCopy(scenarioWithActions: DumbScenarioWithActions, copyName: String? = null): Long? {
        Log.d(TAG, "Add dumb scenario to copy ${scenarioWithActions.scenario}")

        return try {
            val scenarioId = dumbScenarioDao.addDumbScenario(
                scenarioWithActions.scenario.copy(
                    id = DATABASE_ID_INSERTION,
                    name = copyName ?: scenarioWithActions.scenario.name,
                )
            )

            val actionIds = dumbScenarioDao.addDumbActions(
                scenarioWithActions.dumbActions.map { dumbAction ->
                    dumbAction.action.copy(
                        id = DATABASE_ID_INSERTION,
                        dumbScenarioId = scenarioId,
                    )
                }
            )

            scenarioWithActions.dumbActions.forEachIndexed { index, dumbAction ->
                addGestureFingersCopy(actionIds[index], dumbAction.gestureFingers)
            }

            scenarioId
        } catch (ex: Exception) {
            Log.e(TAG, "Error while inserting scenario copy", ex)
            null
        }
    }

    suspend fun markAsUsed(scenarioDbId: Long) {
        val previousStats = dumbScenarioDao.getScenarioStats(scenarioDbId)
        if (previousStats != null) {
            dumbScenarioDao.updateScenarioStats(
                previousStats.copy(
                    lastStartTimestampMs = System.currentTimeMillis(),
                    startCount = previousStats.startCount + 1,
                )
            )
        } else {
            dumbScenarioDao.addScenarioStats(
                DumbScenarioStatsEntity(
                    id = DATABASE_ID_INSERTION,
                    scenarioId = scenarioDbId,
                    lastStartTimestampMs = System.currentTimeMillis(),
                    startCount = 1,
                )
            )
        }
    }

    suspend fun updateDumbScenario(scenario: DumbScenario) {
        Log.d(TAG, "Update dumb scenario $scenario")
        val scenarioEntity = scenario.toEntity()

        dumbScenarioDao.updateDumbScenario(scenarioEntity)
        updateDumbScenarioActions(scenarioEntity.id, scenario.dumbActions)
    }

    private suspend fun updateDumbScenarioActions(scenarioDbId: Long, actions: List<DumbAction>) {
        val updater = DatabaseListUpdater<DumbAction, DumbActionEntity>()
        updater.refreshUpdateValues(
            currentEntities = dumbScenarioDao.getDumbActions(scenarioDbId),
            newItems = actions,
            mappingClosure = { action -> action.toEntity(scenarioDbId = scenarioDbId) }
        )

        Log.d(TAG, "Dumb actions updater: $dumbActionsUpdater")

        updater.executeUpdate(
            addList = dumbScenarioDao::addDumbActions,
            updateList = dumbScenarioDao::updateDumbActions,
            removeList = dumbScenarioDao::deleteDumbActions,
            onSuccess = { addedMapping, added, updated, _ ->
                added.forEach { action ->
                    val actionDbId = action.getDomainId()?.let(addedMapping::get) ?: return@forEach
                    updateDumbActionChildren(actionDbId, action)
                }
                updated.forEach { action -> updateDumbActionChildren(action.id.databaseId, action) }
            }
        )
    }

    private suspend fun updateDumbActionChildren(actionDbId: Long, action: DumbAction) {
        if (action !is DumbAction.DumbGesture) return

        dumbScenarioDao.deleteGestureFingersByActionId(actionDbId)
        action.fingers.forEachIndexed { fingerIndex, finger ->
            val fingerDbId = dumbScenarioDao.addGestureFinger(finger.toEntity(actionDbId, fingerIndex))
            dumbScenarioDao.addGestureFingerMotions(
                finger.motions.mapIndexed { motionIndex, motion -> motion.toEntity(fingerDbId, motionIndex) }
            )
        }
    }

    private suspend fun addGestureFingersCopy(actionDbId: Long, fingers: List<DumbGestureFingerWithMotions>) {
        fingers.forEach { fingerWithMotions ->
            val fingerDbId = dumbScenarioDao.addGestureFinger(
                fingerWithMotions.finger.copy(id = DATABASE_ID_INSERTION, dumbActionId = actionDbId)
            )
            dumbScenarioDao.addGestureFingerMotions(
                fingerWithMotions.motions.map { motion ->
                    motion.copy(id = DATABASE_ID_INSERTION, fingerId = fingerDbId)
                }
            )
        }
    }

    suspend fun deleteDumbScenario(scenario: DumbScenario) {
        Log.d(TAG, "Delete dumb scenario $scenario")

        dumbScenarioDao.deleteDumbScenario(scenario.id.databaseId)
    }
}

private const val TAG = "DumbScenarioDataSource"