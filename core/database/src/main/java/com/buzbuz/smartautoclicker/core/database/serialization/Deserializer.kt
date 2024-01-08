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

import android.util.Log

import com.buzbuz.smartautoclicker.core.database.CLICK_DATABASE_VERSION
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.serialization.compat.CompatV11Deserializer
import com.buzbuz.smartautoclicker.core.database.serialization.compat.CompatV13Deserializer
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
            databaseVersion == VERSION_UP_TO_DATE  -> KotlinDeserializer()

            else -> {
                Log.w(TAG, "Json object version not supported, maximum=$VERSION_UP_TO_DATE, actual=$databaseVersion")
                null
            }
        }

    /** Maximum json object version supported. */
    private const val VERSION_UP_TO_DATE = CLICK_DATABASE_VERSION
    /** Trigger events update version. */
    private const val VERSION_ADVANCED_AUTOMATION_UPDATE = 13
    /** Scenario detection quality revision update version. */
    private const val VERSION_DETECTION_QUALITY_UPDATE = 11
    /** Minimal supported json object version. */
    private const val VERSION_MINIMUM = 8

    /** Tag for logs. */
    private const val TAG = "ScenarioDeserializerFactory"
}

interface Deserializer {
    fun deserializeCompleteScenario(jsonCompleteScenario: JsonObject): CompleteScenario
}