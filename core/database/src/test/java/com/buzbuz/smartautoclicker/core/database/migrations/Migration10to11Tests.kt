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

import android.os.Build

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.ToggleEventType
import com.buzbuz.smartautoclicker.core.database.utils.*

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Migration10to11]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration10to11Tests {

    private companion object {
        private const val TEST_DB = "migration-test"

        private const val OLD_DB_VERSION = 10
        private const val NEW_DB_VERSION = 11

        private const val TEST_EVENT_ID = 2L

        private val TEST_CLICK_ON_POSITION = V10Click(
            id = 1L,
            evtId = TEST_EVENT_ID,
            name = "TOTO",
            x = 1,
            y = 2,
            pressDuration = 500L,
            clickOnCondition = false,
            priority = 1,
        )

        private val TEST_CLICK_ON_CONDITION = V10Click(
            id = 2L,
            evtId = TEST_EVENT_ID,
            name = "TOTO",
            x = null,
            y = null,
            pressDuration = 500L,
            clickOnCondition = true,
            priority = 1,
        )

        private val TEST_SWIPE = ExpectedV10ToV11Swipe(
            id = 3L,
            evtId = TEST_EVENT_ID,
            name = "TOTO",
            fromX = 0,
            fromY = 1,
            toX = 10,
            toY = 11,
            swipeDuration = 500L,
            priority = 1,
        )

        private val TEST_PAUSE = ExpectedV10ToV11Pause(
            id = 4L,
            evtId = TEST_EVENT_ID,
            name = "TOTO",
            pauseDuration = 500L,
            priority = 1,
        )

        private val TEST_INTENT = ExpectedV10ToV11Intent(
            id = 5L,
            evtId = TEST_EVENT_ID,
            name = "TOTO",
            isAdvanced = true,
            isBroadcast = false,
            action = "com.toto",
            componentName = "com.toto/TOTO",
            flags = 17,
            priority = 1,
        )

        private val TEST_TOGGLE_EVENT = ExpectedV10ToV11ToggleEvent(
            id = 6L,
            evtId = TEST_EVENT_ID,
            name = "TOTO",
            toggleEventId = 12L,
            toggleType = ToggleEventType.TOGGLE,
            priority = 1,
        )
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    private fun SupportSQLiteDatabase.insertTestCondition(id: Long, shouldBeDetected: Boolean) {
        execSQL(
            getInsertV10Condition(
                id = id,
                eventId = TEST_EVENT_ID,
                name = "TOTO",
                path = "/toto",
                left = 0, top = 1, right = 2, bottom = 3,
                threshold = 10,
                detectionType = 0,
                shouldBeDetected = shouldBeDetected,
            )
        )
    }

    @Test
    fun migrate_quality() {
        // Given
        val id = 1L
        val quality = 1200

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV10 ->
            dbV10.execSQL(getInsertV10Scenario(id, quality))
        }

        // Migrate
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11).use { dbV11 ->
            dbV11.query(getV11Scenarios()).use { cursor ->

                // Verify
                cursor.assertCountEquals(1)
                cursor.moveToFirst()
                cursor.assertColumnEquals(id, "id")
                cursor.assertColumnEquals(quality + 600, "detection_quality")
            }
        }
    }

    @Test
    fun migrate_swipe() {
        // Given
        val expectedSwipe = TEST_SWIPE

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV10 ->
            dbV10.insertV10Swipe(expectedSwipe)
        }

        // Migrate
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11).use { dbV11 ->
            dbV11.query(getV11Actions()).use { cursor ->

                // Verify
                cursor.assertCountEquals(1)
                cursor.moveToFirst()
                cursor.assertRowIsSwipe(expectedSwipe)
            }
        }
    }

    @Test
    fun migrate_pause() {
        // Given
        val expectedPause = TEST_PAUSE

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV10 ->
            dbV10.insertV10Pause(expectedPause)
        }

        // Migrate
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11).use { dbV11 ->
            dbV11.query(getV11Actions()).use { cursor ->

                // Verify
                cursor.assertCountEquals(1)
                cursor.moveToFirst()
                cursor.assertRowIsPause(expectedPause)
            }
        }
    }

    @Test
    fun migrate_intent() {
        // Given
        val expectedIntent = TEST_INTENT

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV10 ->
            dbV10.insertV10Intent(expectedIntent)
        }

        // Migrate
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11).use { dbV11 ->
            dbV11.query(getV11Actions()).use { cursor ->

                // Verify
                cursor.assertCountEquals(1)
                cursor.moveToFirst()
                cursor.assertRowIsIntent(expectedIntent)
            }
        }
    }

    @Test
    fun migrate_toggle_event() {
        // Given
        val expectedToggleEvent = TEST_TOGGLE_EVENT

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV10 ->
            dbV10.insertV10ToggleEvent(expectedToggleEvent)
        }

        // Migrate
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11).use { dbV11 ->
            dbV11.query(getV11Actions()).use { cursor ->

                // Verify
                cursor.assertCountEquals(1)
                cursor.moveToFirst()
                cursor.assertRowIsToggleEvent(expectedToggleEvent)
            }
        }
    }

    @Test
    fun migrate_click_on_position() {
        // Given
        val v10Click = TEST_CLICK_ON_POSITION

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV10 ->
            dbV10.insertV10Click(v10Click)
        }

        // Migrate
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11).use { dbV11 ->
            dbV11.query(getV11Actions()).use { cursor ->

                // Verify
                cursor.assertCountEquals(1)
                cursor.moveToFirst()
                cursor.assertRowIsV11Click(v10Click.toExpectedV11Click(
                    clickPositionType = ClickPositionType.USER_SELECTED,
                    clickOnConditionId = null,
                ))
            }
        }
    }

    @Test
    fun migrate_click_on_condition_no_conditions() {
        // Given
        val v10Click = TEST_CLICK_ON_CONDITION

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV10 ->
            dbV10.insertV10Click(v10Click)
        }

        // Migrate
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11).use { dbV11 ->
            dbV11.query(getV11Actions()).use { cursor ->

                // Verify
                cursor.assertCountEquals(1)
                cursor.moveToFirst()
                cursor.assertRowIsV11Click(v10Click.toExpectedV11Click(
                    clickPositionType = ClickPositionType.ON_DETECTED_CONDITION,
                    clickOnConditionId = null,
                ))
            }
        }
    }

    @Test
    fun migrate_click_on_condition_invalid_condition() {
        // Given
        val conditionId = 25L
        val shouldBeDetected = false
        val v10Click = TEST_CLICK_ON_CONDITION

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV10 ->
            dbV10.insertV10Click(v10Click)
            dbV10.insertTestCondition(conditionId, shouldBeDetected)
        }

        // Migrate
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11).use { dbV11 ->
            dbV11.query(getV11Actions()).use { cursor ->

                // Verify
                cursor.assertCountEquals(1)
                cursor.moveToFirst()
                cursor.assertRowIsV11Click(v10Click.toExpectedV11Click(
                    clickPositionType = ClickPositionType.ON_DETECTED_CONDITION,
                    clickOnConditionId = null,
                ))
            }
        }
    }

    @Test
    fun migrate_click_on_condition_valid_condition() {
        // Given
        val conditionId = 25L
        val shouldBeDetected = true
        val v10Click = TEST_CLICK_ON_CONDITION

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV10 ->
            dbV10.insertV10Click(v10Click)
            dbV10.insertTestCondition(conditionId, shouldBeDetected)
        }

        // Migrate
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11).use { dbV11 ->
            dbV11.query(getV11Actions()).use { cursor ->

                // Verify
                cursor.assertCountEquals(1)
                cursor.moveToFirst()
                cursor.assertRowIsV11Click(v10Click.toExpectedV11Click(
                    clickPositionType = ClickPositionType.ON_DETECTED_CONDITION,
                    clickOnConditionId = conditionId,
                ))
            }
        }
    }

    @Test
    fun migrate_click_mixed_use_cases() {
        // Given
        val condition1Id = 25L
        val condition1ShouldBeDetected = false
        val condition2Id = 31L
        val condition2ShouldBeDetected = true
        val condition3Id = 42L
        val condition3ShouldBeDetected = true
        val v10ClickOnPosition = TEST_CLICK_ON_POSITION
        val v10ClickOnCondition = TEST_CLICK_ON_CONDITION

        // Insert in V10 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV10 ->
            dbV10.insertV10Click(v10ClickOnPosition)
            dbV10.insertV10Click(v10ClickOnCondition)
            dbV10.insertTestCondition(condition1Id, condition1ShouldBeDetected)
            dbV10.insertTestCondition(condition2Id, condition2ShouldBeDetected)
            dbV10.insertTestCondition(condition3Id, condition3ShouldBeDetected)
        }

        // Migrate
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration10to11).use { dbV11 ->
            dbV11.query(getV11Actions()).use { cursor ->

                // Verify
                cursor.assertCountEquals(2)

                cursor.moveToFirst()
                cursor.assertRowIsV11Click(v10ClickOnPosition.toExpectedV11Click(
                    clickPositionType = ClickPositionType.USER_SELECTED,
                    clickOnConditionId = null,
                ))

                cursor.moveToNext()
                cursor.assertRowIsV11Click(v10ClickOnCondition.toExpectedV11Click(
                    clickPositionType = ClickPositionType.ON_DETECTED_CONDITION,
                    clickOnConditionId = condition2Id,
                ))
            }
        }
    }
}
