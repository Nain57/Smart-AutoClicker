/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.database.domain

import com.buzbuz.smartautoclicker.database.room.entity.ActionEntity
import com.buzbuz.smartautoclicker.database.room.entity.ActionType

/** Base for for all possible actions for en Event. */
sealed class Action {

    /** @return true if this action is complete and can be transformed into its entity. */
    internal abstract fun isComplete(): Boolean
    /** @return the identifier of this action. */
    abstract fun getIdentifier(): Long
    /** @return the name of this action. */
    abstract fun getActionName(): String?
    /** @param name the name of this action. */
    abstract fun setActionName(name: String)
    /** @return the entity equivalent of this action. */
    internal abstract fun toEntity(): ActionEntity
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
        var id: Long = 0,
        var eventId: Long,
        var name: String? = null,
        var pressDuration: Long? = null,
        var x: Int? = null,
        var y: Int? = null,
    ) : Action() {

        override fun isComplete(): Boolean =
            name != null && pressDuration != null && x != null && y != null

        override fun getIdentifier(): Long = id
        override fun getActionName(): String? = name
        override fun setActionName(name: String) { this.name = name }

        override fun toEntity(): ActionEntity {
            if (!isComplete()) throw IllegalStateException("Can't transform to entity, Click is incomplete.")

            return ActionEntity(
                id = id,
                eventId = eventId,
                name = name!!,
                type = ActionType.CLICK,
                pressDuration = pressDuration,
                x = x,
                y = y,
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
        var id: Long = 0,
        var eventId: Long,
        var name: String? = null,
        var swipeDuration: Long? = null,
        var fromX: Int? = null,
        var fromY: Int? = null,
        var toX: Int? = null,
        var toY: Int? = null,
    ) : Action() {

        override fun isComplete(): Boolean =
            name != null && swipeDuration != null && fromX != null && fromY != null && fromY != null
                    && toX != null && toY != null

        override fun getIdentifier(): Long = id
        override fun getActionName(): String? = name
        override fun setActionName(name: String) { this.name = name }

        override fun toEntity(): ActionEntity {
            if (!isComplete()) throw IllegalStateException("Can't transform to entity, Swipe is incomplete.")

            return ActionEntity(
                id = id,
                eventId = eventId,
                name = name!!,
                type = ActionType.SWIPE,
                swipeDuration = swipeDuration,
                fromX = fromX,
                fromY = fromY,
                toX = toX,
                toY = toY,
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
        var id: Long = 0,
        var eventId: Long,
        var name: String? = null,
        var pauseDuration: Long? = null,
    ) : Action() {

        override fun isComplete(): Boolean =
            name != null && pauseDuration != null

        override fun getIdentifier(): Long = id
        override fun getActionName(): String? = name
        override fun setActionName(name: String) { this.name = name }

        override fun toEntity(): ActionEntity {
            if (!isComplete()) throw IllegalStateException("Can't transform to entity, Swipe is incomplete.")

            return ActionEntity(
                id = id,
                eventId = eventId,
                name = name!!,
                type = ActionType.PAUSE,
                pauseDuration = pauseDuration,
            )
        }

        override fun cleanUpIds() {
            id = 0
            eventId = 0
        }

        override fun deepCopy(): Pause = copy(name = "" + name)
    }
}

/**
 *
 */
internal fun ActionEntity.toAction(): Action {
    return when (type) {
        ActionType.CLICK -> Action.Click(id, eventId, name, pressDuration!!, x!!, y!!)
        ActionType.SWIPE -> Action.Swipe(id, eventId, name, swipeDuration!!, fromX!!, fromY!!, toX!!, toY!!)
        ActionType.PAUSE -> Action.Pause(id, eventId, name, pauseDuration!!)
    }
}