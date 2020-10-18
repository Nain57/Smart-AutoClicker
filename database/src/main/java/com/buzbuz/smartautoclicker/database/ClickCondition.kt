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

import android.graphics.Bitmap
import android.graphics.Rect

import com.buzbuz.smartautoclicker.database.room.ConditionEntity

/**
 * Object defining a click condition for a click.
 *
 * This is basically a holder of the data of click condition in database. It handles to conversion between the simple
 * types that a database can contains into more complex types easier to use in the application code.
 *
 * @param area the area of the screen to detect.
 * @param path the path to the bitmap that should be matched for detection.
 */
data class ClickCondition internal constructor(
    var area: Rect,
    var path: String,
    var bitmap: Bitmap? = null
) {

    /**
     * Instantiates a new condition.
     * For convenience use for a new condition that has not been saved yet. Do no insert it in the database without
     * setting the path first.
     *
     * @param area the area of the screen to detect.
     * @param bitmap the image that should be matched for detection.
     */
    constructor(area: Rect, bitmap: Bitmap): this(area, "", bitmap)

    companion object {

        /**
         * Convert a list of [ConditionEntity] into a list of [ClickCondition].
         *
         * @param conditionEntities the conditions to be converted.
         *
         * @return the list of corresponding conditions.
         */
        internal fun fromEntities(conditionEntities: List<ConditionEntity>) : List<ClickCondition> =
            conditionEntities.map {
                ClickCondition(
                    Rect(it.areaLeft, it.areaTop, it.areaRight, it.areaBottom),
                    it.path
                )
            }

        /**
         * Convert a list of [ClickCondition] into a list of [ConditionEntity]
         *
         * @param conditions the conditions to be converted.
         *
         * @return the list of corresponding entities.
         */
        internal fun toEntities(conditions: List<ClickCondition>) : List<ConditionEntity> = conditions.map { it.toEntity() }
    }

    /**
     * Convert this click condition into a [ConditionEntity].
     *
     * @return the entity representing this condition.
     */
    private fun toEntity() : ConditionEntity =
        ConditionEntity(
            path,
            area.left,
            area.top,
            area.right,
            area.bottom,
            area.width(),
            area.height()
        )
}