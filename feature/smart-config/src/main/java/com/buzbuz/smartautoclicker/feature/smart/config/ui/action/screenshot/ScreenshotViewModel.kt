package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.screenshot

import android.content.Context
import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Screenshot
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*

class ScreenshotViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val action: Flow<Screenshot> =
        editionRepository.editionState.editedActionState.mapNotNull { it.value as? Screenshot }

    val name: Flow<String?> = action.map { it.name }.take(1)
    val savePath: Flow<String?> = action.map { it.savePath }.take(1)
    val roi: Flow<Rect?> = action.map { it.roi }.take(1)

    val canSave: Flow<Boolean> = action.map { true }

    fun setName(name: String) =
        editionRepository.editionState.getEditedAction<Screenshot>()?.let {
            editionRepository.updateEditedAction(it.copy(name = "" + name))
        } ?: Unit

    fun setSavePath(path: String) =
        editionRepository.editionState.getEditedAction<Screenshot>()?.let {
            editionRepository.updateEditedAction(it.copy(savePath = path.ifBlank { null }))
        } ?: Unit

    fun setRoi(rect: Rect?) =
        editionRepository.editionState.getEditedAction<Screenshot>()?.let {
            editionRepository.updateEditedAction(it.copy(roi = rect))
        } ?: Unit

    fun saveLastConfig() {}
    fun deleteAction() { editionRepository.deleteEditedAction() }
}