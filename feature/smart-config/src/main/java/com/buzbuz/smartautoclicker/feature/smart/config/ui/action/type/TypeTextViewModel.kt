package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.type

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.TypeText
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*

class TypeTextViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val action: Flow<TypeText> =
        editionRepository.editionState.editedActionState.mapNotNull { it.value as? TypeText }

    val name: Flow<String?> = action.map { it.name }.take(1)
    val text: Flow<String?> = action.map { it.text }.take(1)
    val canSave: Flow<Boolean> = action.map { !it.text.isNullOrBlank() }

    fun setName(name: String) =
        editionRepository.editionState.getEditedAction<TypeText>()?.let {
            editionRepository.updateEditedAction(it.copy(name = "" + name))
        } ?: Unit

    fun setText(text: String) =
        editionRepository.editionState.getEditedAction<TypeText>()?.let {
            editionRepository.updateEditedAction(it.copy(text = text))
        } ?: Unit

    fun saveLastConfig() {}
    fun deleteAction() { editionRepository.deleteEditedAction() }
}