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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger

import android.content.Context
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.toUiTriggerCondition
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject


class TriggerConditionListViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    val configuredTriggerConditions: Flow<List<UiTriggerCondition>> =
        editionRepository.editionState.editedEventTriggerConditionsState
            .mapNotNull { triggerConditionsState ->
                triggerConditionsState.value?.map { triggerCondition ->
                    triggerCondition.toUiTriggerCondition(context, inError = !triggerCondition.isComplete())
                }
            }

    val canBeSaved: Flow<Boolean> = editionRepository.editionState.editedEventTriggerConditionsState
        .map { it.canBeSaved }

    /** Tells if there is at least one condition to copy. */
    val canCopyCondition: Flow<Boolean> = editionRepository.editionState.canCopyConditions

    /**
     * Create a new condition with the default values from configuration.
     *
     * @param context the Android Context.
     * @param type the type of condition to create.
     */
    fun createNewTriggerCondition(context: Context, type: TriggerConditionTypeChoice): TriggerCondition =
        when (type) {
            TriggerConditionTypeChoice.OnBroadcastReceived ->
                editionRepository.editedItemsBuilder.createNewOnBroadcastReceived(context)
            TriggerConditionTypeChoice.OnCounterReached ->
                editionRepository.editedItemsBuilder.createNewOnCounterReached(context)
            TriggerConditionTypeChoice.OnTimerReached ->
                editionRepository.editedItemsBuilder.createNewOnTimerReached(context)
        }

    /**
     * Get a new condition based on the provided one.
     * @param condition the condition to copy.
     */
    fun createNewTriggerConditionFromCopy(condition: TriggerCondition): TriggerCondition =
        editionRepository.editedItemsBuilder.createNewTriggerConditionFrom(condition)

    fun startConditionEdition(condition: Condition) = editionRepository.startConditionEdition(condition)

    /** Insert/update a new condition to the event. */
    fun upsertEditedCondition() =
        editionRepository.upsertEditedCondition()

    /** Remove a condition from the event. */
    fun removeEditedCondition() =
        editionRepository.deleteEditedCondition()

    /** Drop all changes made to the currently edited event. */
    fun dismissEditedCondition() = editionRepository.stopConditionEdition()
}