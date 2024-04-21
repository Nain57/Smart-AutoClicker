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
package com.buzbuz.smartautoclicker.feature.dumb.config.domain

import android.util.Log

import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DumbEditionRepository @Inject constructor(
    private val dumbRepository: IDumbRepository,
) {

    private val _editedDumbScenario: MutableStateFlow<DumbScenario?> = MutableStateFlow(null)
    val editedDumbScenario: StateFlow<DumbScenario?> = _editedDumbScenario

    private val otherActions: Flow<List<DumbAction>> = _editedDumbScenario
        .filterNotNull()
        .flatMapLatest { dumbScenario ->
            dumbRepository.getAllDumbActionsFlowExcept(dumbScenario.id.databaseId)
        }
    val actionsToCopy: Flow<List<DumbAction>> = _editedDumbScenario
        .filterNotNull()
        .combine(otherActions) { dumbScenario, otherActions ->
            mutableListOf<DumbAction>().apply {
                addAll(dumbScenario.dumbActions)
                addAll(otherActions)
            }
        }

    /** Tells if the editions made on the scenario are synchronized with the database values. */
    val isEditionSynchronized: Flow<Boolean> = editedDumbScenario.map { it == null }

    val dumbActionBuilder: EditedDumbActionsBuilder = EditedDumbActionsBuilder()

    /** Set the scenario to be configured. */
    suspend fun startEdition(scenarioId: Long): Boolean {
        val scenario = dumbRepository.getDumbScenario(scenarioId) ?: run {
            Log.e(TAG, "Can't start edition, dumb scenario $scenarioId not found")
            return false
        }

        Log.d(TAG, "Start edition of dumb scenario $scenarioId")

        _editedDumbScenario.value = scenario
        dumbActionBuilder.startEdition(scenario.id)

        return true
    }

    /** Save editions changes in the database. */
    suspend fun saveEditions() {
        val scenarioToSave = _editedDumbScenario.value ?: return
        Log.d(TAG, "Save editions")

        dumbRepository.updateDumbScenario(scenarioToSave)
        stopEdition()
    }

    fun stopEdition() {
        Log.d(TAG, "Stop editions")

        _editedDumbScenario.value = null
        dumbActionBuilder.clearState()
    }

    fun updateDumbScenario(dumbScenario: DumbScenario) {
        Log.d(TAG, "Updating dumb scenario with $dumbScenario")
        _editedDumbScenario.value = dumbScenario
    }

    fun addNewDumbAction(dumbAction: DumbAction, insertionIndex: Int? = null) {
        val editedScenario = _editedDumbScenario.value ?: return

        Log.d(TAG, "Add dumb action to edited scenario $dumbAction at position $insertionIndex")
        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = editedScenario.dumbActions.toMutableList().apply {
                if (insertionIndex != null && insertionIndex in editedScenario.dumbActions.indices) {
                    add(insertionIndex, dumbAction)
                    for (index in editedScenario.dumbActions.indices) {
                        set(index, dumbAction.copyWithNewPriority(index))
                    }
                } else {
                    add(dumbAction)
                }
            }
        )
    }

    fun updateDumbAction(dumbAction: DumbAction) {
        val editedScenario = _editedDumbScenario.value ?: return
        val actionIndex = editedScenario.dumbActions.indexOfFirst { it.id == dumbAction.id }
        if (actionIndex == -1) {
            Log.w(TAG, "Can't update action, it is not in the edited scenario.")
            return
        }

        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = editedScenario.dumbActions.toMutableList().apply {
                set(actionIndex, dumbAction)
            }
        )
    }

    fun deleteDumbAction(dumbAction: DumbAction) {
        val editedScenario = _editedDumbScenario.value ?: return
        val deleteIndex = editedScenario.dumbActions.indexOfFirst { it.id == dumbAction.id }

        Log.d(TAG, "Delete dumb action from edited scenario $dumbAction")
        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = editedScenario.dumbActions.toMutableList().apply {
                removeAt(deleteIndex)

                for (index in editedScenario.dumbActions.indices) {
                    set(index, dumbAction.copyWithNewPriority(index))
                }
            }
        )
    }

    fun updateDumbActions(dumbActions: List<DumbAction>) {
        val editedScenario = _editedDumbScenario.value ?: return

        Log.d(TAG, "Updating dumb action list with $dumbActions")
        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = dumbActions.mapIndexed { index, action ->
                action.copyWithNewPriority(index)
            }
        )
    }

    private fun DumbAction.copyWithNewPriority(priority: Int): DumbAction =
        when (this) {
            is DumbAction.DumbClick -> copy(priority = priority)
            is DumbAction.DumbPause -> copy(priority = priority)
            is DumbAction.DumbSwipe -> copy(priority = priority)
        }
}

/** Tag for logs */
private const val TAG = "DumbEditionRepository"