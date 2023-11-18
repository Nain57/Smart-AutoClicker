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
package com.buzbuz.smartautoclicker.feature.scenario.config.domain

import android.content.Context

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getClickPressDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getIntentIsAdvancedConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getPauseDurationConfig
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getSwipeDurationConfig

internal class EditionDefaultValues(context: Context) {

    private val scenarioRepository: Repository = Repository.getRepository(context)

    private fun isTutorialModeEnabled(): Boolean = scenarioRepository.isTutorialModeEnabled()

    fun eventName(context: Context): String =
        context.getString(R.string.default_event_name)
    @ConditionOperator fun eventConditionOperator(): Int =
        AND

    fun conditionName(context: Context): String =
        context.getString(R.string.default_condition_name)
    fun conditionThreshold(context: Context): Int =
        if (isTutorialModeEnabled()) 15
        else context.resources.getInteger(R.integer.default_condition_threshold)
    fun conditionDetectionType(): Int =
        EXACT
    fun conditionShouldBeDetected(): Boolean =
        true

    fun clickName(context: Context): String =
        context.getString(R.string.default_click_name)
    fun clickPressDuration(context: Context): Long =
        if (isTutorialModeEnabled()) 1
        else context.getEventConfigPreferences().getClickPressDurationConfig(context)
    fun clickPositionType(): Action.Click.PositionType =
        Action.Click.PositionType.USER_SELECTED

    fun swipeName(context: Context): String =
        context.getString(R.string.default_swipe_name)
    fun swipeDuration(context: Context): Long =
        context.getEventConfigPreferences().getSwipeDurationConfig(context)

    fun pauseName(context: Context): String =
        context.getString(R.string.default_pause_name)
    fun pauseDuration(context: Context): Long =
        context.getEventConfigPreferences().getPauseDurationConfig(context)

    fun intentName(context: Context): String =
        context.getString(R.string.default_intent_name)
    fun intentIsAdvanced(context: Context): Boolean =
        context.getEventConfigPreferences().getIntentIsAdvancedConfig(context)

    fun toggleEventName(context: Context): String =
        context.getString(R.string.default_toggle_event_name)
    fun toggleEventType(): Action.ToggleEvent.ToggleType =
        Action.ToggleEvent.ToggleType.ENABLE
}