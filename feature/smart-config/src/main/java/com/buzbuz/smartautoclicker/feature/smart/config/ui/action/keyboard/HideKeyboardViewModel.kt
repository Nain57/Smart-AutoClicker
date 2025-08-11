package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.keyboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.HideKeyboard
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.HideMethod
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*

class HideKeyboardViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val action: Flow<HideKeyboard> =
        editionRepository.editionState.editedActionState.mapNotNull { it.value as? HideKeyboard }

    val name: Flow<String?> = action.map { it.name }.take(1)
    val methodIndex: StateFlow<Int> = action.map {
        when (it.method) {
            HideMethod.BACK -> 0
            HideMethod.TAP_OUTSIDE -> 1
            HideMethod.BACK_THEN_TAP_OUTSIDE -> 2
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 2)

    val canSave: Flow<Boolean> = action.map { true }

    fun setName(name: String) =
        editionRepository.editionState.getEditedAction<HideKeyboard>()?.let {
            editionRepository.updateEditedAction(it.copy(name = "" + name))
        } ?: Unit

    fun setMethodByIndex(idx: Int) {
        val m = when (idx) { 0->HideMethod.BACK; 1->HideMethod.TAP_OUTSIDE; else->HideMethod.BACK_THEN_TAP_OUTSIDE }
        editionRepository.editionState.getEditedAction<HideKeyboard>()?.let {
            editionRepository.updateEditedAction(it.copy(method = m))
        }
    }

    fun saveLastConfig() {}
    fun deleteAction() { editionRepository.deleteEditedAction() }
}