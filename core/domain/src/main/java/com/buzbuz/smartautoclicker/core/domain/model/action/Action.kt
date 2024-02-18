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
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.content.ComponentName

import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType

/** Base for for all possible actions for an Event. */
sealed class Action : Identifiable, Completable {

    /** The identifier of the event for this action. */
    abstract val eventId: Identifier
    /** The name of the action. */
    abstract val name: String?
    /** The name of the action. */
    abstract val priority: Int

    /** @return true if this action is complete and can be transformed into its entity. */
    override fun isComplete(): Boolean = name != null

    abstract fun hashCodeNoIds(): Int

    /** @return creates a deep copy of this action. */
    abstract fun deepCopy(): Action

    fun copyBase(
        id: Identifier = this.id,
        eventId: Identifier = this.eventId,
        name: String? = this.name,
        priority: Int = this.priority,
    ): Action =
        when (this) {
            is Click -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is ChangeCounter -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Intent -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Pause -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Swipe -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is ToggleEvent -> copy(id = id, eventId = eventId, name = name, priority = priority)
        }

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
        override val priority: Int,
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

        override fun hashCodeNoIds(): Int =
            name.hashCode() + pressDuration.hashCode() + positionType.hashCode() + x.hashCode() + y.hashCode() +
                    clickOnConditionId.hashCode()


        override fun deepCopy(): Click = copy(name = "" + name)

        private fun isPositionValid(): Boolean =
            (positionType == PositionType.USER_SELECTED && x != null && y != null) || positionType == PositionType.ON_DETECTED_CONDITION

        fun isClickOnConditionValid(): Boolean =
            (positionType == PositionType.ON_DETECTED_CONDITION && clickOnConditionId != null) || positionType == PositionType.USER_SELECTED
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
        override val priority: Int,
        val swipeDuration: Long? = null,
        val fromX: Int? = null,
        val fromY: Int? = null,
        val toX: Int? = null,
        val toY: Int? = null,
    ) : Action() {

        override fun isComplete(): Boolean =
            super.isComplete() && swipeDuration != null && fromX != null && fromY != null && toX != null && toY != null

        override fun hashCodeNoIds(): Int =
            name.hashCode() + swipeDuration.hashCode() + fromX.hashCode() + fromY.hashCode() + toX.hashCode() +
                    toY.hashCode()

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
        override val priority: Int,
        val pauseDuration: Long? = null,
    ) : Action() {

        override fun isComplete(): Boolean = super.isComplete() && pauseDuration != null

        override fun hashCodeNoIds(): Int =
            name.hashCode() + pauseDuration.hashCode()


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
        override val priority: Int,
        val isAdvanced: Boolean? = null,
        val isBroadcast: Boolean,
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

        override fun hashCodeNoIds(): Int =
            name.hashCode() + isAdvanced.hashCode() + isBroadcast.hashCode() + intentAction.hashCode() +
                    componentName.hashCode() + flags.hashCode() + extras.hashCode()

        override fun deepCopy(): Intent = copy(name = "" + name)
    }

    /**
     * Toggle Event Action.
     *
     * @param id the unique identifier for the action.
     * @param eventId the identifier of the event for this action.
     * @param name the name of the action.
     * @param toggleAll true to toggle all events, false to control only via EventToggle.
     * @param toggleAllType the type of manipulation to apply for toggle all.
     */
    data class ToggleEvent(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        override val priority: Int,
        val toggleAll: Boolean = false,
        val toggleAllType: ToggleType? = null,
        val eventToggles: List<EventToggle> = emptyList(),
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

            fun toEntity(): EventToggleType = EventToggleType.valueOf(name)
        }

        override fun isComplete(): Boolean {
            if (!super.isComplete()) return false

            return if (toggleAll) {
                toggleAllType != null
            } else {
                eventToggles.isNotEmpty() && eventToggles.find { !it.isComplete() } == null
            }
        }

        override fun hashCodeNoIds(): Int =
            name.hashCode() + toggleAll.hashCode() + toggleAllType.hashCode() + eventToggles.hashCode()

        override fun deepCopy(): ToggleEvent = copy(name = "" + name)
    }

    data class ChangeCounter(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        override val priority: Int,
        val counterName: String,
        val operation: OperationType,
        val operationValue: Int,
    ): Action() {

        /**
         * Types of counter change of a [ChangeCounter].
         * Keep the same names as the db ones.
         */
        enum class OperationType {
            /** Add to the current counter value. */
            ADD,
            /** Remove from the current counter value. */
            MINUS,
            /** Set the counter to a specific value. */
            SET;

            fun toEntity(): ChangeCounterOperationType = ChangeCounterOperationType.valueOf(name)
        }

        override fun isComplete(): Boolean =
            super.isComplete() && counterName.isNotEmpty() && operationValue >= 0

        override fun hashCodeNoIds(): Int =
            name.hashCode() + counterName.hashCode() + operation.hashCode() + operationValue.hashCode()

        override fun deepCopy(): ChangeCounter = copy(
            name = "" + name,
            counterName = "" + counterName,
        )
    }
}
