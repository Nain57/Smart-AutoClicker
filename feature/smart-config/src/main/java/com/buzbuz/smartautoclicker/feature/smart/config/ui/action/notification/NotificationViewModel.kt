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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class)
class NotificationViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val configuredNotification = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<Notification>()

    private val editedActionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedActionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    val name: Flow<String?> = configuredNotification
        .map { it.name }
        .take(1)
    val nameError: Flow<Boolean> = configuredNotification.map { it.name?.isEmpty() ?: true }

    val notificationMessage: Flow<UiNotificationMessage> = configuredNotification
        .map { notification ->
            when (notification.messageType) {
                Notification.MessageType.TEXT ->
                    UiNotificationMessage(notification.messageType.toTypeItem(), notification.messageText)
                Notification.MessageType.COUNTER_VALUE ->
                    UiNotificationMessage(notification.messageType.toTypeItem(), notification.messageCounterName)
            }
        }

    val importanceItem: Flow<NotificationImportanceItem> = configuredNotification
        .map { it.channelImportance.toImportanceItem() }

    val isValidAction: Flow<Boolean> = editionRepository.editionState.editedActionState
        .map { it.canBeSaved }

    fun hasUnsavedModifications(): Boolean =
        editedActionHasChanged.value

    fun setName(name: String) {
        updateEditedNotification { old -> old.copy(name = "" + name) }
    }

    fun setNotificationMessageType(typeItem: NotificationMessageTypeItem) {
        updateEditedNotification { old -> old.copy(messageType = typeItem.toNotificationMessageType()) }
    }

    fun setNotificationMessage(message: String) {
        updateEditedNotification { old -> old.copy(messageText = "" + message) }
    }

    fun setNotificationMessageCounterName(counterName: String) {
        updateEditedNotification { old -> old.copy(messageCounterName = "" + counterName) }
    }

    fun setNotificationImportance(channelItem: NotificationImportanceItem) {
        updateEditedNotification { old -> old.copy(channelImportance = channelItem.toNotificationImportance()) }
    }

    private fun updateEditedNotification(updater: (old: Notification) -> Notification) {
        editionRepository.editionState.getEditedAction<Notification>()?.let { oldNotification ->
            editionRepository.updateEditedAction(updater(oldNotification))
        }
    }
}

data class UiNotificationMessage(
    val typeItem: NotificationMessageTypeItem,
    val messageContent: String,
)
