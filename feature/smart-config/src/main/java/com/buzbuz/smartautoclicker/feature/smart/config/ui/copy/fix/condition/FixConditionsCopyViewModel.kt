/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.condition

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.GetConditionMissingReferencesUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references.ReplaceMissingCounterReferenceUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixCopyUiItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixEventsChildrenCopyUiState
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.toFixCopyUiItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.toFixCopyUiState

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * View model for the [FixConditionsCopyDialog].
 * The conditions are copied into the currently edited event, so its edition state is used as reference.
 */
class FixConditionsCopyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val getConditionMissingReferencesUseCase: GetConditionMissingReferencesUseCase,
    private val replaceMissingCounterReferenceUseCase: ReplaceMissingCounterReferenceUseCase,
) : ViewModel() {

    private val conditionsToCopy: MutableStateFlow<List<Condition>?> = MutableStateFlow(null)

    val uiState: StateFlow<FixEventsChildrenCopyUiState?> = conditionsToCopy
        .map { conditions -> conditions.toUiItems(context).toFixCopyUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun setConditionsToCopy(conditions: List<Condition>) {
        conditionsToCopy.update { conditions }
    }

    fun updateCounter(
        item: FixCopyUiItem.Item.EventChildren.ConditionItem,
        missingRef: MissingCopyReference.CounterReference,
        replacement: String,
    ) {
        val oldCondition = item.uiCondition.condition
        val newCondition = replaceMissingCounterReferenceUseCase(oldCondition, missingRef, replacement)
            ?: return

        conditionsToCopy.update { old ->
            old?.map { condition -> if (condition.id == oldCondition.id) newCondition else condition }
        }
    }

    fun getFixedConditionsToCopy(): List<Condition> =
        conditionsToCopy.value ?: emptyList()

    private fun List<Condition>?.toUiItems(context: Context): List<FixCopyUiItem> {
        this ?: return emptyList()

        return buildList {
            add(FixCopyUiItem.Header(R.string.item_header_event_copy_fix))
            this@toUiItems.forEach { condition ->
                add(getConditionMissingReferencesUseCase(condition).toFixCopyUiItem(context))
            }
        }
    }
}
