/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.config

import android.content.Context
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.viewModels
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ContentEventConfigBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_DISABLED_ITEM
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_ENABLED_ITEM

import kotlinx.coroutines.launch

class EventConfigContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: EventConfigViewModel by viewModels()

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentEventConfigBinding

    private var billingFlowStarted: Boolean = false

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentEventConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            eventNameInputLayout.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setEventName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            dialogController.hideSoftInputOnFocusLoss(eventNameInputLayout.textField)

            conditionsOperatorField.setItems(
                label = context.getString(R.string.dropdown_label_condition_operator),
                items = viewModel.conditionOperatorsItems,
                onItemSelected = viewModel::setConditionOperator,
                onItemBound = ::onConditionOperatorDropdownItemBound,
            )
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        // When the billing flow is not longer displayed, restore the dialogs states
        lifecycleScope.launch {
            repeatOnLifecycle((Lifecycle.State.CREATED)) {
                viewModel.isBillingFlowDisplayed.collect { isDisplayed ->
                    if (!isDisplayed) {
                        if (billingFlowStarted) {
                            dialogController.show()
                            billingFlowStarted = false
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.eventNameError.collect(viewBinding.eventNameInputLayout::setError) }
                launch { viewModel.eventName.collect(::updateEventName) }
                launch { viewModel.conditionOperator.collect(::updateConditionOperator) }
                launch { viewModel.eventStateDropdownState.collect(::updateEventStateDropdown) }
                launch { viewModel.eventStateItem.collect(::updateEventState) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorConditionOperatorView(viewBinding.conditionsOperatorField.root)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    private fun onConditionOperatorDropdownItemBound(item: DropdownItem, view: View?) {
        if (item == viewModel.conditionAndItem) {
            if (view != null) viewModel.monitorDropdownItemAndView(view)
            else viewModel.stopDropdownItemConditionViewMonitoring()
        }
    }

    private fun updateEventName(name: String?) {
        viewBinding.eventNameInputLayout.setText(name)
    }

    private fun updateConditionOperator(operatorItem: DropdownItem) {
        viewBinding.conditionsOperatorField.setSelectedItem(operatorItem)
    }

    private fun updateEventStateDropdown(dropdownState: EventStateDropdownUiState) {
        viewBinding.enabledOnStartField.setItems(
            label = context.resources.getString(R.string.input_field_label_event_state),
            items = dropdownState.items,
            enabled = dropdownState.enabled,
            disabledIcon = dropdownState.disabledIcon,
            onItemSelected = viewModel::setEventState,
            onDisabledClick = {
                billingFlowStarted = true
                dialogController.hide()
                viewModel.onEventStateClickedWithoutProMode(context)
           },
        )

        viewBinding.enabledOnStartField.root.alpha =
            if (dropdownState.enabled) ALPHA_ENABLED_ITEM
            else ALPHA_DISABLED_ITEM
    }

    private fun updateEventState(stateItem: DropdownItem) {
        viewBinding.enabledOnStartField.setSelectedItem(stateItem)
    }
}