
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@OptIn(FlowPreview::class)
class BroadcastReceivedConditionViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /** The condition being configured by the user. */
    private val configuredCondition: Flow<TriggerCondition.OnBroadcastReceived> =
        editionRepository.editionState.editedTriggerConditionState
            .mapNotNull { it.value }
            .filterIsInstance<TriggerCondition.OnBroadcastReceived>()

    private val editedConditionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedTriggerConditionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)

    val name: Flow<String?> = configuredCondition.map { it.name }.take(1)
    val nameError: Flow<Boolean> = configuredCondition.map { it.name.isEmpty() }

    val intentAction: Flow<String?> = configuredCondition
        .map { it.intentAction }
        .take(1)
    val intentActionError: Flow<Boolean> = configuredCondition.map { it.intentAction.isEmpty() }

    /** Tells if the configured condition is valid and can be saved. */
    val conditionCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedTriggerConditionState.map { condition ->
        condition.canBeSaved
    }

    fun hasUnsavedModifications(): Boolean =
        editedConditionHasChanged.value

    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    fun setIntentAction(action: String?) {
        updateEditedCondition { it.copy(intentAction = action ?: "") }
    }

    fun getIntentAction(): String =
        editionRepository.editionState.getEditedCondition<TriggerCondition.OnBroadcastReceived>()?.intentAction ?: ""

    private fun updateEditedCondition(
        closure: (oldValue: TriggerCondition.OnBroadcastReceived) -> TriggerCondition.OnBroadcastReceived?,
    ) {
        editionRepository.editionState.getEditedCondition<TriggerCondition.OnBroadcastReceived>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }
}