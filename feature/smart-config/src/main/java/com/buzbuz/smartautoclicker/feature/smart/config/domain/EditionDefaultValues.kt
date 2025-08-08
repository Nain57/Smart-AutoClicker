
package com.buzbuz.smartautoclicker.feature.smart.config.domain

import android.content.Context

import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getClickPressDurationConfig
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getIntentIsAdvancedConfig
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getPauseDurationConfig
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getSwipeDurationConfig

internal class EditionDefaultValues(private val scenarioRepository: IRepository) {

    fun eventName(context: Context): String =
        context.getString(R.string.default_event_name)
    @ConditionOperator fun eventConditionOperator(): Int =
        AND

    fun conditionName(context: Context): String =
        context.getString(R.string.default_condition_name)
    fun conditionThreshold(context: Context): Int =
        context.resources.getInteger(R.integer.default_condition_threshold)
    fun conditionDetectionType(): Int =
        EXACT
    fun conditionShouldBeDetected(): Boolean =
        true

    fun clickName(context: Context): String =
        context.getString(R.string.default_click_name)
    fun clickPressDuration(context: Context): Long =
        context.getEventConfigPreferences().getClickPressDurationConfig(context)
    fun clickPositionType(): Click.PositionType =
        Click.PositionType.USER_SELECTED

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
    fun eventToggleType(): ToggleEvent.ToggleType =
        ToggleEvent.ToggleType.ENABLE

    fun changeCounterName(context: Context): String =
        context.getString(R.string.default_change_counter_name)

    fun notificationName(context: Context): String =
        context.getString(R.string.default_notification_name)

    fun counterComparisonOperation(): TriggerCondition.OnCounterCountReached.ComparisonOperation =
        TriggerCondition.OnCounterCountReached.ComparisonOperation.EQUALS

}