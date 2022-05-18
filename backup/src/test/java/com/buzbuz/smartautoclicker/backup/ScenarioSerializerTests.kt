/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.backup

import android.graphics.Point
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buzbuz.smartautoclicker.database.room.CLICK_DATABASE_VERSION

import com.buzbuz.smartautoclicker.database.room.entity.*

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.junit.Assert.assertEquals

import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Test the [ScenarioSerializer] class. */
@OptIn(ExperimentalSerializationApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioSerializerTests {

    companion object {
        val DEFAULT_SCREEN_SIZE = Point(800,600)

        val DEFAULT_COMPLETE_SCENARIO = CompleteScenario(
            scenario = ScenarioEntity(1, "Scenario", 600, 1),
            events = listOf(
                CompleteEventEntity(
                    event = EventEntity(1, 1, "Event", 1, 0, null),
                    conditions = listOf(
                        ConditionEntity(1, 1, "Condition", "/toto/tutu", 1, 2, 3, 4, 5, 1, true)
                    ),
                    actions = listOf(
                        CompleteActionEntity(
                            action = ActionEntity(1, 1, 0, "Intent", ActionType.INTENT,
                                isAdvanced = false, isBroadcast = false, intentAction = "org.action", flags = 0,
                                componentName = "org.action/org.action.TOTO",
                            ),
                            intentExtras = listOf(
                                IntentExtraEntity(1, 1, IntentExtraType.BOOLEAN, "ExtraKey", "true")
                            )
                        )
                    )
                )
            ),
            endConditions = listOf(
                EndConditionEntity(1, 1, 1, 20)
            )
        )
    }

    @Test
    fun serialization_invalidVersion() {
        val backup = ScenarioBackup(
            version = 0,
            scenario = DEFAULT_COMPLETE_SCENARIO,
            screenWidth = DEFAULT_SCREEN_SIZE.x,
            screenHeight = DEFAULT_SCREEN_SIZE.y,
        )

        ScenarioSerializer().apply {
            val outputStream = ByteArrayOutputStream()
            Json.encodeToStream(backup, outputStream)

            assertNull(deserialize(ByteArrayInputStream(outputStream.toByteArray())))
        }
    }

    @Test
    fun serialization_sameVersion() {
        val backup = ScenarioBackup(
            version = CLICK_DATABASE_VERSION,
            scenario = DEFAULT_COMPLETE_SCENARIO,
            screenWidth = DEFAULT_SCREEN_SIZE.x,
            screenHeight = DEFAULT_SCREEN_SIZE.y,
        )

        ScenarioSerializer().apply {
            val outputStream = ByteArrayOutputStream()
            Json.encodeToStream(backup, outputStream)

            assertEquals(backup, deserialize(ByteArrayInputStream(outputStream.toByteArray())))
        }
    }

    @Test
    fun serialization_otherValidVersion() {
        val backup = ScenarioBackup(
            version = 19,
            scenario = DEFAULT_COMPLETE_SCENARIO,
            screenWidth = DEFAULT_SCREEN_SIZE.x,
            screenHeight = DEFAULT_SCREEN_SIZE.y,
        )

        ScenarioSerializer().apply {
            val outputStream = ByteArrayOutputStream()
            Json.encodeToStream(backup, outputStream)

            assertEquals(backup, deserialize(ByteArrayInputStream(outputStream.toByteArray())))
        }
    }
}