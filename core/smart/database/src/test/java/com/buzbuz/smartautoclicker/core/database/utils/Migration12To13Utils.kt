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
package com.buzbuz.smartautoclicker.core.database.utils

import android.graphics.Rect
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType


fun getV13Events() =
    "SELECT * FROM event_table"
fun getV13TriggerEvents() =
    "SELECT * FROM event_table WHERE `type` = \"TRIGGER_EVENT\""
fun getV13Conditions() =
    "SELECT * FROM condition_table"
fun getV13CounterReachedConditions(eventId: Long, executions: Int) =
    "SELECT * FROM condition_table  WHERE `type` = \"ON_COUNTER_REACHED\" AND `eventId` = $eventId AND `counter_value` = $executions"
fun getV13ToggleEventsActions() =
    "SELECT * FROM action_table WHERE `type` = \"TOGGLE_EVENT\""
fun getV13ChangeCounterActions(eventId: Long) =
    "SELECT * FROM action_table WHERE `type` = \"CHANGE_COUNTER\" AND `eventId` = $eventId"
fun getV13ToggleEventsActions(eventId: Long) =
    "SELECT * FROM action_table WHERE `type` = \"TOGGLE_EVENT\" AND `eventId` = $eventId"
fun getV13EventToggle(actionId: Long) =
    "SELECT * FROM event_toggle_table WHERE `action_id` = $actionId"


data class V12Scenario(
    val id: Long,
    val name: String = "titi",
    val detectionQuality: Int = 3000,
    val endConditionOperator: Int = 1,
    val randomize: Boolean = false,
)

internal fun SupportSQLiteDatabase.insertV12Scenario(event: V12Scenario) {
    execSQL(
        """
            INSERT INTO scenario_table (id, name, detection_quality, end_condition_operator, randomize) 
            VALUES (${event.id}, "${event.name}", ${event.detectionQuality}, ${event.endConditionOperator}, ${event.randomize.toSqlite()})
        """.trimIndent()
    )
}

data class V12EndCondition(
    val id: Long,
    val scenarioId: Long,
    val eventId: Long,
    val executions: Int = 2,
)

internal fun SupportSQLiteDatabase.insertV12EndCondition(endCondition: V12EndCondition) {
    execSQL(
        """
            INSERT INTO end_condition_table (id, scenario_id, event_id, executions) 
            VALUES (${endCondition.id}, "${endCondition.scenarioId}", ${endCondition.eventId}, ${endCondition.executions})
        """.trimIndent()
    )
}

data class V12Event(
    val id: Long,
    val scenarioId: Long,
    val name: String = "toto",
    val operator: Int = 2,
    val priority: Int = 0,
    val enabledOnStart: Boolean = true,
)

internal fun SupportSQLiteDatabase.insertV12Event(event: V12Event) {
    execSQL(
        """
            INSERT INTO event_table (id, scenario_id, name, operator, priority, enabled_on_start) 
            VALUES (${event.id}, ${event.scenarioId}, "${event.name}", ${event.operator}, ${event.priority}, ${event.enabledOnStart.toSqlite()})
        """.trimIndent()
    )
}

data class V12Condition(
    var id: Long,
    var eventId: Long,
    val name: String = "tata",
    var path: String = "/toto/tutu",
    val area: Rect = Rect(1, 2, 3, 4),
    val threshold: Int = 4,
    val detectionType: Int = 1,
    val shouldBeDetected: Boolean = true,
    val detectionArea: Rect? = null,
)

internal fun SupportSQLiteDatabase.insertV12Condition(condition: V12Condition) {
    execSQL(
        """
            INSERT INTO condition_table (id, eventId, name, path, area_left, area_top, area_right, area_bottom, detection_area_left, detection_area_top, detection_area_right, detection_area_bottom, threshold, detection_type, shouldBeDetected) 
            VALUES (${condition.id}, ${condition.eventId}, "${condition.name}","${condition.path}", ${condition.area.toSqlite()}, ${condition.detectionArea.toSqlite()}, ${condition.threshold}, ${condition.detectionType}, ${condition.shouldBeDetected.toSqlite()})
        """.trimIndent()
    )
}

data class V12ToggleEvent(
    var id: Long,
    var eventId: Long,
    val name: String = "toggleEvent",
    var type: ActionType = ActionType.TOGGLE_EVENT,
    val priority: Int = 0,
    var toggleEventId: Long = 2L,
    val toggleType: EventToggleType = EventToggleType.TOGGLE,
)

internal fun SupportSQLiteDatabase.insertV12ToggleEvent(action: V12ToggleEvent) {
    execSQL(
        """
            INSERT INTO action_table (id, eventId, name, type, priority, toggle_event_id, toggle_type) 
            VALUES (${action.id}, ${action.eventId}, "${action.name}","${action.type.name}", ${action.priority}, ${action.toggleEventId}, "${action.toggleType.name}")
        """.trimIndent()
    )
}