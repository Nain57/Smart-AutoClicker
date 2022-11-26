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

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ContentEventConfigBinding
import com.buzbuz.smartautoclicker.overlays.base.bindings.*
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.config.event.EventDialogViewModel

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
            eventNameInputLayout.apply {
                setLabel(R.string.dialog_event_config_name_title)
                setOnTextChangedListener { viewModel.setEventName(it.toString()) }
            }

            conditionsOperatorField.setItems(
                label = context.getString(R.string.dropdown_label_condition_operator),
                items = viewModel.conditionOperatorsItems,
                onItemSelected = viewModel::setConditionOperator,
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
            }
        }
    }

    private fun updateEventName(name: String?) {
        viewBinding.eventNameInputLayout.setText(name)
    }

    private fun updateConditionOperator(operatorItem: DropdownItem) {
        viewBinding.conditionsOperatorField.setSelectedItem(operatorItem)
    }
}