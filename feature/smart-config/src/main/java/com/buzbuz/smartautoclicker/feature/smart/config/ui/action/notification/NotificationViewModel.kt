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

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.common.actions.text.appendCounterReference

import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.counter.UiStaticOrCounterSelection

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    val uiState: StateFlow<NotificationDialogUiState?> =
        combine(configuredNotification, editedActionHasChanged) { notification, hasChanged ->
            notification.toUiState(hasChanged)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)


    fun hasUnsavedModifications(): Boolean =
        uiState.value?.hasUnsavedModifications == true

    @ChecksSdkIntAtLeast(Build.VERSION_CODES.O)
    fun shouldShowSettingsButton(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    fun setName(name: String) {
        updateEditedNotification { old -> old.copy(name = "" + name) }
    }

    fun setNotificationMessage(message: String) {
        updateEditedNotification { old -> old.copy(messageText = "" + message) }
    }

    fun setNotificationImportance(channelItem: NotificationImportanceItem) {
        updateEditedNotification { old -> old.copy(channelImportance = channelItem.toNotificationImportance()) }
    }

    private fun updateEditedNotification(updater: (old: Notification) -> Notification) {
        editionRepository.editionState.getEditedAction<Notification>()?.let { oldNotification ->
            editionRepository.updateEditedAction(updater(oldNotification))
        }
    }

    private fun Notification.toUiState(hasUnsavedModifications: Boolean): NotificationDialogUiState =
        NotificationDialogUiState(
            canBeSaved = isComplete(),
            hasUnsavedModifications = hasUnsavedModifications,
            name = name ?: "",
            nameError = name.isNullOrEmpty(),
            message = messageText,
            importance = channelImportance.toImportanceItem(),
        )
}
