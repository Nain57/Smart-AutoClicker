
package com.buzbuz.smartautoclicker.core.domain

import android.util.Log
import com.buzbuz.smartautoclicker.core.base.FILE_EXTENSION_PNG

import com.buzbuz.smartautoclicker.core.base.extensions.mapList
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.domain.data.ScenarioDataSource
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.toDomain
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.toDomain
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.toDomainImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.toDomainTriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.domain.model.scenario.toDomain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.max

/**
 * Repository for the database and bitmap manager.
 * Provide the access to the scenario, events, actions and conditions from the database and the conditions bitmap from
 * the application data folder.
 */
internal class Repository @Inject internal constructor(
    private val dataSource: ScenarioDataSource,
    private val bitmapRepository: BitmapRepository,
): IRepository {

    override val scenarios: Flow<List<Scenario>> =
        dataSource.scenarios.mapList { it.toDomain() }

    override val allImageEvents: Flow<List<ImageEvent>> =
        dataSource.allImageEvents.mapList { it.toDomainImageEvent() }

    override val allTriggerEvents: Flow<List<TriggerEvent>> =
        dataSource.allTriggerEvents.mapList { it.toDomainTriggerEvent() }

    override val allConditions: Flow<List<Condition>> =
        dataSource.getAllConditions().mapList { it.toDomain() }

    override val allActions: Flow<List<Action>> =
        dataSource.getAllActions().mapList { it.toDomain() }

    override val legacyConditionsCount: Flow<Int> =
        dataSource.getLegacyImageConditionsFlow()
            .map { it.size }
            .distinctUntilChanged()


    override suspend fun getScenario(scenarioId: Long): Scenario? =
        dataSource.getScenario(scenarioId)?.toDomain()

    override fun getScenarioFlow(scenarioId: Long): Flow<Scenario?> =
        dataSource.getScenarioFlow(scenarioId).map { it?.toDomain() }

    override fun getEventsFlow(scenarioId: Long): Flow<List<Event>> =
        getImageEventsFlow(scenarioId).combine(getTriggerEventsFlow(scenarioId)) { imgEvts, trigEvts ->
            buildList {
                addAll(imgEvts)
                addAll(trigEvts)
            }
        }

    override suspend fun getImageEvents(scenarioId: Long): List<ImageEvent> =
        dataSource.getImageEvents(scenarioId).map { it.toDomainImageEvent() }

    override fun getImageEventsFlow(scenarioId: Long): Flow<List<ImageEvent>> =
        dataSource.getImageEventsFlow(scenarioId).mapList { it.toDomainImageEvent() }

    override suspend fun getTriggerEvents(scenarioId: Long): List<TriggerEvent> =
        dataSource.getTriggerEvents(scenarioId).map { it.toDomainTriggerEvent() }

    override fun getTriggerEventsFlow(scenarioId: Long): Flow<List<TriggerEvent>> =
        dataSource.getTriggerEventsFlow(scenarioId).mapList { it.toDomainTriggerEvent() }

    override suspend fun addScenario(scenario: Scenario): Long =
        dataSource.addScenario(scenario)

    override suspend fun deleteScenario(scenarioId: Identifier): Unit =
        dataSource.deleteScenario(scenarioId, ::clearRemovedConditionsBitmaps)

    override suspend fun markAsUsed(scenarioId: Identifier) {
        dataSource.markAsUsed(scenarioId.databaseId)
    }

    override suspend fun addScenarioCopy(completeScenario: CompleteScenario): Long? {
        val (scenario, events) = completeScenario.toDomain(cleanIds = true)
        return dataSource.addCompleteScenario(scenario, events, ::clearRemovedConditionsBitmaps)
    }

    override suspend fun addScenarioCopy(scenarioId: Long, copyName: String): Long? {
        val (scenario, events) = dataSource.getCompleteScenario(scenarioId)
            ?.toDomain(cleanIds = true) ?: return null
        return dataSource.addCompleteScenario(scenario.copy(name = copyName), events, ::clearRemovedConditionsBitmaps)
    }

    override suspend fun updateScenario(scenario: Scenario, events: List<Event>): Boolean =
        dataSource.updateScenario(scenario, events, ::clearRemovedConditionsBitmaps)

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