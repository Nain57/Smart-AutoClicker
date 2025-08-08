
package com.buzbuz.smartautoclicker.core.domain.model.condition

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

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
}