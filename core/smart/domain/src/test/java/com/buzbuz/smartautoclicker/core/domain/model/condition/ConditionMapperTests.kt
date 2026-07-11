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
package com.buzbuz.smartautoclicker.core.domain.model.condition

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.database.entity.NumberFormatType as DbNumberFormatType

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ConditionMapperTests {

    @Test
    fun imageCondition_toEntity() {
        assertEquals(
            ConditionTestsData.getNewImageConditionEntity(eventId = ConditionTestsData.CONDITION_EVENT_ID),
            ConditionTestsData.getNewImageCondition(eventId = ConditionTestsData.CONDITION_EVENT_ID).toEntity()
        )
    }

    @Test
    fun imageCondition_toDomain() {
        assertEquals(
            ConditionTestsData.getNewImageCondition(eventId = ConditionTestsData.CONDITION_EVENT_ID),
            ConditionTestsData.getNewImageConditionEntity(eventId = ConditionTestsData.CONDITION_EVENT_ID).toDomain()
        )
    }

    @Test
    fun triggerCondition_onBroadcastReceived_toEntity() {
        assertEquals(
            ConditionTestsData.getNewBroadcastReceivedConditionEntity(eventId = ConditionTestsData.CONDITION_EVENT_ID),
            ConditionTestsData.getNewBroadcastReceivedCondition(eventId = ConditionTestsData.CONDITION_EVENT_ID).toEntity()
        )
    }

    @Test
    fun triggerCondition_onBroadcastReceived_toDomain() {
        assertEquals(
            ConditionTestsData.getNewBroadcastReceivedCondition(eventId = ConditionTestsData.CONDITION_EVENT_ID),
            ConditionTestsData.getNewBroadcastReceivedConditionEntity(eventId = ConditionTestsData.CONDITION_EVENT_ID).toDomain()
        )
    }

    @Test
    fun triggerCondition_onCounterReached_toEntity() {
        assertEquals(
            ConditionTestsData.getNewCounterReachedConditionEntity(eventId = ConditionTestsData.CONDITION_EVENT_ID),
            ConditionTestsData.getNewCounterReachedCondition(eventId = ConditionTestsData.CONDITION_EVENT_ID).toEntity()
        )
    }

    @Test
    fun triggerCondition_onCounterReached_toDomain() {
        assertEquals(
            ConditionTestsData.getNewCounterReachedCondition(eventId = ConditionTestsData.CONDITION_EVENT_ID),
            ConditionTestsData.getNewCounterReachedConditionEntity(eventId = ConditionTestsData.CONDITION_EVENT_ID).toDomain()
        )
    }

    @Test
    fun triggerCondition_onTimerReached_toEntity() {
        assertEquals(
            ConditionTestsData.getNewTimerReachedConditionEntity(eventId = ConditionTestsData.CONDITION_EVENT_ID),
            ConditionTestsData.getNewTimerReachedCondition(eventId = ConditionTestsData.CONDITION_EVENT_ID).toEntity()
        )
    }

    @Test
    fun triggerCondition_onTimerReached_toDomain() {
        assertEquals(
            ConditionTestsData.getNewTimerReachedCondition(eventId = ConditionTestsData.CONDITION_EVENT_ID),
            ConditionTestsData.getNewTimerReachedConditionEntity(eventId = ConditionTestsData.CONDITION_EVENT_ID).toDomain()
        )
    }

    @Test
    fun numberCondition_toEntity_formatAuto() {
        assertEquals(
            ConditionTestsData.getNewNumberConditionEntity(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = DbNumberFormatType.AUTO,
            ),
            ConditionTestsData.getNewNumberCondition(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = NumberFormatType.AUTO,
            ).toEntity()
        )
    }

    @Test
    fun numberCondition_toEntity_formatDotDecimal() {
        assertEquals(
            ConditionTestsData.getNewNumberConditionEntity(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = DbNumberFormatType.DOT_DECIMAL,
            ),
            ConditionTestsData.getNewNumberCondition(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = NumberFormatType.DOT_DECIMAL,
            ).toEntity()
        )
    }

    @Test
    fun numberCondition_toEntity_formatCommaDecimal() {
        assertEquals(
            ConditionTestsData.getNewNumberConditionEntity(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = DbNumberFormatType.COMMA_DECIMAL,
            ),
            ConditionTestsData.getNewNumberCondition(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = NumberFormatType.COMMA_DECIMAL,
            ).toEntity()
        )
    }

    @Test
    fun numberCondition_toDomain_formatAuto() {
        assertEquals(
            ConditionTestsData.getNewNumberCondition(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = NumberFormatType.AUTO,
            ),
            ConditionTestsData.getNewNumberConditionEntity(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = DbNumberFormatType.AUTO,
            ).toDomain()
        )
    }

    @Test
    fun numberCondition_toDomain_formatNullFallsBackToAuto() {
        assertEquals(
            ConditionTestsData.getNewNumberCondition(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = NumberFormatType.AUTO,
            ),
            ConditionTestsData.getNewNumberConditionEntity(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = null,
            ).toDomain()
        )
    }

    @Test
    fun numberCondition_toDomain_formatDotDecimal() {
        assertEquals(
            ConditionTestsData.getNewNumberCondition(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = NumberFormatType.DOT_DECIMAL,
            ),
            ConditionTestsData.getNewNumberConditionEntity(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = DbNumberFormatType.DOT_DECIMAL,
            ).toDomain()
        )
    }

    @Test
    fun numberCondition_toDomain_formatCommaDecimal() {
        assertEquals(
            ConditionTestsData.getNewNumberCondition(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = NumberFormatType.COMMA_DECIMAL,
            ),
            ConditionTestsData.getNewNumberConditionEntity(
                eventId = ConditionTestsData.CONDITION_EVENT_ID,
                numberFormatType = DbNumberFormatType.COMMA_DECIMAL,
            ).toDomain()
        )
    }
}