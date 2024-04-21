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
package com.buzbuz.smartautoclicker.core.database.serialization.compat

import com.buzbuz.smartautoclicker.core.base.extensions.getBoolean
import com.buzbuz.smartautoclicker.core.base.extensions.getInt
import com.buzbuz.smartautoclicker.core.base.extensions.getLong
import com.buzbuz.smartautoclicker.core.base.extensions.getString
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity

import kotlinx.serialization.json.JsonObject

/** Deserializer for all Json object version below 11. */
internal open class CompatV11Deserializer : CompatV13Deserializer() {

    override fun deserializeScenarioDetectionQuality(jsonScenario: JsonObject): Int =
        super.deserializeScenarioDetectionQuality(jsonScenario) + 600

    /**
     * Before v11, they were no condition attached when selecting this clickOnCondition. Starting with v11,
     * condition id is attached and position type is clearly defined. To fix this, we search for the first condition
     * that should be detected.
     */
    override fun deserializeActionClick(jsonClick: JsonObject, eventConditions: List<ConditionEntity>): ActionEntity? {
        val id = jsonClick.getLong("id", true) ?: return null
        val eventId = jsonClick.getLong("eventId", true) ?: return null
        val clickOnCondition = jsonClick.getBoolean("clickOnCondition") ?: return null

        val x: Int?
        val y: Int?
        val clickOnConditionId: Long?
        val clickPositionType: ClickPositionType

        if (clickOnCondition) {
            x = null
            y = null
            clickOnConditionId = eventConditions.find { it.shouldBeDetected == true }?.id
            clickPositionType = ClickPositionType.ON_DETECTED_CONDITION
        } else {
            x = jsonClick.getInt("x", true) ?: return null
            y = jsonClick.getInt("y", true) ?: return null
            clickOnConditionId = null
            clickPositionType = ClickPositionType.USER_SELECTED
        }

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonClick.getString("name") ?: "",
            priority = jsonClick.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.CLICK,
            clickPositionType = clickPositionType,
            clickOnConditionId = clickOnConditionId,
            x = x,
            y = y,
            pressDuration = jsonClick.getLong("pressDuration")
                ?.coerceIn(DURATION_LOWER_BOUND..DURATION_GESTURE_UPPER_BOUND)
                ?: DEFAULT_CLICK_DURATION,
        )
    }

}