/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.domain

import android.content.Context
import android.util.Log

import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
internal class DumbEditionRepository private constructor(context: Context) {

    companion object {

        /** Tag for logs */
        private const val TAG = "DumbEditionRepository"

        /** Singleton preventing multiple instances of the DumbEditionRepository at the same time. */
        @Volatile
        private var INSTANCE: DumbEditionRepository? = null

        /**
         * Get the DumbEditionRepository singleton, or instantiates it if it wasn't yet.
         * @param context the Android context.
         * @return the DumbEditionRepository singleton.
         */
        fun getInstance(context: Context): DumbEditionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DumbEditionRepository(context)
                INSTANCE = instance
                instance

            }
        }
    }

    /** The repository providing access to the database. */
    private val dumbRepository: DumbRepository = DumbRepository.getRepository(context)

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

    fun addNewDumbAction(dumbAction: DumbAction, insertionIndex: Int? = null) {
        val editedScenario = _editedDumbScenario.value ?: return

        Log.d(TAG, "Add dumb action to edited scenario $dumbAction at position $insertionIndex")
        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = editedScenario.dumbActions.toMutableList().apply {
                if (insertionIndex != null && insertionIndex in editedScenario.dumbActions.indices) {
                    add(insertionIndex, dumbAction)
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
            }
        )
    }

    fun updateDumbScenario(dumbScenario: DumbScenario) {
        Log.d(TAG, "Updating dumb scenario with $dumbScenario")
        _editedDumbScenario.value = dumbScenario
    }
}