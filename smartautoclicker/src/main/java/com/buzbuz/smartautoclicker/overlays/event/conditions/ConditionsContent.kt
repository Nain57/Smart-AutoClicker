/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.event.conditions

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ContentConditionsBinding
import com.buzbuz.smartautoclicker.domain.*
import com.buzbuz.smartautoclicker.overlays.base.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.NavigationRequest
import com.buzbuz.smartautoclicker.overlays.bindings.*
import com.buzbuz.smartautoclicker.overlays.condition.ConditionDialog
import com.buzbuz.smartautoclicker.overlays.condition.ConditionSelectorMenu
import com.buzbuz.smartautoclicker.overlays.condition.copy.ConditionCopyDialog
import com.buzbuz.smartautoclicker.overlays.event.EventDialogViewModel

import kotlinx.coroutines.launch

class ConditionsContent : NavBarDialogContent() {

    /** View model for the container dialog. */
    private val dialogViewModel: EventDialogViewModel by lazy {
        ViewModelProvider(dialogViewModelStoreOwner).get(EventDialogViewModel::class.java)
    }
    /** View model for this content. */
    private val viewModel: ConditionsViewModel by lazy {
        ViewModelProvider(this).get(ConditionsViewModel::class.java)
    }

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentConditionsBinding
    /** Adapter for the list of conditions. */
    private lateinit var conditionsAdapter: ConditionAdapter

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setConfiguredEvent(dialogViewModel.configuredEvent)

        viewBinding = ContentConditionsBinding.inflate(LayoutInflater.from(context), container, false).apply {
            createCopyButtons.apply {
                buttonNew.setOnClickListener { onNewButtonClicked() }
                buttonCopy.setOnClickListener { onCopyButtonClicked() }
            }
        }

        conditionsAdapter = ConditionAdapter(
            conditionClickedListener = ::onConditionClicked,
            bitmapProvider = viewModel::getConditionBitmap,
        )

        viewBinding.layoutList.apply {
            setEmptyText(R.string.dialog_conditions_empty)
            list.apply {
                adapter = conditionsAdapter
                layoutManager = GridLayoutManager(
                    context,
                    2,
                )
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.conditions.collect(::updateConditionList) }
            }
        }
    }

    private fun onNewButtonClicked() {
        dialogViewModel.requestSubOverlay(newConditionSelectorNavigationRequest())
    }

    private fun onCopyButtonClicked() {
        dialogViewModel.requestSubOverlay(newConditionCopyNavigationRequest())
    }

    private fun onConditionClicked(condition: Condition, index: Int) {
        dialogViewModel.requestSubOverlay(newConditionConfigNavigationRequest(condition, index))
    }

    private fun updateConditionList(newItems: List<Condition>?) {
        viewBinding.layoutList.updateState(newItems)
        conditionsAdapter.submitList(newItems)
    }

    private fun newConditionSelectorNavigationRequest() = NavigationRequest(
        overlay = ConditionSelectorMenu(
            context = context,
            onConditionSelected = { area, bitmap ->
                dialogViewModel.requestSubOverlay(
                    newConditionConfigNavigationRequest(
                        viewModel.createCondition(context, area, bitmap)
                    )
                )
            }
        ),
        hideCurrent = true,
    )

    private fun newConditionCopyNavigationRequest() = NavigationRequest(
        ConditionCopyDialog(
            context = context,
            conditions = viewModel.conditions.value!!,
            onConditionSelected = { conditionSelected ->
                dialogViewModel.requestSubOverlay(
                    newConditionConfigNavigationRequest(conditionSelected)
                )
            }
        )
    )

    private fun newConditionConfigNavigationRequest(condition: Condition, index: Int = -1) = NavigationRequest(
        ConditionDialog(
            context = context,
            condition = condition,
            onConfirmClicked = {
                if (index != -1) {
                    viewModel.updateCondition(it, index)
                } else {
                    viewModel.addCondition(it)
                }
            },
            onDeleteClicked = { viewModel.removeCondition(condition) }
        )
    )
}