/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.condition.copy

import android.content.Context

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager

import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.overlays.base.bindings.updateState
import com.buzbuz.smartautoclicker.overlays.base.dialog.CopyDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 * [CopyDialog] implementation for displaying the whole list of conditions for a copy.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param conditions the list of edited conditions for the configured event.
 * @param onConditionSelected the listener called when the user select a Condition.
 */
class ConditionCopyDialog(
    context: Context,
    private val conditions: List<Condition>,
    private val onConditionSelected: (Condition) -> Unit,
) : CopyDialog(context)  {

    /** View model for this content. */
    private val viewModel: ConditionCopyModel by lazy { ViewModelProvider(this).get(ConditionCopyModel::class.java) }

    /** Adapter displaying the list of conditions. */
    private lateinit var conditionAdapter: ConditionCopyAdapter

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewModel.setItemsFromContainer(conditions)

        conditionAdapter = ConditionCopyAdapter(
            conditionClickedListener = { selectedCondition, _ ->
                viewModel.let {
                    onConditionSelected(it.getNewConditionForCopy(selectedCondition))
                    destroy()
                }
            },
            bitmapProvider = { bitmap, onLoaded ->
                viewModel.getConditionBitmap(bitmap, onLoaded)
            },
        )

        viewBinding.layoutLoadableList.list.apply {
            adapter = conditionAdapter

            layoutManager = GridLayoutManager(
                context,
                2,
            ).apply {
                spanSizeLookup = conditionAdapter.spanSizeLookup
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.conditionList.collect(::updateConditionList)
            }
        }
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    private fun updateConditionList(newItems: List<ConditionCopyModel.ConditionCopyItem>?) {
        viewBinding.layoutLoadableList.updateState(newItems)
        conditionAdapter.submitList(if (newItems == null) ArrayList() else ArrayList(newItems))
    }
}