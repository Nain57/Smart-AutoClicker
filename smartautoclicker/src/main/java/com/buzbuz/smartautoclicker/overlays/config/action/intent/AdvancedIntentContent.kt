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
package com.buzbuz.smartautoclicker.overlays.config.action.intent

import android.text.Editable
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ContentIntentConfigAdvancedBinding
import com.buzbuz.smartautoclicker.domain.IntentExtra
import com.buzbuz.smartautoclicker.overlays.base.bindings.addOnCheckedListener
import com.buzbuz.smartautoclicker.overlays.base.bindings.setButtonsText
import com.buzbuz.smartautoclicker.overlays.base.bindings.setChecked
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.baseui.OnAfterTextChangedListener
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
            editNameText.addTextChangedListener(OnAfterTextChangedListener {
                dialogViewModel.setName(it.toString())
            })

            intentSendingTypeButton.apply {
                setButtonsText(
                    left = R.string.dialog_button_intent_start_activity,
                    right = R.string.dialog_button_intent_send_broadcast,
                )
                addOnCheckedListener { checkedId ->
                    dialogViewModel.setIsBroadcast(checkedId == R.id.right_button)
                }
            }

            editActionText.addTextChangedListener(OnAfterTextChangedListener {
                dialogViewModel.setIntentAction(it.toString())
            })

            editFlagsText.addTextChangedListener(OnAfterTextChangedListener {
                dialogViewModel.setIntentFlags(
                    try { if (it.isNotEmpty()) it.toString().toInt() else null }
                    catch (nfe: NumberFormatException) { null }
                )
            })

            editComponentNameText.addTextChangedListener(OnAfterTextChangedListener {
                dialogViewModel.setComponentName(it.toString())
            })

            extrasList.adapter = extrasAdapter
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { dialogViewModel.name.collect(::updateClickName) }
                launch { dialogViewModel.isBroadcast.collect(::updateIsBroadcast) }
                launch { dialogViewModel.action.collect(::updateIntentAction) }
                launch { dialogViewModel.flags.collect(::updateIntentFlags) }
                launch { dialogViewModel.componentName.collect(::updateComponentName) }
                launch { dialogViewModel.extras.collect(extrasAdapter::submitList) }
            }
        }
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameText.setText(newName)
    }

    private fun updateIsBroadcast(isBroadcast: Boolean) {
        viewBinding.apply {
            if (isBroadcast) {
                intentSendingTypeButton.setChecked(
                    R.id.right_button,
                    R.string.dialog_action_config_intent_advanced_send_type_broadcast,
                )
            } else {
                intentSendingTypeButton.setChecked(
                    R.id.left_button,
                    R.string.dialog_action_config_intent_advanced_send_type_start_app,
                )
            }
        }
    }

    private fun updateIntentAction(action: String?) {
        viewBinding.editActionText.setText(action)
    }

    private fun updateIntentFlags(flags: String) {
        viewBinding.editFlagsText.setText(flags)
    }

    private fun updateComponentName(componentName: String?) {
        viewBinding.editComponentNameText.setText(componentName)
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