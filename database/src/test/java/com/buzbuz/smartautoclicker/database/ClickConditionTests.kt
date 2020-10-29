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

import android.graphics.Rect
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.database.room.ConditionEntity
import com.buzbuz.smartautoclicker.database.utils.TestsData

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [ClickCondition] class. This tests the conversion between database type and api types. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ClickConditionTests {

    @Test
    fun fromEntitiesEmpty() {
        assertEquals(emptyList<ClickCondition>(), ClickCondition.fromEntities(emptyList()))
    }

    @Test
    fun fromEntities() {
        val expected = listOf(
            ClickCondition(Rect(
                TestsData.CONDITION_LEFT, TestsData.CONDITION_TOP, TestsData.CONDITION_RIGHT,
                TestsData.CONDITION_BOTTOM), TestsData.CONDITION_PATH),
            ClickCondition(Rect(
                TestsData.CONDITION_LEFT_2, TestsData.CONDITION_TOP_2, TestsData.CONDITION_RIGHT_2,
                TestsData.CONDITION_BOTTOM_2), TestsData.CONDITION_PATH_2)
        )
        val entities = listOf(TestsData.CONDITION_ENTITY, TestsData.CONDITION_ENTITY_2)

        assertEquals(expected, ClickCondition.fromEntities(entities))
    }

    @Test
    fun toEntitiesEmpty() {
        assertEquals(emptyList<ConditionEntity>(), ClickCondition.toEntities(emptyList()))
    }

    @Test
    fun toEntities() {
        val expected = listOf(TestsData.CONDITION_ENTITY, TestsData.CONDITION_ENTITY_2)
        val clickConditions = listOf(
            ClickCondition(Rect(
                TestsData.CONDITION_LEFT, TestsData.CONDITION_TOP, TestsData.CONDITION_RIGHT,
                TestsData.CONDITION_BOTTOM), TestsData.CONDITION_PATH),
            ClickCondition(Rect(
                TestsData.CONDITION_LEFT_2, TestsData.CONDITION_TOP_2, TestsData.CONDITION_RIGHT_2,
                TestsData.CONDITION_BOTTOM_2), TestsData.CONDITION_PATH_2)
        )

        assertEquals(expected, ClickCondition.toEntities(clickConditions))
    }
}