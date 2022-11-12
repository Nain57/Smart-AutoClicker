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
package com.buzbuz.smartautoclicker.overlays.config.event.config

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ContentEventConfigBinding
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.ConditionOperator
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.bindings.addOnCheckedListener
import com.buzbuz.smartautoclicker.overlays.base.bindings.setButtonsText
import com.buzbuz.smartautoclicker.overlays.base.bindings.setChecked
import com.buzbuz.smartautoclicker.overlays.base.utils.setError
import com.buzbuz.smartautoclicker.overlays.config.event.EventDialogViewModel
import com.buzbuz.smartautoclicker.baseui.OnAfterTextChangedListener

import kotlinx.coroutines.launch

class EventConfigContent : NavBarDialogContent() {

    /** View model for the container dialog. */
    private val dialogViewModel: EventDialogViewModel by lazy {
        ViewModelProvider(dialogController).get(EventDialogViewModel::class.java)
    }
    /** View model for this content. */
    private val viewModel: EventConfigViewModel by lazy {
        ViewModelProvider(this).get(EventConfigViewModel::class.java)
    }

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentEventConfigBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setConfiguredEvent(dialogViewModel.configuredEvent)

        viewBinding = ContentEventConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            eventNameInputEditText.apply {
                addTextChangedListener(OnAfterTextChangedListener {
                    viewModel.setEventName(it.toString())
                })
            }

            conditionsOperatorButton.apply {
                setButtonsText(R.string.dialog_button_condition_and, R.string.dialog_button_condition_or)
                addOnCheckedListener { checkedId ->
                    when (checkedId) {
                        R.id.left_button -> viewModel.setConditionOperator(AND)
                        R.id.right_button -> viewModel.setConditionOperator(OR)
                    }
                }
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.eventNameError.collect(viewBinding.eventNameInputLayout::setError) }
                launch { viewModel.eventName.collect(::updateEventName) }
                launch { viewModel.conditionOperator.collect(::updateConditionOperator) }
            }
        }
    }

    private fun updateEventName(name: String?) {
        viewBinding.eventNameInputEditText.setText(name)
    }

    private fun updateConditionOperator(@ConditionOperator operator: Int?) {
        viewBinding.conditionsOperatorButton.apply {
            when (operator) {
                AND -> setChecked(R.id.left_button, R.string.condition_operator_and_desc)
                OR -> setChecked(R.id.right_button, R.string.condition_operator_or_desc)
            }
        }
    }
}