
package com.buzbuz.smartautoclicker.core.dumb.domain

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.data.DumbScenarioDataSource
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DumbRepository @Inject constructor(
    private val dumbScenarioDataSource: DumbScenarioDataSource,
) : IDumbRepository {

    override val dumbScenarios: Flow<List<DumbScenario>> =
        dumbScenarioDataSource.getAllDumbScenarios

    override suspend fun getDumbScenario(dbId: Long): DumbScenario? =
        dumbScenarioDataSource.getDumbScenario(dbId)

    override fun getDumbScenarioFlow(dbId: Long): Flow<DumbScenario?> =
        dumbScenarioDataSource.getDumbScenarioFlow(dbId)

    override fun getAllDumbActionsFlowExcept(scenarioDbId: Long): Flow<List<DumbAction>> =
        dumbScenarioDataSource.getAllDumbActionsExcept(scenarioDbId)

    override suspend fun addDumbScenario(scenario: DumbScenario) {
        dumbScenarioDataSource.addDumbScenario(scenario)
    }

    override suspend fun addDumbScenarioCopy(scenario: DumbScenarioWithActions): Long? =
        dumbScenarioDataSource.addDumbScenarioCopy(scenario)

    override suspend fun addDumbScenarioCopy(scenarioId: Long, copyName: String): Long? =
        dumbScenarioDataSource.addDumbScenarioCopy(scenarioId, copyName)

    override suspend fun updateDumbScenario(scenario: DumbScenario) {
        dumbScenarioDataSource.updateDumbScenario(scenario)
    }

    override suspend fun deleteDumbScenario(scenario: DumbScenario) {
        dumbScenarioDataSource.deleteDumbScenario(scenario)
    }

    override suspend fun markAsUsed(scenarioId: Identifier) {
        dumbScenarioDataSource.markAsUsed(scenarioId.databaseId)
    }
}