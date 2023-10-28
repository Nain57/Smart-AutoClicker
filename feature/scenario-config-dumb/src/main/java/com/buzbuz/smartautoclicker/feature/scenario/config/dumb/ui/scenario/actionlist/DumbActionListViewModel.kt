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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.actionlist

import android.app.Application
import android.graphics.Point

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.domain.DumbEditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class DumbActionListViewModel(application: Application) : AndroidViewModel(application) {

    private val dumbEditionRepository = DumbEditionRepository.getInstance(application)

    private val userModifications: StateFlow<DumbScenario?> =
        dumbEditionRepository.editedDumbScenario

    /** The list of dumb actions for the scenario. */
    val dumbActionsDetails: Flow<List<DumbActionDetails>> = userModifications
        .map { dumbScenario ->
            dumbScenario?.dumbActions?.map { it.toDumbActionDetails(application) } ?: emptyList()
        }

    val canCopyAction: Flow<Boolean> = dumbEditionRepository.actionsToCopy
        .map { it.isNotEmpty() }

    fun createNewDumbClick(position: Point): DumbAction.DumbClick =
        dumbEditionRepository.dumbActionBuilder.createNewDumbClick(getApplication(), position)

    fun createNewDumbSwipe(from: Point, to: Point): DumbAction.DumbSwipe =
        dumbEditionRepository.dumbActionBuilder.createNewDumbSwipe(getApplication(), from, to)

    fun createNewDumbPause(): DumbAction.DumbPause =
        dumbEditionRepository.dumbActionBuilder.createNewDumbPause(getApplication())

    fun createDumbActionCopy(actionToCopy: DumbAction): DumbAction =
        dumbEditionRepository.dumbActionBuilder.createNewDumbActionFrom(actionToCopy)

    fun addNewDumbAction(dumbAction: DumbAction) {
        val actionList = userModifications.value?.dumbActions ?: return

        userModifications.value?.copy(
            dumbActions = actionList.toMutableList().apply {
                add(dumbAction)
            }
        )?.let {
            dumbEditionRepository.updateDumbScenario(it)
        }
    }

    fun updateDumbAction(dumbAction: DumbAction) {
        val actionList = userModifications.value?.dumbActions ?: return

        val actionIndex = actionList.indexOfFirst { it.id == dumbAction.id }
        if (actionIndex == -1) return

        userModifications.value?.copy(
            dumbActions = actionList.toMutableList().apply {
                set(actionIndex, dumbAction)
            }
        )?.let {
            dumbEditionRepository.updateDumbScenario(it)
        }
    }

    fun updateDumbActionOrder(actions: List<DumbActionDetails>) {
        userModifications.value?.copy(dumbActions = actions.map { it.action })?.let {
            dumbEditionRepository.updateDumbScenario(it)
        }
    }

    fun deleteDumbAction(dumbAction: DumbAction) {
        val actionList = userModifications.value?.dumbActions ?: return

        val actionIndex = actionList.indexOfFirst { it.id == dumbAction.id }
        if (actionIndex == -1) return

        userModifications.value?.copy(
            dumbActions = actionList.toMutableList().apply {
                removeAt(actionIndex)
            }
        )?.let {
            dumbEditionRepository.updateDumbScenario(it)
        }
    }
}