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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.domain

import android.content.Context
import android.graphics.Point

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.identifier.IdentifierCreator
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getDefaultDumbClickDurationMs
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getDefaultDumbClickName
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getDefaultDumbPauseDurationMs
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getDefaultDumbPauseName
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getDefaultDumbSwipeDurationMs
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.data.getDefaultDumbSwipeName

class EditedDumbActionsBuilder {

    private val dumbActionsIdCreator = IdentifierCreator()

    private var dumbScenarioId: Identifier? = null

    internal fun startEdition(scenarioId: Identifier) {
        dumbActionsIdCreator.resetIdCount()
        dumbScenarioId = scenarioId
    }

    internal fun clearState() {
        dumbActionsIdCreator.resetIdCount()
        dumbScenarioId = null
    }

    fun createNewDumbClick(context: Context, position: Point): DumbAction.DumbClick =
        DumbAction.DumbClick(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbClickName(),
            position = position,
            pressDurationMs = context.getDefaultDumbClickDurationMs(),
            repeatCount = 1,
            isRepeatInfinite = false,
            repeatDelayMs = 0,
        )

    fun createNewDumbSwipe(context: Context, from: Point, to: Point): DumbAction.DumbSwipe =
        DumbAction.DumbSwipe(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbSwipeName(),
            fromPosition = from,
            toPosition = to,
            swipeDurationMs = context.getDefaultDumbSwipeDurationMs(),
            repeatCount = 1,
            isRepeatInfinite = false,
            repeatDelayMs = 0,
        )

    fun createNewDumbPause(context: Context): DumbAction.DumbPause =
        DumbAction.DumbPause(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbPauseName(),
            pauseDurationMs = context.getDefaultDumbPauseDurationMs(),
        )

    private fun getEditedScenarioIdOrThrow(): Identifier = dumbScenarioId
        ?: throw IllegalStateException("Can't create items without an edited dumb scenario")
}