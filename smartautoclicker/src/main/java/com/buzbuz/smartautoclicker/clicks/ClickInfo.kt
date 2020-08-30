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
package com.buzbuz.smartautoclicker.clicks

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.IntDef

import com.buzbuz.smartautoclicker.clicks.database.ClickEntity
import com.buzbuz.smartautoclicker.clicks.database.ClickWithConditions
import com.buzbuz.smartautoclicker.clicks.database.ConditionEntity

/**
 * Object defining a click in a click scenario.
 *
 * This is basically a holder of the data of click contained in database. It handles to conversion between the simple
 * types that a database can contains into more complex types easier to use in the application code. It also load/save
 * the bitmap for the click conditions automatically.
 *
 * @param name the name of the click.
 * @param type the type of the click.
 * @param from the position of the click for a [SINGLE] click, or the start position for a [SWIPE].
 * @param to the end position for a [SWIPE]. Will always be null for a [SINGLE] click.
 * @param conditionOperator the operator to apply between the conditions in the [conditionList]
 * @param conditionList the list of conditions to fulfill to execute this click.
 * @param id the identifier of the click. Use 0 to save a new click, as the identifier generation is handled by the
 *           room database.
 * @param delayAfterMs the delay to wait after executing this click before executing another one.
 */
data class ClickInfo(
    var name: String,
    @ClickType var type: Int? = null,
    var from: Point? = null,
    var to: Point? = null,
    @Operator var conditionOperator: Int = AND,
    var conditionList: List<Pair<Rect, Bitmap>> = emptyList(),
    var id: Long = 0,
    var delayAfterMs: Long = 50
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
         * @param bitmapManager the bitmap manager loading the conditions from the persistent memory.
         *
         * @return the list of corresponding click info.
         */
        suspend fun fromEntities(entities: List<ClickWithConditions>?, bitmapManager: BitmapManager) : List<ClickInfo> {
            return entities?.map { entity ->
                ClickInfo(
                    entity.click.name,
                    entity.click.type,
                    Point(entity.click.fromX, entity.click.fromY),
                    Point(entity.click.toX, entity.click.toY),
                    entity.click.conditionOperator,
                    fromConditionEntities(entity.conditions, bitmapManager),
                    entity.click.clickId,
                    entity.click.delayAfter
                )
            } ?: emptyList()
        }

        /**
         * Convert a list of [ConditionEntity] into a Pair of area [Rect] to conditions [Bitmap].
         *
         * @param conditionEntities the conditions to be converted.
         * @param bitmapManager the bitmap manager loading the conditions from the persistent memory.
         *
         * @return the list of corresponding conditions.
         */
        private suspend fun fromConditionEntities(
            conditionEntities: List<ConditionEntity>,
            bitmapManager: BitmapManager
        ) : List<Pair<Rect, Bitmap>> =
            conditionEntities.map {
                Pair(
                    Rect(it.areaLeft, it.areaTop, it.areaRight, it.areaBottom),
                    bitmapManager.loadBitmap(it.path, it.width, it.height)
                )
            }
    }

    /**
     * Convert this click info into a [ClickWithConditions] ready to be inserted into the database.
     *
     * @param scenarioId the scenario containing this click.
     * @param priority the priority of the click within the scenario.
     * @param bitmapManager the bitmap manager saving the conditions to the persistent memory.
     *
     * @return the click, ready to be inserted.
     */
    suspend fun toEntity(scenarioId: Long, priority: Int, bitmapManager: BitmapManager) : ClickWithConditions {
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
            getConditionsEntities(bitmapManager)
        )
    }

    /**
     * Convert the list of click conditions into a list of [ConditionEntity] ready to be inserted into the database.
     *
     * @param bitmapManager the bitmap manager saving the conditions to the persistent memory.
     *
     * @return the list of conditions, ready to be inserted.
     */
    private suspend fun getConditionsEntities(bitmapManager: BitmapManager) : List<ConditionEntity> {
        return conditionList.map { condition ->
            ConditionEntity(
                bitmapManager.saveBitmap(condition.second),
                condition.first.left,
                condition.first.top,
                condition.first.right,
                condition.first.bottom,
                condition.second.width,
                condition.second.height
            ) }
    }
}
