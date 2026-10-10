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
package com.buzbuz.smartautoclicker.core.database.serialization

import android.os.Build
import android.util.Log

import com.buzbuz.smartautoclicker.core.database.DATABASE_VERSION
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.GestureFingerMotionType
import com.buzbuz.smartautoclicker.core.database.serialization.compat.CompatDeserializer
import com.buzbuz.smartautoclicker.core.database.serialization.compat.CompatV11Deserializer
import com.buzbuz.smartautoclicker.core.database.serialization.compat.CompatV13Deserializer
import com.buzbuz.smartautoclicker.core.database.serialization.compat.CompatV20Deserializer
import com.buzbuz.smartautoclicker.core.database.serialization.compat.CompatV24Deserializer

import kotlinx.serialization.json.JsonObject


object DeserializerFactory {

    fun create(databaseVersion: Int): Deserializer? =
        when {
            databaseVersion < VERSION_MINIMUM -> {
                Log.w(TAG, "Json object version not supported, minimum=$VERSION_MINIMUM, actual=$databaseVersion")
                null
            }

            databaseVersion < VERSION_DETECTION_QUALITY_UPDATE -> CompatV11Deserializer()
            databaseVersion < VERSION_ADVANCED_AUTOMATION_UPDATE -> CompatV13Deserializer()
            databaseVersion < VERSION_COUNTER_TYPE_UPDATE -> CompatV20Deserializer()
            databaseVersion < VERSION_GESTURE_UPDATE -> CompatV24Deserializer()
            databaseVersion < VERSION_UP_TO_DATE -> CompatDeserializer()
            databaseVersion == VERSION_UP_TO_DATE -> KotlinDeserializer()

            else -> {
                Log.w(TAG, "Json object version not supported, maximum=$VERSION_UP_TO_DATE, actual=$databaseVersion")
                null
            }
        }

    /** Maximum json object version supported. */
    private const val VERSION_UP_TO_DATE = DATABASE_VERSION
    /** Multitouch gesture version. */
    private const val VERSION_GESTURE_UPDATE = 24
    /** Number & Text detection update. */
    private const val VERSION_COUNTER_TYPE_UPDATE = 20
    /** Trigger events update version. */
    private const val VERSION_ADVANCED_AUTOMATION_UPDATE = 13
    /** Scenario detection quality revision update version. */
    private const val VERSION_DETECTION_QUALITY_UPDATE = 11
    /** Minimal supported json object version. */
    private const val VERSION_MINIMUM = 8

    /** Tag for logs. */
    private const val TAG = "ScenarioDeserializerFactory"
}

abstract class Deserializer {

    /** Deserialize a complete scenario, and remove from it everything the device can't execute. */
    fun deserializeCompleteScenario(jsonCompleteScenario: JsonObject): CompleteScenario {
        val completeScenario = deserialize(jsonCompleteScenario)

        // Holding a finger still requires stroke continuation, only available from Android O.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "Removing all gesture finger wait motions, not supported on this Android version.")
            return completeScenario.withoutGestureWaitMotions()
        }

        return completeScenario
    }

    /** Deserialize a complete scenario from a json of the versions handled by this deserializer. */
    protected abstract fun deserialize(jsonCompleteScenario: JsonObject): CompleteScenario

    private companion object {
        /** Tag for logs. */
        const val TAG = "Deserializer"
    }
}

private fun CompleteScenario.withoutGestureWaitMotions(): CompleteScenario =
    copy(
        events = events.map { completeEvent ->
            completeEvent.copy(
                actions = completeEvent.actions.map { completeAction ->
                    // A click wait motion is only there to hold its press duration, it is never continuing a stroke
                    if (completeAction.action.type == ActionType.CLICK) return@map completeAction

                    completeAction.copy(
                        gestureFingers = completeAction.gestureFingers.map { fingerWithMotions ->
                            fingerWithMotions.copy(
                                motions = fingerWithMotions.motions.filter { motion ->
                                    motion.motionType != GestureFingerMotionType.WAIT
                                },
                            )
                        },
                    )
                },
            )
        },
    )