/*
 * Copyright (C) 2026 Kevin Buzeau
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action

import android.os.Build

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.actions.text.appendCounterReference
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification.NotificationViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.settext.SetTextViewModel
import com.buzbuz.smartautoclicker.feature.smart.config.ui.createEditionRepository

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ActionTextViewModelTests {

    @Test
    fun setTextCounterReference_isWrittenToTheEditedAction() = runTest {
        val scenario = Scenario(Identifier(databaseId = 1L), "Scenario", detectionQuality = 600)
        val event = ScreenEvent(Identifier(databaseId = 2L), scenario.id, "Event", AND, priority = 0, keepDetecting = false, cooldownMs = 0)
        val action = SetText(Identifier(databaseId = 3L), event.id, priority = 0, text = "Hello ", validateInput = false)
        val editionRepository = createEditionRepository(scenario, screenEvents = listOf(event))
        editionRepository.startEventEdition(event)
        editionRepository.startActionEdition(action)

        SetTextViewModel(editionRepository).appendCounterReferenceToTextToWrite("score")

        assertEquals("Hello ".appendCounterReference("score"), editionRepository.editionState.getEditedAction<SetText>()?.text)
    }

    @Test
    fun notificationCounterReference_isWrittenToTheEditedAction() = runTest {
        val scenario = Scenario(Identifier(databaseId = 1L), "Scenario", detectionQuality = 600)
        val event = ScreenEvent(Identifier(databaseId = 2L), scenario.id, "Event", AND, priority = 0, keepDetecting = false, cooldownMs = 0)
        val action = Notification(Identifier(databaseId = 3L), event.id, priority = 0, messageText = "Done: ", channelImportance = 3)
        val editionRepository = createEditionRepository(scenario, screenEvents = listOf(event))
        editionRepository.startEventEdition(event)
        editionRepository.startActionEdition(action)

        NotificationViewModel(editionRepository).setNotificationMessage("Done: ".appendCounterReference("score"))

        assertEquals("Done: ".appendCounterReference("score"), editionRepository.editionState.getEditedAction<Notification>()?.messageText)
    }
}
