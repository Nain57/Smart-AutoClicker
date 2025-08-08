
package com.buzbuz.smartautoclicker.feature.dumb.config.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.engine.DumbEngine
import com.buzbuz.smartautoclicker.feature.dumb.config.domain.DumbEditionRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DumbMainMenuModel @Inject constructor(
    private val dumbEditionRepository: DumbEditionRepository,
    private val dumbEngine: DumbEngine,
) : ViewModel() {

    val canPlay: Flow<Boolean> =
        combine(dumbEditionRepository.isEditionSynchronized, dumbEngine.dumbScenario) { isSync, scenario ->
            isSync && scenario?.isValid() ?: false
        }
    val isPlaying: StateFlow<Boolean> =
        dumbEngine.isRunning

    fun startEdition(dumbScenarioId: Identifier, onStarted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (dumbEditionRepository.startEdition(dumbScenarioId.databaseId)) {
                withContext(Dispatchers.Main) {
                    onStarted()
                }
            }
        }
    }

    fun saveEditions() {
        viewModelScope.launch(Dispatchers.IO) {
            dumbEditionRepository.apply {
                saveEditions()
            }
        }
    }

    fun stopEdition() {
        dumbEditionRepository.stopEdition()
    }

    fun toggleScenarioPlay() {
        viewModelScope.launch {
            if (isPlaying.value) dumbEngine.stopDumbScenario()
            else dumbEngine.startDumbScenario()
        }
    }

    fun stopScenarioPlay(): Boolean {
        if (!isPlaying.value) return false

        viewModelScope.launch {
            dumbEngine.stopDumbScenario()
        }
        return true
    }
}