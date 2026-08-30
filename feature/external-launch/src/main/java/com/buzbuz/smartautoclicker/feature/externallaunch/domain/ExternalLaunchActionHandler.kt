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
package com.buzbuz.smartautoclicker.feature.externallaunch.domain

import android.content.Intent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

interface ExternalLaunchActionHandler {
    fun isRunning(): Boolean
    fun isScenarioRunning(): Boolean
    fun isOverlayVisible(): Boolean
    fun isOverlayHidden(): Boolean
    fun isScenarioConfigurationOpen(): Boolean
    fun isSmartScreenRecordActive(): Boolean
    fun getSmartScenarioId(): Long?
    fun getDumbScenarioId(): Long?
    fun launchDumbScenario(dumbScenario: DumbScenario)
    fun launchSmartScenario(resultCode: Int, data: Intent, scenario: Scenario)
    fun replaceDumbScenario(dumbScenario: DumbScenario)
    fun replaceSmartScenario(resultCode: Int, data: Intent, scenario: Scenario)
    fun replaceSmartScenarioWithCurrentProjection(scenario: Scenario)
    fun runCurrentScenario()
    fun stop()
}
