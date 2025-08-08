
package com.buzbuz.smartautoclicker.core.dumb.domain

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

import kotlinx.coroutines.flow.Flow

interface IDumbRepository {

    val dumbScenarios: Flow<List<DumbScenario>>

    suspend fun getDumbScenario(dbId: Long): DumbScenario?

    fun getDumbScenarioFlow(dbId: Long): Flow<DumbScenario?>

    fun getAllDumbActionsFlowExcept(scenarioDbId: Long): Flow<List<DumbAction>>

    suspend fun addDumbScenario(scenario: DumbScenario)

    suspend fun addDumbScenarioCopy(scenario: DumbScenarioWithActions): Long?

    suspend fun addDumbScenarioCopy(scenarioId: Long, copyName: String): Long?

    suspend fun updateDumbScenario(scenario: DumbScenario)

    suspend fun deleteDumbScenario(scenario: DumbScenario)

    suspend fun markAsUsed(scenarioId: Identifier)
}