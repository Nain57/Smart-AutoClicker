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
package com.buzbuz.smartautoclicker.overlays.config.event.config

import android.text.InputFilter
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.bindings.DropdownItem
import com.buzbuz.smartautoclicker.baseui.bindings.setItems
import com.buzbuz.smartautoclicker.baseui.bindings.setLabel
import com.buzbuz.smartautoclicker.baseui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.baseui.bindings.setSelectedItem
import com.buzbuz.smartautoclicker.baseui.bindings.setText
import com.buzbuz.smartautoclicker.databinding.ContentEventConfigBinding
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.utils.setError

import kotlinx.coroutines.launch

class EventConfigContent : NavBarDialogContent() {

    /** View model for this content. */
    private val viewModel: EventConfigViewModel by lazy {
        ViewModelProvider(this).get(EventConfigViewModel::class.java)
    }

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentEventConfigBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentEventConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            eventNameInputLayout.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setEventName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }

            conditionsOperatorField.setItems(
                label = context.getString(R.string.dropdown_label_condition_operator),
                items = viewModel.conditionOperatorsItems,
                onItemSelected = viewModel::setConditionOperator,
            )

            enabledOnStartField.setItems(
                label = context.resources.getString(R.string.input_field_label_event_state),
                items = viewModel.eventStateItems,
                onItemSelected = viewModel::setEventState,
            )
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.eventNameError.collect(viewBinding.eventNameInputLayout::setError) }
                launch { viewModel.eventName.collect(::updateEventName) }
                launch { viewModel.conditionOperator.collect(::updateConditionOperator) }
                launch { viewModel.eventStateItem.collect(::updateEventState) }
            }
        }
    }

    private fun updateEventName(name: String?) {
        viewBinding.eventNameInputLayout.setText(name)
    }

    private fun updateConditionOperator(operatorItem: DropdownItem) {
        viewBinding.conditionsOperatorField.setSelectedItem(operatorItem)
    }

    private fun updateEventState(stateItem: DropdownItem) {
        viewBinding.enabledOnStartField.setSelectedItem(stateItem)
    }
}