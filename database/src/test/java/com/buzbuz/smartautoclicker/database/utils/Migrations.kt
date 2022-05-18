/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.database.utils


// ----- Utils for Database V3 -----

fun getInsertV3Scenario(id: Long, name: String) =
    """
        INSERT INTO scenario_table (id, name) 
        VALUES ($id, "$name")
    """.trimIndent()


fun getInsertV3Click(id: Long, scenarioId: Long, name: String, type: Int, fromX: Int, fromY: Int, toX: Int, toY: Int,
                     operator: Int, delayAfter: Long, stopAfter: Int, priority: Int) =
    """
        INSERT INTO click_table (clickId, scenario_id, name, type, from_X, from_y, to_x, to_y, operator, delay_after, stop_after, priority) 
        VALUES ($id, $scenarioId, "$name", $type, $fromX, $fromY, $toX, $toY, $operator, $delayAfter, $stopAfter, $priority)
    """.trimIndent()


fun getInsertV3Condition(path: String, left: Int, top: Int, right: Int, bottom: Int, width: Int, height: Int, threshold: Int) =
    """
        INSERT INTO condition_table (path, area_left, area_top, area_right, area_bottom, width, height, threshold) 
        VALUES ("$path", $left, $top, $right, $bottom, $width, $height, $threshold)
    """.trimIndent()


fun getInsertV3ConditionCrossRef(clickId: Long, path: String) =
    """
        INSERT INTO ClickConditionCrossRef (clickId, path) 
        VALUES ($clickId, "$path")
    """.trimIndent()


// ----- Utils for Database V4 -----

fun getV4Scenarios() = "SELECT * FROM scenario_table"
fun getV4Events() = "SELECT * FROM event_table"
fun getV4Conditions() = "SELECT * FROM condition_table"
fun getV4Actions() = "SELECT * FROM action_table"

fun getInsertV4Condition(id: Long, eventId: Long, path: String, left: Int, top: Int, right: Int, bottom: Int,
                         threshold: Int) =
    """
        INSERT INTO condition_table (id, eventId, path, area_left, area_top, area_right, area_bottom, threshold) 
        VALUES ($id, $eventId, "$path", $left, $top, $right, $bottom, $threshold)
    """.trimIndent()


// ----- Utils for Database V5 -----

fun getV5Conditions() = "SELECT * FROM condition_table"

fun getInsertV5Click(id: Long, eventId: Long, name: String, type: String, x: Int, y: Int, pressDuration: Long, priority: Int) =
    """
        INSERT INTO action_table (id, eventId, priority, name, type, x, y, pressDuration) 
        VALUES ($id, $eventId, $priority, "$name", "$type", $x, $y, $pressDuration)
    """.trimIndent()

fun getInsertV5Swipe(id: Long, eventId: Long, name: String, type: String, fromX: Int, fromY: Int, toX: Int, toY: Int, swipeDuration: Long, priority: Int) =
    """
        INSERT INTO action_table (id, eventId, priority, name, type, fromX, fromY, toX, toY, swipeDuration) 
        VALUES ($id, $eventId, $priority, "$name", "$type", $fromX, $fromY, $toX, $toY, $swipeDuration)
    """.trimIndent()

fun getInsertV5Pause(id: Long, eventId: Long, name: String, type: String, pauseDuration: Long, priority: Int) =
    """
        INSERT INTO action_table (id, eventId, priority, name, type, pauseDuration) 
        VALUES ($id, $eventId, $priority, "$name", "$type", $pauseDuration)
    """.trimIndent()

fun getInsertV5Condition(id: Long, eventId: Long, name: String, path: String, left: Int, top: Int, right: Int, bottom: Int,
                         threshold: Int, detectionType: Int) =
    """
        INSERT INTO condition_table (id, eventId, name, path, area_left, area_top, area_right, area_bottom, threshold, detection_type) 
        VALUES ($id, $eventId, "$name","$path", $left, $top, $right, $bottom, $threshold, $detectionType)
    """.trimIndent()


// ----- Utils for Database V6 -----

fun getV6Actions() = "SELECT * FROM action_table"
fun getV6Conditions() = "SELECT * FROM condition_table"

fun getInsertV6Scenario(id: Long, name: String) =
    """
        INSERT INTO scenario_table (id, name) 
        VALUES ($id, "$name")
    """.trimIndent()

fun getInsertV6Event(
    id: Long,
    scenarioId: Long,
    name: String,
    conditionOperator: Int,
    stopAfter: Int?,
    priority: Int,
) =
    """
        INSERT INTO event_table (id, scenario_id, name, operator, priority, stop_after) 
        VALUES ($id, $scenarioId, "$name", $conditionOperator, $priority, $stopAfter)
    """.trimIndent()

// ----- Utils for Database V7 -----

fun getV7Scenario() = "SELECT * FROM scenario_table"
fun getV7EndCondition() = "SELECT * FROM end_condition_table"
