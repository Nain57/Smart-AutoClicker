package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.scroll

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Axis
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Scroll
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*

class ScrollViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val action: Flow<Scroll> =
        editionRepository.editionState.editedActionState.mapNotNull { it.value as? Scroll }

    val name: Flow<String?> = action.map { it.name }.take(1)
    val axisIndex: StateFlow<Int> = action.map {
        when (it.axis) { Axis.UP -> 0; Axis.DOWN -> 1; Axis.LEFT -> 2; Axis.RIGHT -> 3; else -> 1 }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 1)
    val distancePercent: Flow<Int?> = action.map { (it.distancePercent ?: 0.6f * 100).toInt() }.take(1)
    val duration: Flow<Long?> = action.map { it.duration }.take(1)
    val stutter: Flow<Boolean?> = action.map { it.stutter }.take(1)

    val canSave: Flow<Boolean> = action.map { (it.distancePercent ?: 0.6f) in 0.1f..0.9f }

    fun setName(name: String) =
        editionRepository.editionState.getEditedAction<Scroll>()?.let {
            editionRepository.updateEditedAction(it.copy(name = "" + name))
        } ?: Unit

    fun setAxisByIndex(idx: Int) {
        val axis = when (idx) { 0->Axis.UP; 1->Axis.DOWN; 2->Axis.LEFT; else->Axis.RIGHT }
        editionRepository.editionState.getEditedAction<Scroll>()?.let {
            editionRepository.updateEditedAction(it.copy(axis = axis))
        }
    }

    fun setDistancePercent(pct: Int) {
        val v = (pct.coerceIn(10, 90) / 100f)
        editionRepository.editionState.getEditedAction<Scroll>()?.let {
            editionRepository.updateEditedAction(it.copy(distancePercent = v))
        }
    }

    fun setDuration(ms: Long) =
        editionRepository.editionState.getEditedAction<Scroll>()?.let {
            editionRepository.updateEditedAction(it.copy(duration = ms))
        } ?: Unit

    fun setStutter(value: Boolean) =
        editionRepository.editionState.getEditedAction<Scroll>()?.let {
            editionRepository.updateEditedAction(it.copy(stutter = value))
        } ?: Unit

    fun saveLastConfig() { /* optional */ }
    fun deleteAction() { editionRepository.deleteEditedAction() }
}