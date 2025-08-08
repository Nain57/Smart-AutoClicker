
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.actionlist

import android.content.Context
import android.graphics.Point
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.dumb.config.domain.DumbEditionRepository
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy.DumbActionDetails
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy.toDumbActionDetails

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DumbActionListViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val dumbEditionRepository: DumbEditionRepository,
) : ViewModel() {

    private val userModifications: StateFlow<DumbScenario?> =
        dumbEditionRepository.editedDumbScenario

    /** The list of dumb actions for the scenario. */
    val dumbActionsDetails: Flow<List<DumbActionDetails>> = userModifications
        .map { dumbScenario ->
            dumbScenario?.dumbActions?.map { it.toDumbActionDetails(context) } ?: emptyList()
        }

    val canCopyAction: Flow<Boolean> = dumbEditionRepository.actionsToCopy
        .map { it.isNotEmpty() }

    fun createNewDumbClick(context: Context, position: Point): DumbAction.DumbClick =
        dumbEditionRepository.dumbActionBuilder.createNewDumbClick(context, position)

    fun createNewDumbSwipe(context: Context, from: Point, to: Point): DumbAction.DumbSwipe =
        dumbEditionRepository.dumbActionBuilder.createNewDumbSwipe(context, from, to)

    fun createNewDumbPause(context: Context): DumbAction.DumbPause =
        dumbEditionRepository.dumbActionBuilder.createNewDumbPause(context)

    fun createDumbActionCopy(actionToCopy: DumbAction): DumbAction =
        dumbEditionRepository.dumbActionBuilder.createNewDumbActionFrom(actionToCopy)

    fun addNewDumbAction(dumbAction: DumbAction) {
        dumbEditionRepository.addNewDumbAction(dumbAction)
    }

    fun updateDumbAction(dumbAction: DumbAction) {
        dumbEditionRepository.updateDumbAction(dumbAction)
    }

    fun updateDumbActionOrder(actions: List<DumbActionDetails>) {
        dumbEditionRepository.updateDumbActions(actions.map { it.action })
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