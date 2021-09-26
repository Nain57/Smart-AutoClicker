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

import com.buzbuz.smartautoclicker.database.domain.ConditionOperator

fun getInsertV3Scenario(id: Long, name: String) =
    """
        INSERT INTO scenario_table (id, name) 
        VALUES ($id, "$name")
    """.trimIndent()


fun getInsertV3Click(id: Long, scenarioId: Long, name: String, type: Int, fromX: Int, fromY: Int, toX: Int, toY: Int,
                     @ConditionOperator operator: Int, delayAfter: Long, stopAfter: Int, priority: Int) =
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


fun getV4Scenarios() = "SELECT * FROM scenario_table"
fun getV4Events() = "SELECT * FROM event_table"
fun getV4Conditions() = "SELECT * FROM condition_table"
fun getV4Actions() = "SELECT * FROM action_table"
