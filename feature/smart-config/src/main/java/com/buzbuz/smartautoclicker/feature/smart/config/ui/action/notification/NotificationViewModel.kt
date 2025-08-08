
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
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

    @ChecksSdkIntAtLeast(Build.VERSION_CODES.O)
    fun shouldShowSettingsButton(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

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
