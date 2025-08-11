package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.keyboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.ShowKeyboard
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*

class ShowKeyboardViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val action: Flow<ShowKeyboard> =
        editionRepository.editionState.editedActionState.mapNotNull { it.value as? ShowKeyboard }

    val name: Flow<String?> = action.map { it.name }.take(1)
    val positionTypeIndex: StateFlow<Int> = action.map {
        if (it.positionType == Click.PositionType.ON_DETECTED_CONDITION) 0 else 1
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val conditionNames: StateFlow<List<String>> =
        editionRepository.editionState.editedEventState.map { evt ->
            val e = evt.value
            if (e is ImageEvent) e.conditions.map { it.name } else emptyList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun currentConditionIndex(): Int {
        val evt = editionRepository.editionState.editedEventState.value.value
        val a = editionRepository.editionState.getEditedAction<ShowKeyboard>() ?: return 0
        if (evt !is ImageEvent || a.onConditionId == null) return 0
        return evt.conditions.indexOfFirst { it.id == a.onConditionId }.coerceAtLeast(0)
    }

    fun setName(name: String) =
        editionRepository.editionState.getEditedAction<ShowKeyboard>()?.let {
            editionRepository.updateEditedAction(it.copy(name = "" + name))
        } ?: Unit

    fun setPositionTypeByIndex(idx: Int) {
        val type = if (idx == 0) Click.PositionType.ON_DETECTED_CONDITION else Click.PositionType.USER_SELECTED
        editionRepository.editionState.getEditedAction<ShowKeyboard>()?.let {
            editionRepository.updateEditedAction(it.copy(positionType = type))
        }
    }
    fun setTargetConditionByIndex(idx: Int) {
        val evt = editionRepository.editionState.editedEventState.value.value as? ImageEvent ?: return
        val id = evt.conditions.getOrNull(idx)?.id ?: return
        editionRepository.editionState.getEditedAction<ShowKeyboard>()?.let {
            editionRepository.updateEditedAction(it.copy(onConditionId = id))
        }
    }

    val canSave: Flow<Boolean> = action.map { true }
    fun saveLastConfig() {}
    fun deleteAction() { editionRepository.deleteEditedAction() }
}