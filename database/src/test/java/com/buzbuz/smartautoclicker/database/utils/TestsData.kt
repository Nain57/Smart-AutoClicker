/*
 * Copyright (C) 2020 Nain57
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

import com.buzbuz.smartautoclicker.database.ClickInfo
import com.buzbuz.smartautoclicker.database.room.ClickEntity
import com.buzbuz.smartautoclicker.database.room.ClickWithConditions
import com.buzbuz.smartautoclicker.database.room.ConditionEntity
import com.buzbuz.smartautoclicker.database.room.ScenarioEntity

/** Data set for the database tests. */
internal object TestsData {

    const val SCENARIO_ID = 42L
    const val SCENARIO_NAME = "ClickScenario"
    val SCENARIO_ENTITY = ScenarioEntity(SCENARIO_ID, SCENARIO_NAME)

    const val SCENARIO_ID_2 = 51L
    const val SCENARIO_NAME_2 = "Another ClickScenario"
    val SCENARIO_ENTITY_2 = ScenarioEntity(SCENARIO_ID_2, SCENARIO_NAME_2)

    const val CLICK_ID = 1667L
    const val CLICK_SCENARIO_ID = SCENARIO_ID
    const val CLICK_NAME = "ClickName"
    const val CLICK_TYPE = ClickInfo.SINGLE
    const val CLICK_FROMX = 100
    const val CLICK_FROMY = 200
    const val CLICK_TOX = 0
    const val CLICK_TOY = 0
    const val CLICK_CONDITION_OPERATOR = ClickInfo.AND
    const val CLICK_DELAY_AFTER = 1250L
    const val CLICK_PRIORITY = 0
    val CLICK_ENTITY = ClickEntity(
        CLICK_ID, SCENARIO_ID, CLICK_NAME, CLICK_TYPE, CLICK_FROMX, CLICK_FROMY, CLICK_TOX, CLICK_TOY,
        CLICK_CONDITION_OPERATOR, CLICK_DELAY_AFTER, CLICK_PRIORITY
    )
    /**
     * Instantiates a new [ClickWithConditions] based on [CLICK_ENTITY].
     * @param scenarioId the scenario for this click
     * @param clickId the id for this click. Use 0 to let the database creates one.
     * @param conditions the list of conditions for this click.
     */
    fun newClickWithConditionEntity(scenarioId: Long, clickId: Long = 0, conditions: List<ConditionEntity> = emptyList()) =
        ClickWithConditions(CLICK_ENTITY.copy(clickId = clickId, scenarioId = scenarioId), conditions)

    const val CLICK_ID_2 = 1792L
    const val CLICK_SCENARIO_ID_2 = SCENARIO_ID_2
    const val CLICK_NAME_2 = "Another ClickName"
    const val CLICK_TYPE_2 = ClickInfo.SWIPE
    const val CLICK_FROMX_2 = 200
    const val CLICK_FROMY_2 = 300
    const val CLICK_TOX_2 = 500
    const val CLICK_TOY_2 = 700
    const val CLICK_CONDITION_OPERATOR_2 = ClickInfo.OR
    const val CLICK_DELAY_AFTER_2 = 300L
    const val CLICK_PRIORITY_2 = 1
    val CLICK_ENTITY_2 = ClickEntity(
        CLICK_ID_2, SCENARIO_ID_2, CLICK_NAME_2, CLICK_TYPE_2, CLICK_FROMX_2, CLICK_FROMY_2,
        CLICK_TOX_2, CLICK_TOY_2, CLICK_CONDITION_OPERATOR_2, CLICK_DELAY_AFTER_2, CLICK_PRIORITY_2
    )
    /**
     * Instantiates a new [ClickWithConditions] based on [CLICK_ENTITY_2].
     * @param scenarioId the scenario for this click
     * @param clickId the id for this click. Use 0 to let the database creates one.
     * @param conditions the list of conditions for this click.
     */
    fun newClickWithConditionEntity2(scenarioId: Long, clickId: Long = 0, conditions: List<ConditionEntity> = emptyList()) =
        ClickWithConditions(CLICK_ENTITY.copy(clickId = clickId, scenarioId = scenarioId), conditions)

    const val CONDITION_PATH = "/toto/tutu/tata"
    const val CONDITION_LEFT = 0
    const val CONDITION_TOP = 45
    const val CONDITION_RIGHT = 69
    const val CONDITION_BOTTOM = 89
    const val CONDITION_WIDTH = CONDITION_RIGHT - CONDITION_LEFT
    const val CONDITION_HEIGHT = CONDITION_BOTTOM - CONDITION_TOP
    val CONDITION_ENTITY = ConditionEntity(
        CONDITION_PATH, CONDITION_LEFT, CONDITION_TOP, CONDITION_RIGHT,
        CONDITION_BOTTOM, CONDITION_WIDTH, CONDITION_HEIGHT
    )

    const val CONDITION_PATH_2 = "/titi/tete/tyty"
    const val CONDITION_LEFT_2 = -50
    const val CONDITION_TOP_2 = -50
    const val CONDITION_RIGHT_2 = 50
    const val CONDITION_BOTTOM_2 = 50
    const val CONDITION_WIDTH_2 = CONDITION_RIGHT_2 - CONDITION_LEFT_2
    const val CONDITION_HEIGHT_2 = CONDITION_BOTTOM_2 - CONDITION_TOP_2
    val CONDITION_ENTITY_2 = ConditionEntity(
        CONDITION_PATH_2, CONDITION_LEFT_2, CONDITION_TOP_2, CONDITION_RIGHT_2,
        CONDITION_BOTTOM_2, CONDITION_WIDTH_2, CONDITION_HEIGHT_2
    )
}