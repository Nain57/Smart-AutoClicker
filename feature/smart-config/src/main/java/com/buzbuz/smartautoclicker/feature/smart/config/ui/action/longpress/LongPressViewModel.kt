package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.longpress

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.LongPress
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putClickPressDurationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*

class LongPressViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val prefs: SharedPreferences = context.getEventConfigPreferences()

    private val actionFlow: Flow<LongPress> =
        editionRepository.editionState.editedActionState
            .mapNotNull { it.value as? LongPress }

    val name: Flow<String?> = actionFlow.map { it.name }.take(1)
    val holdDuration: Flow<Long?> = actionFlow.map { it.holdDuration }.take(1)

    // Position type & target condition UI
    val positionTypeIndex: StateFlow<Int> = actionFlow.map {
        if (it.positionType == Click.PositionType.ON_DETECTED_CONDITION) 0 else 1
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val conditionNames: StateFlow<List<String>> =
        editionRepository.editionState.editedEventState.map { evtState ->
            val evt = evtState.value
            if (evt is ImageEvent) evt.conditions.map { it.name } else emptyList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun currentConditionIndex(): Int {
        val evt = editionRepository.editionState.editedEventState.value.value
        val press = editionRepository.editionState.getEditedAction<LongPress>() ?: return 0
        if (evt !is ImageEvent || press.onConditionId == null) return 0
        return evt.conditions.indexOfFirst { it.id == press.onConditionId }.coerceAtLeast(0)
    }

    // Mutations
    fun setName(name: String) {
        editionRepository.editionState.getEditedAction<LongPress>()?.let {
            editionRepository.updateEditedAction(it.copy(name = "" + name))
        }
    }
    fun setHoldDuration(ms: Long) {
        editionRepository.editionState.getEditedAction<LongPress>()?.let {
            editionRepository.updateEditedAction(it.copy(holdDuration = ms))
        }
    }
    fun setPositionTypeByIndex(idx: Int) {
        val type = if (idx == 0) Click.PositionType.ON_DETECTED_CONDITION else Click.PositionType.USER_SELECTED
        editionRepository.editionState.getEditedAction<LongPress>()?.let {
            editionRepository.updateEditedAction(it.copy(positionType = type))
        }
    }
    fun setTargetConditionByIndex(idx: Int) {
        val evt = editionRepository.editionState.editedEventState.value.value as? com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
            ?: return
        val id = evt.conditions.getOrNull(idx)?.id ?: return
        editionRepository.editionState.getEditedAction<LongPress>()?.let {
            editionRepository.updateEditedAction(it.copy(onConditionId = id))
        }
    }

    fun saveLastConfig() {
        // Persist last used durations like Click does (optional but consistent)
        editionRepository.editionState.getEditedAction<LongPress>()?.holdDuration?.let { ms ->
            prefs.putClickPressDurationConfig(ms)
        }
    }

    fun deleteAction() {
        editionRepository.deleteEditedAction()
    }
}