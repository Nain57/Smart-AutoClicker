/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.dumb.utils

// ----- Utils for Dumb Database V2 -----

fun getInsertV2Scenario(id: Long, name: String) =
    """
        INSERT INTO dumb_scenario_table (id, name, repeat_count, is_repeat_infinite, max_duration_minutes, is_duration_infinite, randomize)
        VALUES ($id, "$name", 1, 1, 1, 1, 0)
    """.trimIndent()

fun getInsertV2Click(id: Long, scenarioId: Long, name: String, priority: Int, x: Int, y: Int, pressDuration: Long) =
    """
        INSERT INTO dumb_action_table (id, dumb_scenario_id, priority, name, type, repeat_count, is_repeat_infinite, repeat_delay, press_duration, x, y)
        VALUES ($id, $scenarioId, $priority, "$name", "CLICK", 1, 0, 0, $pressDuration, $x, $y)
    """.trimIndent()

fun getInsertV2Swipe(id: Long, scenarioId: Long, name: String, priority: Int, fromX: Int, fromY: Int, toX: Int, toY: Int, swipeDuration: Long) =
    """
        INSERT INTO dumb_action_table (id, dumb_scenario_id, priority, name, type, repeat_count, is_repeat_infinite, repeat_delay, swipe_duration, fromX, fromY, toX, toY)
        VALUES ($id, $scenarioId, $priority, "$name", "SWIPE", 1, 0, 0, $swipeDuration, $fromX, $fromY, $toX, $toY)
    """.trimIndent()

fun getInsertV2Pause(id: Long, scenarioId: Long, name: String, priority: Int, pauseDuration: Long) =
    """
        INSERT INTO dumb_action_table (id, dumb_scenario_id, priority, name, type, pause_duration)
        VALUES ($id, $scenarioId, $priority, "$name", "PAUSE", $pauseDuration)
    """.trimIndent()


// ----- Utils for Dumb Database V3 -----

fun getV3Actions() = "SELECT * FROM dumb_action_table ORDER BY id"
fun getV3GestureFingers() = "SELECT * FROM dumb_gesture_finger_table ORDER BY id"
fun getV3GestureFingerMotions() = "SELECT * FROM dumb_gesture_finger_motion_table ORDER BY id"
fun getV3GestureFingersForAction(actionId: Long) =
    "SELECT * FROM dumb_gesture_finger_table WHERE dumb_action_id = $actionId ORDER BY `index`"
fun getV3GestureFingerMotionsForFinger(fingerId: Long) =
    "SELECT * FROM dumb_gesture_finger_motion_table WHERE finger_id = $fingerId ORDER BY `index`"
