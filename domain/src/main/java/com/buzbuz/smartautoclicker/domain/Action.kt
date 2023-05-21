/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.domain

import android.content.ComponentName

import com.buzbuz.smartautoclicker.database.room.entity.ActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.ActionType
import com.buzbuz.smartautoclicker.database.room.entity.CompleteActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.ToggleEventType

/** Base for for all possible actions for an Event. */
sealed class Action {

    /** The unique identifier for the action. Use 0 for creating a new action. Default value is 0. */
    abstract var id: Long
    /** The identifier of the event for this action. */
    abstract var eventId: Long
    /** The name of the action. */
    abstract var name: String?

    /** @return true if this action is complete and can be transformed into its entity. */
    open fun isComplete(): Boolean = name != null
    /** @return the entity equivalent of this action. */
    internal abstract fun toEntity(): CompleteActionEntity
    /** Cleanup all ids contained in this action. Ideal for copying. */
    internal abstract fun cleanUpIds()
    /** @return creates a deep copy of this action. */
    abstract fun deepCopy(): Action

    /**
     * Click action.
     *
     * @param id the unique identifier for the action. Use 0 for creating a new action. Default value is 0.
     * @param eventId the identifier of the event for this action.
     * @param name the name of the action.
     * @param pressDuration the duration between the click down and up in milliseconds.
     * @param x the x position of the click.
     * @param y the y position of the click.
     */
    data class Click(
        override var id: Long = 0,
        override var eventId: Long,
        override var name: String? = null,
        var pressDuration: Long? = null,
        var x: Int? = null,
        var y: Int? = null,
        var clickOnCondition: Boolean,
    ) : Action() {

        override fun isComplete(): Boolean =
            super.isComplete() && pressDuration != null && ((x != null && y != null) || clickOnCondition)

        override fun toEntity(): CompleteActionEntity {
            if (!isComplete()) throw IllegalStateException("Can't transform to entity, Click is incomplete.")

            return CompleteActionEntity(
                action = ActionEntity(
                    id = id,
                    eventId = eventId,
                    name = name!!,
                    type = ActionType.CLICK,
                    pressDuration = pressDuration,
                    x = x,
                    y = y,
                    clickOnCondition = clickOnCondition,
                ),
                intentExtras = emptyList(),
            )
        }

        override fun cleanUpIds() {
            id = 0
            eventId = 0
        }

        override fun deepCopy(): Click = copy(name = "" + name)
    }

    /**
     * Swipe action.
     *
     * @param id the unique identifier for the action. Use 0 for creating a new action. Default value is 0.
     * @param eventId the identifier of the event for this action.
     * @param name the name of the action.
     * @param swipeDuration the duration between the swipe start and end in milliseconds.
     * @param fromX the x position of the swipe start.
     * @param fromY the y position of the swipe start.
     * @param toX the x position of the swipe end.
     * @param toY the y position of the swipe end.
     */
    data class Swipe(
        override var id: Long = 0,
        override var eventId: Long,
        override var name: String? = null,
        var swipeDuration: Long? = null,
        var fromX: Int? = null,
        var fromY: Int? = null,
        var toX: Int? = null,
        var toY: Int? = null,
    ) : Action() {

        override fun isComplete(): Boolean =
            super.isComplete() && swipeDuration != null && fromX != null && fromY != null && toX != null && toY != null

        override fun toEntity(): CompleteActionEntity {
            if (!isComplete()) throw IllegalStateException("Can't transform to entity, Swipe is incomplete.")

            return CompleteActionEntity(
                action = ActionEntity(
                    id = id,
                    eventId = eventId,
                    name = name!!,
                    type = ActionType.SWIPE,
                    swipeDuration = swipeDuration,
                    fromX = fromX,
                    fromY = fromY,
                    toX = toX,
                    toY = toY,
                ),
                intentExtras = emptyList(),
            )
        }

        override fun cleanUpIds() {
            id = 0
            eventId = 0
        }

        override fun deepCopy(): Swipe = copy(name = "" + name)
    }

    /**
     * Pause action.
     *
     * @param id the unique identifier for the action. Use 0 for creating a new action. Default value is 0.
     * @param eventId the identifier of the event for this action.
     * @param name the name of the action.
     * @param pauseDuration the duration of the pause in milliseconds.
     */
    data class Pause(
        override var id: Long = 0,
        override var eventId: Long,
        override var name: String? = null,
        var pauseDuration: Long? = null,
    ) : Action() {

        override fun isComplete(): Boolean = super.isComplete() && pauseDuration != null

        override fun toEntity(): CompleteActionEntity {
            if (!isComplete()) throw IllegalStateException("Can't transform to entity, Pause is incomplete.")

            return CompleteActionEntity(
                action = ActionEntity(
                    id = id,
                    eventId = eventId,
                    name = name!!,
                    type = ActionType.PAUSE,
                    pauseDuration = pauseDuration,
                ),
                intentExtras = emptyList(),
            )
        }

        override fun cleanUpIds() {
            id = 0
            eventId = 0
        }

        override fun deepCopy(): Pause = copy(name = "" + name)
    }

    /**
     * Intent action.
     *
     * @param id the unique identifier for the action. Use 0 for creating a new action. Default value is 0.
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
        override var id: Long = 0,
        override var eventId: Long,
        override var name: String? = null,
        var isAdvanced: Boolean? = null,
        var isBroadcast: Boolean? = null,
        var intentAction: String? = null,
        var componentName: ComponentName? = null,
        var flags: Int? = null,
        val extras: MutableList<IntentExtra<out Any>>? = null,
    ) : Action() {

        override fun isComplete(): Boolean =
            super.isComplete() && isAdvanced != null && intentAction != null && flags != null

        override fun toEntity(): CompleteActionEntity {
            if (!isComplete()) throw IllegalStateException("Can't transform to entity, Intent is incomplete.")

            return CompleteActionEntity(
                action = ActionEntity(
                    id = id,
                    eventId = eventId,
                    name = name!!,
                    type = ActionType.INTENT,
                    isAdvanced = isAdvanced,
                    isBroadcast = isBroadcast,
                    intentAction = intentAction,
                    componentName = componentName?.flattenToString(),
                    flags = flags,
                ),
                intentExtras = extras?.map { it.toEntity() } ?: emptyList(),
            )
        }

        override fun cleanUpIds() {
            id = 0
            eventId = 0
            extras?.forEach { it.cleanUpIds() }
        }

        override fun deepCopy(): Intent = copy(name = "" + name)
    }

    /**
     * Toggle Event Action.
     *
     * @param id the unique identifier for the action. Use 0 for creating a new action. Default value is 0.
     * @param eventId the identifier of the event for this action.
     * @param name the name of the action.
     * @param toggleEventId the identifier of the event to manipulate.
     * @param toggleEventType the type of manipulation to apply.
     */
    data class ToggleEvent(
        override var id: Long = 0,
        override var eventId: Long,
        override var name: String? = null,
        var toggleEventId: Long? = null,
        var toggleEventType: ToggleType? = null,
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

        override fun toEntity(): CompleteActionEntity {
            if (!isComplete()) throw IllegalStateException("Can't transform to entity, ToggleEvent is incomplete.")

            return CompleteActionEntity(
                action = ActionEntity(
                    id = id,
                    eventId = eventId,
                    name = name!!,
                    type = ActionType.TOGGLE_EVENT,
                    toggleEventId = toggleEventId!!,
                    toggleEventType = toggleEventType!!.toEntity(),
                ),
                intentExtras = emptyList(),
            )
        }

        override fun cleanUpIds() {
            id = 0
            eventId = 0
        }

        override fun deepCopy(): ToggleEvent = copy(name = "" + name)

        fun deepCopyWithToggleEventCleanup(): ToggleEvent = copy(name = "" + name, toggleEventId = null)
    }
}

/** Creates a deep copy of the action and reset its id. */
fun Action.copyAndResetId(): Action = when (this) {
    is Action.Click -> copy(id = 0, name = "" + name)
    is Action.Swipe -> copy(id = 0, name = "" + name)
    is Action.Pause -> copy(id = 0, name = "" + name)
    is Action.Intent -> copy(id = 0, name = "" + name)
    is Action.ToggleEvent -> copy(id = 0, name = "" + name)
}

fun ToggleEventType.toDomain(): Action.ToggleEvent.ToggleType = Action.ToggleEvent.ToggleType.valueOf(name)

/** Convert an Action entity into a Domain Action. */
internal fun CompleteActionEntity.toAction(): Action {
    return when (action.type) {
        ActionType.CLICK -> Action.Click(
            action.id, action.eventId, action.name, action.pressDuration!!,
            action.x, action.y, action.clickOnCondition!!
        )

        ActionType.SWIPE -> Action.Swipe(
            action.id, action.eventId, action.name, action.swipeDuration!!,
            action.fromX!!, action.fromY!!, action.toX!!, action.toY!!
        )

        ActionType.PAUSE -> Action.Pause(
            action.id,
            action.eventId,
            action.name,
            action.pauseDuration!!
        )

        ActionType.INTENT -> {
            val componentName = action.componentName?.let { ComponentName.unflattenFromString(it) }
            Action.Intent(action.id,
                action.eventId,
                action.name,
                action.isAdvanced,
                action.isBroadcast,
                action.intentAction,
                componentName,
                action.flags,
                intentExtras.map { it.toIntentExtra() }.toMutableList()
            )
        }

        ActionType.TOGGLE_EVENT -> Action.ToggleEvent(
            action.id,
            action.eventId,
            action.name,
            action.toggleEventId,
            action.toggleEventType?.toDomain(),
        )
    }
}

/** The maximum supported duration for a gesture. This limitation comes from Android GestureStroke API.  */
const val GESTURE_DURATION_MAX_VALUE = 59_999L