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
package com.buzbuz.smartautoclicker.overlays.config.action.intent

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ContentIntentConfigAdvancedBinding
import com.buzbuz.smartautoclicker.domain.IntentExtra
import com.buzbuz.smartautoclicker.overlays.base.bindings.*
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.config.action.intent.extras.ExtraConfigDialog

import kotlinx.coroutines.launch

class AdvancedIntentContent : NavBarDialogContent() {

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
            addExtraClickedListener = { showExtraDialog(dialogViewModel.getNewExtra()) },
            extraClickedListener = { extra, index ->  showExtraDialog(extra, index) },
        )

        viewBinding = ContentIntentConfigAdvancedBinding.inflate(LayoutInflater.from(context)).apply {
            editNameLayout.apply {
                setLabel(R.string.dialog_event_config_name_title)
                setOnTextChangedListener { dialogViewModel.setName(it.toString()) }
            }

            intentSendingTypeField.setItems(
                label = context.getString(R.string.dropdown_label_intent_sending_type),
                items = dialogViewModel.sendingTypeItems,
                onItemSelected = dialogViewModel::setSendingType,
            )

            editActionLayout.apply {
                setLabel(R.string.dialog_action_config_intent_advanced_action_title)
                setOnTextChangedListener { dialogViewModel.setIntentAction(it.toString()) }
            }

            editFlagsLayout.apply {
                setLabel(R.string.dialog_action_config_intent_advanced_flags_title)
                setOnTextChangedListener {
                    dialogViewModel.setIntentFlags(
                        try { if (it.isNotEmpty()) it.toString().toInt() else null }
                        catch (nfe: NumberFormatException) { null }
                    )
                }
            }

            editComponentNameLayout.apply {
                setLabel(R.string.dialog_action_config_intent_advanced_comp_name_title)
                setOnTextChangedListener { dialogViewModel.setComponentName(it.toString()) }
            }

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
        viewBinding.editFlagsLayout.setText(flags)
    }

    private fun updateComponentName(componentName: String?) {
        viewBinding.editComponentNameLayout.setText(componentName)
    }

    private fun showExtraDialog(extra: IntentExtra<out Any>, index: Int = -1) {
        dialogController.showSubOverlay(
            overlayController = ExtraConfigDialog(
                context = context,
                extra = extra,
                onConfigComplete = { configuredExtra ->
                    dialogViewModel.addUpdateExtra(configuredExtra, index)
                },
                onDeleteClicked = { if (index != -1) dialogViewModel.deleteExtra(index) }
            ),
            hideCurrent = false,
        )
    }
}