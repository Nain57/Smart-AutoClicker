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
package com.buzbuz.smartautoclicker.core.domain.model.scenario

import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.domain.utils.asIdentifier

internal object ScenarioTestsData {

    /* ------- Scenario Data ------- */

    const val SCENARIO_ID = 42L
    const val SCENARIO_NAME = "ClickScenario"
    const val SCENARIO_DETECTION_QUALITY = 500
    const val SCENARIO_RANDOMIZE = false

    fun getNewScenarioEntity(
        id: Long = SCENARIO_ID,
        name: String = SCENARIO_NAME,
        detectionQuality: Int = SCENARIO_DETECTION_QUALITY,
        randomize: Boolean = SCENARIO_RANDOMIZE,
    ) = ScenarioEntity(id, name, detectionQuality, randomize)

    fun getNewScenario(
        id: Long = SCENARIO_ID,
        name: String = SCENARIO_NAME,
        detectionQuality: Int = SCENARIO_DETECTION_QUALITY,
        randomize: Boolean = SCENARIO_RANDOMIZE,
        eventCount: Int = 0,
    ) = Scenario(id.asIdentifier(), name, detectionQuality, randomize, eventCount)
}