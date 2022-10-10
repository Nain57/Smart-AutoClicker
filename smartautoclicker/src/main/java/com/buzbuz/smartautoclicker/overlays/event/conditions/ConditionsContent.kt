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
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ContentConditionsBinding
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.ConditionOperator
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.overlays.base.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.eventconfig.SubOverlay
import com.buzbuz.smartautoclicker.overlays.utils.LoadableListController

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ConditionsContent(private val event: MutableStateFlow<Event?>) : NavBarDialogContent() {

    /** View model for this content. */
    private val viewModel: ConditionsViewModel by lazy { ViewModelProvider(this).get(ConditionsViewModel::class.java) }

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentConditionsBinding
    /** Controls the display state of the condition list (empty, loading, loaded). */
    private lateinit var listController: LoadableListController<ConditionListItem, RecyclerView.ViewHolder>
    /** Adapter for the list of conditions. */
    private lateinit var conditionsAdapter: ConditionAdapter

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setConfiguredEvent(event)

        viewBinding = ContentConditionsBinding.inflate(LayoutInflater.from(context), container, false).apply {
            buttonNew.setOnClickListener { onNewButtonClicked() }
            buttonCopy.setOnClickListener { onCopyButtonClicked() }

            conditionsOperatorButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                when (checkedId) {
                    R.id.end_conditions_and_button -> viewModel.setConditionOperator(AND)
                    R.id.end_conditions_or_button -> viewModel.setConditionOperator(OR)
                }
            }
        }

        conditionsAdapter = ConditionAdapter(
            addConditionClickedListener = {
                //subOverlayViewModel?.requestSubOverlay(SubOverlay.ConditionCapture)
            },
            copyConditionClickedListener = {
                //subOverlayViewModel?.requestSubOverlay(SubOverlay.ConditionCopy)
            },
            conditionClickedListener = { index, condition ->
                //subOverlayViewModel?.requestSubOverlay(SubOverlay.ConditionConfig(condition, index))
            },
            bitmapProvider = viewModel::getConditionBitmap,
        )

        listController = LoadableListController(
            owner = this,
            root = viewBinding.layoutList,
            adapter = conditionsAdapter,
            emptyTextId = R.string.dialog_conditions_empty,
        )
        listController.listView.adapter = conditionsAdapter

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.conditionListItems.collect(listController::submitList) }
                launch { viewModel.conditionOperator.collect(::updateConditionOperator) }
            }
        }
    }

    private fun onNewButtonClicked() {

    }

    private fun onCopyButtonClicked() {

    }

    private fun updateConditionOperator(@ConditionOperator operator: Int?) {
        viewBinding.apply {
            val (text, buttonId) = when (operator) {
                AND -> context.getString(R.string.condition_operator_and) to R.id.end_conditions_and_button
                OR -> context.getString(R.string.condition_operator_or) to R.id.end_conditions_or_button
                else -> return@apply
            }

            conditionsOperatorDesc.text = text
            if (conditionsOperatorButton.checkedButtonId != buttonId) {
                conditionsOperatorButton.check(buttonId)
            }
        }
    }
}