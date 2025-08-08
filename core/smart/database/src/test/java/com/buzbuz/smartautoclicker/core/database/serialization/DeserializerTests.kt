
package com.buzbuz.smartautoclicker.core.database.serialization

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.database.CLICK_DATABASE_VERSION
import com.buzbuz.smartautoclicker.core.database.entity.*
import com.buzbuz.smartautoclicker.core.database.utils.encodeToJsonObject

import kotlinx.serialization.json.*

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Test the [Deserializer] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class DeserializerTests {

    private companion object {

        private val DEFAULT_COMPLETE_SCENARIO = CompleteScenario(
            scenario = ScenarioEntity(1, "Scenario", 600, false),
            events = listOf(
                CompleteEventEntity(
                    event = EventEntity(1, 1, "Event", 1, 0, true, EventType.IMAGE_EVENT),
                    conditions = listOf(
                        ConditionEntity(1, 1, "Condition", ConditionType.ON_IMAGE_DETECTED, 0, "/toto/tutu", 1, 2, 3, 4, 5, 1, true)
                    ),
                    actions = listOf(
                        CompleteActionEntity(
                            action = ActionEntity(1, 1, 0, "Intent", ActionType.INTENT,
                                isAdvanced = false, isBroadcast = false, intentAction = "org.action", flags = 0,
                                componentName = "org.action/org.action.TOTO",
                            ),
                            intentExtras = listOf(
                                IntentExtraEntity(1, 1, IntentExtraType.BOOLEAN, "ExtraKey", "true")
                            ),
                            eventsToggle = listOf(
                                EventToggleEntity(1, 1, EventToggleType.DISABLE, 1)
                            ),
                        )
                    )
                )
            ),
        )
    }

    @Test
    fun deserialization_invalidVersion_lower() {
        assertNull(DeserializerFactory.create(7))
    }

    @Test
    fun deserialization_invalidVersion_higher() {
        assertNull(DeserializerFactory.create(CLICK_DATABASE_VERSION + 1))
    }

    @Test
    fun deserialization_sameVersion() {
        // Given
        val jsonScenario = DEFAULT_COMPLETE_SCENARIO.encodeToJsonObject()

        // When
        val deserializedScenario = DeserializerFactory.create(CLICK_DATABASE_VERSION)
            ?.deserializeCompleteScenario(jsonScenario)

        // Then
        assertEquals(DEFAULT_COMPLETE_SCENARIO, deserializedScenario)
    }
}