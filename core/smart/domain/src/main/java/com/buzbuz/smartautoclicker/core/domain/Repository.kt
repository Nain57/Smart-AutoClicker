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
package com.buzbuz.smartautoclicker.core.domain

import android.util.Log
import com.buzbuz.smartautoclicker.core.base.FILE_EXTENSION_PNG

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.extensions.mapList
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.domain.data.ScenarioDataSource
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.mapper.toDomain
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.toDomain
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.counter.toDomain
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.toDomainScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.toDomainTriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.domain.model.scenario.toDomain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

/**
 * Repository for the database and bitmap manager.
 * Provide the access to the scenario, events, actions and conditions from the database and the conditions bitmap from
 * the application data folder.
 */
internal class Repository @Inject internal constructor(
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val dataSource: ScenarioDataSource,
    private val bitmapRepository: BitmapRepository,
): IRepository {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    override val scenarios: Flow<List<Scenario>>
        get() = dataSource.scenarios().mapList { it.toDomain() }

    override val allScreenEvents: Flow<List<ScreenEvent>>
        get() = dataSource.allImageEvents().mapList { it.toDomainScreenEvent() }

    override val allTriggerEvents: Flow<List<TriggerEvent>>
        get() = dataSource.allTriggerEvents().mapList { it.toDomainTriggerEvent() }

    override val allConditions: Flow<List<Condition>>
        get() = dataSource.getAllConditions().mapList { it.toDomain() }

    override val allActions: Flow<List<Action>>
        get() = dataSource.getAllActions().mapList { it.toDomain() }

    override val screenEventsCount: Flow<Int>
        get() = dataSource.screenEventsCount()

    override val triggerEventsCount: Flow<Int>
        get() = dataSource.triggerEventsCount()

    override val screenConditionsCount: Flow<Int>
        get() = dataSource.screenConditionsCount()

    override val triggerConditionsCount: Flow<Int>
        get() = dataSource.triggerConditionsCount()

    override val actionsCount: Flow<Int>
        get() = dataSource.actionsCount()

    override val legacyConditionsCount: Flow<Int>
        get() = dataSource.getLegacyImageConditionsFlow()
            .map { it.size }
            .distinctUntilChanged()


    override suspend fun getScenario(scenarioId: Long): Scenario? =
        dataSource.getScenario(scenarioId)?.toDomain()

    override fun getScenarioFlow(scenarioId: Long): Flow<Scenario?> =
        dataSource.getScenarioFlow(scenarioId).map { it?.toDomain() }

    override fun getEventsFlow(scenarioId: Long): Flow<List<Event>> =
        getScreenEventsFlow(scenarioId).combine(getTriggerEventsFlow(scenarioId)) { imgEvts, trigEvts ->
            buildList {
                addAll(imgEvts)
                addAll(trigEvts)
            }
        }

    override suspend fun getScreenEvents(scenarioId: Long): List<ScreenEvent> =
        dataSource.getScreenEvents(scenarioId).map { it.toDomainScreenEvent() }

    override fun getScreenEventsFlow(scenarioId: Long): Flow<List<ScreenEvent>> =
        dataSource.getScreenEventsFlow(scenarioId).mapList { it.toDomainScreenEvent() }

    override suspend fun getTriggerEvents(scenarioId: Long): List<TriggerEvent> =
        dataSource.getTriggerEvents(scenarioId).map { it.toDomainTriggerEvent() }

    override fun getTriggerEventsFlow(scenarioId: Long): Flow<List<TriggerEvent>> =
        dataSource.getTriggerEventsFlow(scenarioId).mapList { it.toDomainTriggerEvent() }

    override fun getCountersFlow(scenarioId: Long): Flow<List<Counter>> =
        dataSource.getCountersFlow(scenarioId).mapList { it.toDomain() }

    override suspend fun getCounters(scenarioId: Long): List<Counter> =
        dataSource.getCounters(scenarioId).map { it.toDomain() }

    override suspend fun getConditionName(conditionId: Identifier): String? =
        dataSource.getConditionName(conditionId.databaseId)

    override suspend fun getEventName(eventId: Identifier): String? =
        dataSource.getEventName(eventId.databaseId)

    override suspend fun addScenario(scenario: Scenario): Long =
        dataSource.addScenario(scenario)

    override suspend fun deleteScenario(scenarioId: Identifier): Unit =
        dataSource.deleteScenario(scenarioId, ::clearRemovedConditionsBitmaps)

    override suspend fun markAsUsed(scenarioId: Identifier) {
        dataSource.markAsUsed(scenarioId.databaseId)
    }

    override suspend fun addScenarioCopy(completeScenario: CompleteScenario): Long? {
        val (scenario, events, counters) = completeScenario.toDomain(cleanIds = true)
        return dataSource.addCompleteScenario(scenario, events, counters, ::clearRemovedConditionsBitmaps)
    }

    override fun addScenarioCopy(scenarioId: Long, copyName: String, onCopyCompleted: (Boolean) -> Unit) {
        coroutineScopeIo.launch {
            val (scenario, events, counters) = dataSource.getCompleteScenario(scenarioId)?.toDomain(cleanIds = true) ?: run {
                onCopyCompleted(false)
                return@launch
            }

            dataSource.addCompleteScenario(scenario.copy(name = copyName), events, counters, ::clearRemovedConditionsBitmaps)
            onCopyCompleted(true)
        }
    }

    override suspend fun updateScenario(scenario: Scenario, events: List<Event>, counters: List<Counter>): Boolean =
        dataSource.updateScenario(scenario, events, counters, ::clearRemovedConditionsBitmaps)

    override suspend fun migrateLegacyImageConditions(): Boolean {
        val legacyConditions = dataSource.getLegacyImageConditions()
        Log.i(TAG, "Migrating ${legacyConditions.size} image conditions...")

        var success = true
        val removedPaths = mutableMapOf<String, String>()

        legacyConditions.forEach { conditionEntity ->
            val oldPath = conditionEntity.path ?: return@forEach
            if (oldPath.endsWith(FILE_EXTENSION_PNG)) return@forEach

            val newPath =
                if (removedPaths.containsKey(oldPath)) removedPaths[oldPath]
                else bitmapRepository.migrateImageConditionBitmap(
                    path = oldPath,
                    width = max(0, (conditionEntity.areaRight ?: 0) - (conditionEntity.areaLeft ?: 0)),
                    height = max(0, (conditionEntity.areaBottom ?: 0) - (conditionEntity.areaTop ?: 0)),
                )

            if (newPath == null) {
                success = false
                Log.w(TAG, "Can't migrate legacy condition ${conditionEntity.id}:${conditionEntity.name}")
                return@forEach
            }

            removedPaths[oldPath] = newPath
            dataSource.updateLegacyImageCondition(
                condition = conditionEntity,
                newPath = newPath,
            )
        }

        return success
    }

    /**
     * Remove bitmaps from the application data folder.
     * @param removedPath the list of path for the bitmaps to be removed.
     */
    private suspend fun clearRemovedConditionsBitmaps(removedPath: List<String>) {
        Log.d(TAG, "Clearing removed conditions bitmaps: $removedPath")
        val deletedPaths = removedPath.filter { path ->
            path.isNotEmpty() && dataSource.getImageConditionPathUsageCount(path) == 0
        }

        Log.d(TAG, "Removed conditions count: ${removedPath.size}; Unused bitmaps after removal: ${deletedPaths.size}")
        if (deletedPaths.isNotEmpty()) bitmapRepository.deleteImageConditionBitmaps(deletedPaths)
    }
}

/** Tag for logs. */
private const val TAG = "RepositoryImpl"
