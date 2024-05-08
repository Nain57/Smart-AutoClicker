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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event.config

import android.content.Context
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.DualStateButtonTextConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.texts.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.texts.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setEnabled
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setupDescriptions
import com.buzbuz.smartautoclicker.core.ui.bindings.texts.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.texts.setText
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ContentEventConfigBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay.TryElementOverlayMenu

import kotlinx.coroutines.launch

class EventConfigContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: EventConfigViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { eventConfigViewModel() },
    )

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentEventConfigBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentEventConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            fieldEventName.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setEventName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            dialogController.hideSoftInputOnFocusLoss(fieldEventName.textField)

            fieldConditionOperator.apply {
                setTitle(context.getString(R.string.dropdown_label_condition_operator))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.dropdown_helper_text_condition_and),
                        context.getString(R.string.dropdown_helper_text_condition_or),
                    )
                )
                setButtonConfig(
                    DualStateButtonTextConfig(
                        textLeft = context.getString(R.string.button_text_and),
                        textRight = context.getString(R.string.button_text_or),
                        selectionRequired = true,
                        singleSelection = true,
                    )
                )
                setOnCheckedListener { checkedId ->
                    viewModel.setConditionOperator(if (checkedId == 0) AND else OR)
                }
            }

            fieldIsEnabled.apply {
                setTitle(context.resources.getString(R.string.input_field_label_event_state))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.dropdown_helper_text_event_state_disabled),
                        context.getString(R.string.dropdown_helper_text_event_state_enabled),
                    )
                )
                setOnClickListener(viewModel::toggleEventState)
            }

            fieldTestEvent.apply {
                setTitle(
                    context.getString(
                        R.string.item_title_try_element,
                        context.getString(R.string.dialog_overlay_title_image_event_config),
                    )
                )
                setOnClickListener { debounceUserInteraction { showTryElementMenu() } }
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.eventName.collect(viewBinding.fieldEventName::setText) }
                launch { viewModel.eventNameError.collect(viewBinding.fieldEventName::setError) }
                launch { viewModel.conditionOperator.collect(::updateConditionOperator) }
                launch { viewModel.eventEnabledOnStart.collect(::updateEnabledOnStart) }
                launch { viewModel.shouldShowTryCard.collect(::updateTryFieldVisibility) }
                launch { viewModel.canTryEvent.collect(::updateTryFieldEnabledState) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        //viewModel.monitorConditionOperatorView(viewBinding.conditionsOperatorField.root)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    private fun updateConditionOperator(@ConditionOperator operator: Int) {
        viewBinding.fieldConditionOperator.apply {
            val index = if (operator == AND) 0 else 1
            setChecked(index)
            setDescription(index)
        }
    }

    private fun updateEnabledOnStart(enabledOnStart: Boolean) {
        viewBinding.fieldIsEnabled.apply {
            setChecked(enabledOnStart)
            setDescription(if (enabledOnStart) 1 else 0)
        }
    }

    private fun updateTryFieldVisibility(isEnabled: Boolean) {
        viewBinding.fieldTestEvent.root.visibility = if (isEnabled) View.VISIBLE else View.GONE

    }

    private fun updateTryFieldEnabledState(isEnabled: Boolean) {
        viewBinding.fieldTestEvent.setEnabled(isEnabled)
    }

    private fun onConditionOperatorDropdownItemBound(item: DropdownItem, view: View?) {
        /*if (item == viewModel.conditionAndItem) {
            if (view != null) viewModel.monitorDropdownItemAndView(view)
            else viewModel.stopDropdownItemConditionViewMonitoring()
        }*/
    }


    private fun showTryElementMenu() {
        viewModel.getTryInfo()?.let { (scenario, imageEvent) ->
            dialogController.overlayManager.navigateTo(
                context = context,
                newOverlay = TryElementOverlayMenu(scenario, imageEvent),
                hideCurrent = true,
            )
        }
    }
}