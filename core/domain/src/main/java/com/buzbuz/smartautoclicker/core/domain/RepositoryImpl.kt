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
package com.buzbuz.smartautoclicker.core.domain

import android.util.Log
import com.buzbuz.smartautoclicker.core.base.extensions.mapList

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapManager
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.TutorialDatabase
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.domain.data.ScenarioDataSource
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.toAction
import com.buzbuz.smartautoclicker.core.domain.model.condition.toCondition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.core.domain.model.endcondition.toEndCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.toEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.domain.model.scenario.toScenario

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

// TODO: We should clean unused bitmaps, it can happens that some stays there
/**
 * Repository for the database and bitmap manager.
 * Provide the access to the scenario, events, actions and conditions from the database and the conditions bitmap from
 * the application data folder.
 *
 * @param database the database containing the list of scenario.
 * @param bitmapManager save and loads the bitmap for the conditions.
 */
internal class RepositoryImpl internal constructor(
    private val database: ClickDatabase,
    private val tutorialDatabase: TutorialDatabase,
    private val bitmapManager: BitmapManager,
): Repository {

    private val dataSource: ScenarioDataSource = ScenarioDataSource(database, bitmapManager)

    override val scenarios: Flow<List<Scenario>> =
        dataSource.scenarios.mapList { it.toScenario() }

    override suspend fun getScenario(scenarioId: Long): Scenario? =
        dataSource.getScenario(scenarioId)?.toScenario()

    override suspend fun getEvents(scenarioId: Long): List<Event> =
        dataSource.getEvents(scenarioId).map { it.toEvent() }

    override suspend fun getEndConditions(scenarioId: Long): List<EndCondition> =
        dataSource.getEndConditionsWithEvent(scenarioId).map { it.toEndCondition() }

    override fun getScenarioWithEndConditionsFlow(scenarioId: Long): Flow<Pair<Scenario, List<EndCondition>>> =
        dataSource.getScenarioWithEndConditionsFlow(scenarioId)
            .mapNotNull { scenarioWithEndConditions ->
                scenarioWithEndConditions ?: return@mapNotNull null
                scenarioWithEndConditions.scenario.toScenario() to scenarioWithEndConditions.endConditions.map { it.toEndCondition() }
            }

    override fun getEventsFlow(scenarioId: Long): Flow<List<Event>> =
        dataSource.getCompleteEventListFlow(scenarioId).mapList { it.toEvent() }

    override fun getAllEventsFlow(): Flow<List<Event>> =
        dataSource.getAllEvents().mapList { it.toEvent() }

    override fun getAllActions(): Flow<List<Action>> =
        dataSource.getAllActions().mapList { it.toAction() }

    override fun getAllConditions(): Flow<List<Condition>> =
        dataSource.getAllConditions().mapList { it.toCondition() }

    override suspend fun addScenario(scenario: Scenario): Long =
        dataSource.addScenario(scenario)

    override suspend fun deleteScenario(scenarioId: Identifier): Unit =
        dataSource.deleteScenario(scenarioId)

    override suspend fun addScenarioCopy(completeScenario: CompleteScenario): Long? =
        dataSource.addScenarioCopy(completeScenario)

    override suspend fun updateScenario(scenario: Scenario, events: List<Event>, endConditions: List<EndCondition>): Boolean =
        dataSource.updateScenario(scenario, events, endConditions)
    override suspend fun getBitmap(path: String, width: Int, height: Int) =
        bitmapManager.loadBitmap(path, width, height)

    override fun cleanCache(): Unit =
        bitmapManager.releaseCache()

    override fun startTutorialMode() {
        Log.d(TAG, "Start tutorial mode, use tutorial database")
        dataSource.currentDatabase.value = tutorialDatabase
    }

    override fun stopTutorialMode() {
        Log.d(TAG, "Stop tutorial mode, use regular database")
        dataSource.currentDatabase.value = database
    }

    override fun isTutorialModeEnabled(): Boolean =
        dataSource.currentDatabase.value == tutorialDatabase

}

/** Tag for logs. */
private const val TAG = "RepositoryImpl"