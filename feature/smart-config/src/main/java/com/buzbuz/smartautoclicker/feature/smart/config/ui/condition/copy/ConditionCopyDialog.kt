/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.CopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.adapters.UiConditionGridAdapter
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiConditionItem

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 * [CopyDialog] implementation for displaying the whole list of conditions for a copy.
 *
 * @param onConditionSelected the listener called when the user select a Condition.
 */
class ConditionCopyDialog(
    private val onConditionSelected: (Condition) -> Unit,
) : CopyDialog(R.style.ScenarioConfigTheme)  {

    /** View model for this content. */
    private val viewModel: ConditionCopyModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { conditionCopyViewModel() },
    )

    /** Adapter displaying the list of conditions. */
    private lateinit var conditionAdapter: UiConditionGridAdapter

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val searchHintRes: Int = R.string.search_view_hint_condition_copy
    override val emptyRes: Int = R.string.message_empty_copy

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        conditionAdapter = UiConditionGridAdapter(
            context = context,
            onItemClicked = { selectedCondition, _ ->
                back()
                onConditionSelected(selectedCondition.condition)
            },
            bitmapProvider = { bitmap, onLoaded ->
                viewModel.getConditionBitmap(bitmap, onLoaded)
            },
        )

        viewBinding.layoutLoadableList.list.adapter = conditionAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.conditionList.collect(::updateConditionList)
            }
        }
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    private fun updateConditionList(newItems: List<UiConditionItem>?) {
        viewBinding.layoutLoadableList.updateState(newItems)
        conditionAdapter.submitList(if (newItems == null) ArrayList() else ArrayList(newItems))
    }
}