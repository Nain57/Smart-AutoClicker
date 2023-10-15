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
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.content.ComponentName
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType

import com.buzbuz.smartautoclicker.core.database.entity.ToggleEventType
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/** Base for for all possible actions for an Event. */
sealed class Action {

    /** The unique identifier for the action. */
    abstract val id: Identifier
    /** The identifier of the event for this action. */
    abstract val eventId: Identifier
    /** The name of the action. */
    abstract val name: String?

    /** @return true if this action is complete and can be transformed into its entity. */
    open fun isComplete(): Boolean = name != null

    /** @return creates a deep copy of this action. */
    abstract fun deepCopy(): Action

    /**
     * Click action.
     *
     * @param id the unique identifier for the action.
     * @param eventId the identifier of the event for this action.
     * @param name the name of the action.
     * @param pressDuration the duration between the click down and up in milliseconds.
     * @param x the x position of the click.
     * @param y the y position of the click.
     */
    data class Click(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        val pressDuration: Long? = null,
        val positionType: PositionType,
        val x: Int? = null,
        val y: Int? = null,
        val clickOnConditionId: Identifier? = null,
    ) : Action() {

        /**
         * Types of click positions a [Click].
         * Keep the same names as the db ones.
         */
        enum class PositionType {
            /** The user must manually select a position to be clicked. */
            USER_SELECTED,
            /**
             * Click on the detected condition.
             * When the condition operator is AND, click on the condition specified by the user.
             * When the condition operator is OR, click on the condition detected condition.
             */
            ON_DETECTED_CONDITION;

            fun toEntity(): ClickPositionType = ClickPositionType.valueOf(name)
        }

        override fun isComplete(): Boolean =
            super.isComplete() && pressDuration != null && isPositionValid()


        override fun deepCopy(): Click = copy(name = "" + name)

        private fun isPositionValid(): Boolean =
            (positionType == PositionType.USER_SELECTED && x != null && y != null) || positionType == PositionType.ON_DETECTED_CONDITION
    }

    /**
     * Swipe action.
     *
     * @param id the unique identifier for the action.
     * @param eventId the identifier of the event for this action.
     * @param name the name of the action.
     * @param swipeDuration the duration between the swipe start and end in milliseconds.
     * @param fromX the x position of the swipe start.
     * @param fromY the y position of the swipe start.
     * @param toX the x position of the swipe end.
     * @param toY the y position of the swipe end.
     */
    data class Swipe(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        val swipeDuration: Long? = null,
        val fromX: Int? = null,
        val fromY: Int? = null,
        val toX: Int? = null,
        val toY: Int? = null,
    ) : Action() {

        override fun isComplete(): Boolean =
            super.isComplete() && swipeDuration != null && fromX != null && fromY != null && toX != null && toY != null

        override fun deepCopy(): Swipe = copy(name = "" + name)
    }

    /**
     * Pause action.
     *
     * @param id the unique identifier for the action.
     * @param eventId the identifier of the event for this action.
     * @param name the name of the action.
     * @param pauseDuration the duration of the pause in milliseconds.
     */
    data class Pause(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        val pauseDuration: Long? = null,
    ) : Action() {

        override fun isComplete(): Boolean = super.isComplete() && pauseDuration != null

        override fun deepCopy(): Pause = copy(name = "" + name)
    }

    /**
     * Intent action.
     *
     * @param id the unique identifier for the action.
     * @param eventId the identifier of the event for this action.
     * @param name the name of the action.
     * @param isAdvanced if false, the user have used the simple config. If true, the advanced config.
     * @param isBroadcast true if this intent should be a broadcast, false for a startActivity.
     * @param intentAction the action of the intent.
     * @param componentName the component name for the intent. Can be null for a broadcast.
     * @param flags the flags for the intent.
     * @param extras the list of extras to sent with the intent.
     */
    data class Intent(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        val isAdvanced: Boolean? = null,
        val isBroadcast: Boolean? = null,
        val intentAction: String? = null,
        val componentName: ComponentName? = null,
        val flags: Int? = null,
        val extras: List<IntentExtra<out Any>>? = null,
    ) : Action() {

        override fun isComplete(): Boolean {
            if (!super.isComplete()) return false

            if (isAdvanced == null || intentAction == null || flags == null) return false
            extras?.forEach { extra -> if (!extra.isComplete()) return false }

            return true
        }


        override fun deepCopy(): Intent = copy(name = "" + name)
    }

    /**
     * Toggle Event Action.
     *
     * @param id the unique identifier for the action.
     * @param eventId the identifier of the event for this action.
     * @param name the name of the action.
     * @param toggleEventId the identifier of the event to manipulate.
     * @param toggleEventType the type of manipulation to apply.
     */
    data class ToggleEvent(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        val toggleEventId: Identifier? = null,
        val toggleEventType: ToggleType? = null,
    ) : Action() {

        /**
         * Types of toggle of a [ToggleEvent].
         * Keep the same names as the db ones.
         */
        enum class ToggleType {
            /** Enable the event. Has no effect if the event is already enabled. */
            ENABLE,
            /** Disable the event. Has no effect if the event is already disabled. */
            DISABLE,
            /** Enable the event if it is disabled, disable it if it is enabled. */
            TOGGLE;

            fun toEntity(): ToggleEventType = ToggleEventType.valueOf(name)
        }

        override fun isComplete(): Boolean = super.isComplete() && toggleEventId != null && toggleEventType != null

        override fun deepCopy(): ToggleEvent = copy(name = "" + name)
    }
}
