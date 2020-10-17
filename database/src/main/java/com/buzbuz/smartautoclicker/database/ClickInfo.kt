/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.database

import android.graphics.Point

import androidx.annotation.IntDef

import com.buzbuz.smartautoclicker.database.room.ClickEntity
import com.buzbuz.smartautoclicker.database.room.ClickWithConditions

/**
 * Object defining a click in a click scenario.
 *
 * This is basically a holder of the data of click contained in database. It handles to conversion between the simple
 * types that a database can contains into more complex types easier to use in the application code.
 *
 * @param name the name of the click.
 * @param type the type of the click.
 * @param from the position of the click for a [SINGLE] click, or the start position for a [SWIPE].
 * @param to the end position for a [SWIPE]. Will always be null for a [SINGLE] click.
 * @param conditionOperator the operator to apply between the conditions in the [conditionList]
 * @param conditionList the list of conditions to fulfill to execute this click.
 * @param scenarioId the identifier of the scenario.
 * @param id the identifier of the click. Use 0 to save a new click, as the identifier generation is handled by the
 *           room database.
 * @param delayAfterMs the delay to wait after executing this click before executing another one.
 * @param priority the priority of the click in the scenario.
 */
data class ClickInfo(
    var name: String,
    var scenarioId: Long = 0L,
    @ClickType var type: Int? = null,
    var from: Point? = null,
    var to: Point? = null,
    @Operator var conditionOperator: Int = AND,
    var conditionList: List<ClickCondition> = emptyList(),
    var id: Long = 0L,
    var delayAfterMs: Long = 50,
    var priority: Int = 0
) {

    companion object {

        /** Defines the different types of clicks. */
        @IntDef(SINGLE, SWIPE)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ClickType
        /** This click is a single click. */
        const val SINGLE = 1
        /** This click is a swipe. */
        const val SWIPE = 2

        /** Defines the operators to be applied between the click conditions. */
        @IntDef(AND, OR)
        @Retention(AnnotationRetention.SOURCE)
        annotation class Operator
        /** All conditions must be fulfilled to execute the click. */
        const val AND = 1
        /** Only one of the conditions must be fulfilled to execute the click. */
        const val OR = 2

        /** Default value for an undefined position. */
        const val NO_POSITION = -1

        /**
         * Convert a list of [ClickEntity] into a list of [ClickInfo].
         *
         * @param entities the clicks to be converted.
         *
         * @return the list of corresponding click info.
         */
        internal fun fromEntities(entities: List<ClickWithConditions>?) : List<ClickInfo> {
            return entities?.map { entity ->
                ClickInfo(
                    entity.click.name,
                    entity.click.scenarioId,
                    entity.click.type,
                    Point(entity.click.fromX, entity.click.fromY),
                    Point(entity.click.toX, entity.click.toY),
                    entity.click.conditionOperator,
                    ClickCondition.fromEntities(entity.conditions),
                    entity.click.clickId,
                    entity.click.delayAfter,
                    entity.click.priority
                )
            } ?: emptyList()
        }
    }

    /**
     * Convert this click info into a [ClickWithConditions] ready to be inserted into the database.
     *
     * @return the click, ready to be inserted.
     */
    internal fun toEntity() : ClickWithConditions {
        val toXPos: Int
        val toYPos: Int
        if (type == SINGLE) {
            toXPos = NO_POSITION
            toYPos = NO_POSITION
        } else {
            toXPos = to!!.x
            toYPos = to!!.y
        }

        return ClickWithConditions(
            ClickEntity(
                id,
                scenarioId,
                name,
                type!!,
                from!!.x,
                from!!.y,
                toXPos,
                toYPos,
                conditionOperator,
                delayAfterMs,
                priority
            ),
            ClickCondition.toEntities(conditionList)
        )
    }

    /**
     * Creates an exact copy of this click and reset its id.
     * Useful for creating a click from another.
     *
     * @return the copy.
     */
    fun copyWithoutId() = copy().apply { id = 0 }
}
