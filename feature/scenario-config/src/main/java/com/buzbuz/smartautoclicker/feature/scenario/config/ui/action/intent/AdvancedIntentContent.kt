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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ContentIntentConfigAdvancedBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.extras.ExtraConfigDialog

import kotlinx.coroutines.launch

class AdvancedIntentContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for the container dialog. */
    private val dialogViewModel: IntentViewModel by lazy {
        ViewModelProvider(dialogController).get(IntentViewModel::class.java)
    }

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentIntentConfigAdvancedBinding
    /** The adapter for the list of extras in advanced configuration mode. */
    private lateinit var extrasAdapter: ExtrasAdapter

    override fun onCreateView(container: ViewGroup): ViewGroup {
        extrasAdapter = ExtrasAdapter(
            addExtraClickedListener = { debounceUserInteraction { showExtraDialog(dialogViewModel.createNewExtra()) } },
            extraClickedListener = { extra -> debounceUserInteraction { showExtraDialog(extra) } },
        )

        viewBinding = ContentIntentConfigAdvancedBinding.inflate(LayoutInflater.from(context)).apply {
            editNameLayout.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { dialogViewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            dialogController.hideSoftInputOnFocusLoss(editNameLayout.textField)

            intentSendingTypeField.setItems(
                label = context.getString(R.string.dropdown_label_intent_sending_type),
                items = dialogViewModel.sendingTypeItems,
                onItemSelected = dialogViewModel::setSendingType,
            )

            editActionLayout.apply {
                setLabel(R.string.input_field_label_intent_action)
                setOnTextChangedListener { dialogViewModel.setIntentAction(it.toString()) }
            }
            dialogController.hideSoftInputOnFocusLoss(editActionLayout.textField)

            editFlagsLayout.apply {
                setLabel(R.string.input_field_label_intent_flags)
                setOnTextChangedListener {
                    dialogViewModel.setIntentFlags(
                        try { if (it.isNotEmpty()) it.toString().toInt() else null }
                        catch (nfe: NumberFormatException) { null }
                    )
                }
            }
            dialogController.hideSoftInputOnFocusLoss(editFlagsLayout.textField)

            editComponentNameLayout.apply {
                setLabel(R.string.input_field_label_intent_component_name)
                setOnTextChangedListener { dialogViewModel.setComponentName(it.toString()) }
            }
            dialogController.hideSoftInputOnFocusLoss(editComponentNameLayout.textField)

            extrasList.adapter = extrasAdapter
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { dialogViewModel.name.collect(::updateClickName) }
                launch { dialogViewModel.nameError.collect(viewBinding.editNameLayout::setError)}
                launch { dialogViewModel.isBroadcast.collect(::updateIsBroadcast) }
                launch { dialogViewModel.action.collect(::updateIntentAction) }
                launch { dialogViewModel.actionError.collect(viewBinding.editActionLayout::setError) }
                launch { dialogViewModel.flags.collect(::updateIntentFlags) }
                launch { dialogViewModel.componentName.collect(::updateComponentName) }
                launch { dialogViewModel.componentNameError.collect(viewBinding.editComponentNameLayout::setError) }
                launch { dialogViewModel.extras.collect(extrasAdapter::submitList) }
            }
        }
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameLayout.setText(newName)
    }

    private fun updateIsBroadcast(item: DropdownItem) {
        viewBinding.intentSendingTypeField.setSelectedItem(item)
    }

    private fun updateIntentAction(action: String?) {
        viewBinding.editActionLayout.setText(action)
    }

    private fun updateIntentFlags(flags: String) {
        viewBinding.editFlagsLayout.setText(flags, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateComponentName(componentName: String?) {
        viewBinding.editComponentNameLayout.setText(componentName)
    }

    private fun showExtraDialog(extra: IntentExtra<out Any>) {
        dialogViewModel.startIntentExtraEdition(extra)

        OverlayManager.getInstance(context).navigateTo(
            context = context,
            newOverlay = ExtraConfigDialog(
                onConfigComplete = dialogViewModel::saveIntentExtraEdition,
                onDeleteClicked = dialogViewModel::deleteIntentExtraEvent,
                onDismissClicked = dialogViewModel::dismissIntentExtraEvent,
            ),
            hideCurrent = false,
        )
    }
}