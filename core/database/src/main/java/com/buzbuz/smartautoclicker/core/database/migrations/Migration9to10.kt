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
package com.buzbuz.smartautoclicker.core.database.migrations

import android.util.Log

import androidx.annotation.VisibleForTesting
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.database.entity.ActionType

import kotlin.math.min

/**
 * Migration from database v9 to v10.
 *
 * This migration has no changes in the database table. It is made to fix a bug that crash the application if a gesture
 * duration is over 1 minute.
 * To fix this crash, we need to add this limitation in the ui, in the scenario import and then ensure all gestures
 * already in database are limited to 1 minute.
 */
object Migration9to10 : Migration(9, 10) {

    /** Tag for logs. */
    private const val TAG = "Migration9to10"

    /** The new limit for the gesture duration in milliseconds. */
    @VisibleForTesting
    internal const val NEW_GESTURE_DURATION_LIMIT_MS = 59999L

    override fun migrate(db: SupportSQLiteDatabase) {
        db.updateGesturesValues()
    }

    private fun SupportSQLiteDatabase.updateGesturesValues() {
        query(getAllClicksAndSwipes).use { cursor ->
            // Nothing to do ?
            if (cursor.count == 0) return
            cursor.moveToFirst()

            // Get all columns indexes
            val idColumnIndex = cursor.getColumnIndex("id")
            val typeIndex = cursor.getColumnIndex("type")
            val clickDurationIndex = cursor.getColumnIndex("pressDuration")
            val swipeDurationIndex = cursor.getColumnIndex("swipeDuration")
            if (idColumnIndex< 0 || typeIndex < 0 || clickDurationIndex < 0 || swipeDurationIndex < 0)
                throw IllegalStateException("Can't find columns")

            do {
                try {
                    // Update the correct value depending on the type of action
                    when (ActionType.valueOf(cursor.getString(typeIndex))) {
                        ActionType.CLICK -> execSQL(
                            updateClickPressDuration(
                                actionId = cursor.getLong(idColumnIndex),
                                duration = cursor.getLong(clickDurationIndex).toLimitedDuration(),
                            )
                        )

                        ActionType.SWIPE -> execSQL(
                            updateSwipeDuration(
                                actionId = cursor.getLong(idColumnIndex),
                                duration = cursor.getLong(swipeDurationIndex).toLimitedDuration(),
                            )
                        )

                        else -> throw IllegalArgumentException("It should be a click or a swipe")
                    }
                } catch (ex: IllegalArgumentException) {
                    Log.e(TAG, "Can't process this item", ex)
                }
            } while (cursor.moveToNext())
        }
    }

    private fun Long.toLimitedDuration(): Long =
        min(this, NEW_GESTURE_DURATION_LIMIT_MS)

    private val getAllClicksAndSwipes = """
        SELECT `id`, `type`, `pressDuration`, `swipeDuration`
        FROM `action_table`
        WHERE `type` = "${ActionType.CLICK}" OR `type` = "${ActionType.SWIPE}"
    """.trimIndent()

    private fun updateClickPressDuration(actionId: Long, duration: Long) = """
        UPDATE `action_table`
        SET `pressDuration` = $duration
        WHERE `id` = $actionId
    """.trimIndent()

    private fun updateSwipeDuration(actionId: Long, duration: Long) = """
        UPDATE `action_table`
        SET `swipeDuration` = $duration
        WHERE `id` = $actionId
    """.trimIndent()
}