/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.model.condition

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.domain.utils.TestsData

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Condition] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ConditionTests {

    @Test
    fun toEntity() {
        assertEquals(
            TestsData.getNewConditionEntity(eventId = TestsData.EVENT_ID),
            TestsData.getNewCondition(eventId = TestsData.EVENT_ID).toEntity()
        )
    }

    @Test
    fun toDomain() {
        assertEquals(
            TestsData.getNewCondition(eventId = TestsData.EVENT_ID),
            TestsData.getNewConditionEntity(eventId = TestsData.EVENT_ID).toCondition()
        )
    }

    @Test
    fun deepCopy() {
        val condition = TestsData.getNewCondition(eventId = TestsData.EVENT_ID)
        assertEquals(condition, condition.deepCopy())
    }
}