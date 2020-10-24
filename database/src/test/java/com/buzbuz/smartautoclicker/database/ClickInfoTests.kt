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
import android.graphics.Rect
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buzbuz.smartautoclicker.database.room.ClickWithConditions

import org.junit.Assert.assertEquals
import org.junit.Test

import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [ClickInfo] class. This tests the conversion between database type and api types. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ClickInfoTests {

    @Test
    fun copyAsNew() {
        val actual = ClickInfo(TestsData.CLICK_NAME, TestsData.CLICK_SCENARIO_ID, TestsData.CLICK_TYPE,
            Point(TestsData.CLICK_FROMX, TestsData.CLICK_FROMY), Point(TestsData.CLICK_TOX, TestsData.CLICK_TOY),
            TestsData.CLICK_CONDITION_OPERATOR, emptyList(), TestsData.CLICK_ID, TestsData.CLICK_DELAY_AFTER,
            TestsData.CLICK_PRIORITY).copyAsNew()

        assertEquals("Invalid name", TestsData.CLICK_NAME, actual.name)
        assertEquals("Invalid scenario", TestsData.CLICK_SCENARIO_ID, actual.scenarioId)
        assertEquals("Invalid type", TestsData.CLICK_TYPE, actual.type)
        assertEquals("Invalid from", Point(TestsData.CLICK_FROMX, TestsData.CLICK_FROMY), actual.from)
        assertEquals("Invalid to", Point(0, 0), actual.to)
        assertEquals("Invalid operator", TestsData.CLICK_CONDITION_OPERATOR, actual.conditionOperator)
        assertEquals("Invalid condition list", emptyList<ClickCondition>(), actual.conditionList)
        assertEquals("Invalid delay after", TestsData.CLICK_DELAY_AFTER, actual.delayAfterMs)
        assertEquals("Invalid priority", TestsData.CLICK_PRIORITY, actual.priority)
        assertEquals("Id should be 0", 0, actual.id)
    }

    @Test
    fun fromEntitiesEmpty() {
        assertEquals(emptyList<ClickInfo>(), ClickInfo.fromEntities(emptyList()))
    }

    @Test
    fun fromEntities() {
        val expected = listOf(
            ClickInfo(TestsData.CLICK_NAME, TestsData.CLICK_SCENARIO_ID, TestsData.CLICK_TYPE,
                Point(TestsData.CLICK_FROMX, TestsData.CLICK_FROMY), Point(TestsData.CLICK_TOX, TestsData.CLICK_TOY),
                TestsData.CLICK_CONDITION_OPERATOR, emptyList(), TestsData.CLICK_ID, TestsData.CLICK_DELAY_AFTER,
                TestsData.CLICK_PRIORITY),
            ClickInfo(TestsData.CLICK_NAME_2, TestsData.CLICK_SCENARIO_ID_2, TestsData.CLICK_TYPE_2,
                Point(TestsData.CLICK_FROMX_2, TestsData.CLICK_FROMY_2),
                Point(TestsData.CLICK_TOX_2, TestsData.CLICK_TOY_2), TestsData.CLICK_CONDITION_OPERATOR_2, emptyList(),
                TestsData.CLICK_ID_2, TestsData.CLICK_DELAY_AFTER_2, TestsData.CLICK_PRIORITY_2)
        )
        val entities = listOf(
            ClickWithConditions(TestsData.CLICK_ENTITY, emptyList()),
            ClickWithConditions(TestsData.CLICK_ENTITY_2, emptyList())
        )

        assertEquals(expected, ClickInfo.fromEntities(entities))
    }

    @Test
    fun fromEntitiesWithConditions() {
        val expectedConditions = listOf(
            ClickCondition(
                Rect(TestsData.CONDITION_LEFT, TestsData.CONDITION_TOP, TestsData.CONDITION_RIGHT,
                TestsData.CONDITION_BOTTOM), TestsData.CONDITION_PATH),
            ClickCondition(
                Rect(TestsData.CONDITION_LEFT_2, TestsData.CONDITION_TOP_2, TestsData.CONDITION_RIGHT_2,
                TestsData.CONDITION_BOTTOM_2), TestsData.CONDITION_PATH_2)
        )
        val expected = listOf(
            ClickInfo(TestsData.CLICK_NAME, TestsData.CLICK_SCENARIO_ID, TestsData.CLICK_TYPE,
                Point(TestsData.CLICK_FROMX, TestsData.CLICK_FROMY), Point(TestsData.CLICK_TOX, TestsData.CLICK_TOY),
                TestsData.CLICK_CONDITION_OPERATOR, expectedConditions, TestsData.CLICK_ID, TestsData.CLICK_DELAY_AFTER,
                TestsData.CLICK_PRIORITY),
            ClickInfo(TestsData.CLICK_NAME_2, TestsData.CLICK_SCENARIO_ID_2, TestsData.CLICK_TYPE_2,
                Point(TestsData.CLICK_FROMX_2, TestsData.CLICK_FROMY_2),
                Point(TestsData.CLICK_TOX_2, TestsData.CLICK_TOY_2), TestsData.CLICK_CONDITION_OPERATOR_2, expectedConditions,
                TestsData.CLICK_ID_2, TestsData.CLICK_DELAY_AFTER_2, TestsData.CLICK_PRIORITY_2)
        )

        val entities = listOf(
            ClickWithConditions(TestsData.CLICK_ENTITY, listOf(TestsData.CONDITION_ENTITY, TestsData.CONDITION_ENTITY_2)),
            ClickWithConditions(TestsData.CLICK_ENTITY_2, listOf(TestsData.CONDITION_ENTITY, TestsData.CONDITION_ENTITY_2))
        )

        assertEquals(expected, ClickInfo.fromEntities(entities))
    }

    @Test
    fun toEntity() {
        val expected = ClickWithConditions(TestsData.CLICK_ENTITY_2, emptyList())
        val clickInfo = ClickInfo(TestsData.CLICK_NAME_2, TestsData.CLICK_SCENARIO_ID_2, TestsData.CLICK_TYPE_2,
            Point(TestsData.CLICK_FROMX_2, TestsData.CLICK_FROMY_2),
            Point(TestsData.CLICK_TOX_2, TestsData.CLICK_TOY_2), TestsData.CLICK_CONDITION_OPERATOR_2, emptyList(),
            TestsData.CLICK_ID_2, TestsData.CLICK_DELAY_AFTER_2, TestsData.CLICK_PRIORITY_2)

        assertEquals(expected, clickInfo.toEntity())
    }

    @Test
    fun toEntityWithConditions() {
        val expected = ClickWithConditions(TestsData.CLICK_ENTITY_2, listOf(TestsData.CONDITION_ENTITY, TestsData.CONDITION_ENTITY_2))

        val clickConditions = listOf(
            ClickCondition(
                Rect(TestsData.CONDITION_LEFT, TestsData.CONDITION_TOP, TestsData.CONDITION_RIGHT,
                    TestsData.CONDITION_BOTTOM), TestsData.CONDITION_PATH),
            ClickCondition(
                Rect(TestsData.CONDITION_LEFT_2, TestsData.CONDITION_TOP_2, TestsData.CONDITION_RIGHT_2,
                    TestsData.CONDITION_BOTTOM_2), TestsData.CONDITION_PATH_2)
        )
        val clickInfo = ClickInfo(TestsData.CLICK_NAME_2, TestsData.CLICK_SCENARIO_ID_2, TestsData.CLICK_TYPE_2,
            Point(TestsData.CLICK_FROMX_2, TestsData.CLICK_FROMY_2),
            Point(TestsData.CLICK_TOX_2, TestsData.CLICK_TOY_2), TestsData.CLICK_CONDITION_OPERATOR_2, clickConditions,
            TestsData.CLICK_ID_2, TestsData.CLICK_DELAY_AFTER_2, TestsData.CLICK_PRIORITY_2)

        assertEquals(expected, clickInfo.toEntity())
    }
}