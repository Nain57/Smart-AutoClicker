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

import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogConfigActionIntentBinding
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.IntentExtra
import com.buzbuz.smartautoclicker.overlays.config.action.intent.activities.ActivitySelectionDialog
import com.buzbuz.smartautoclicker.overlays.bindings.*
import com.buzbuz.smartautoclicker.overlays.config.action.intent.extras.ExtraConfigDialog
import com.buzbuz.smartautoclicker.overlays.base.bindings.*
import com.buzbuz.smartautoclicker.overlays.base.utils.OnAfterTextChangedListener

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class IntentDialog(
    context: Context,
    private val intent: Action.Intent,
    private val onDeleteClicked: (Action.Intent) -> Unit,
    private val onConfirmClicked: (Action.Intent) -> Unit,
) : OverlayDialogController(context) {

    /** The view model for this dialog. */
    private val viewModel: IntentViewModel by lazy {
        ViewModelProvider(this).get(IntentViewModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionIntentBinding
    /** The adapter for the list of extras in advanced configuration mode. */
    private lateinit var extrasAdapter: ExtrasAdapter

    override fun onCreateDialog(): BottomSheetDialog {
        viewModel.setConfiguredIntent(intent)

        viewBinding = DialogConfigActionIntentBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_action_type_intent)

                buttonDismiss.setOnClickListener { dismiss() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
                }
            }

            editNameText.addTextChangedListener(object : OnAfterTextChangedListener() {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setName(s.toString())
                }
            })

            configurationTypeButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                viewModel.setIsAdvancedConfiguration(checkedId == R.id.advanced_configuration_button)
            }
        }

        onCreateSimpleConfigView()
        onCreateAdvancedConfigView()

        return BottomSheetDialog(context).apply {
            setContentView(viewBinding.root)
        }
    }

    private fun onCreateSimpleConfigView() {
        viewBinding.simpleLayout.apply {
            selectApplicationButton.setOnClickListener { showApplicationSelectionDialog() }
            selectedApplicationLayout.root.setOnClickListener { showApplicationSelectionDialog() }
        }
    }

    private fun onCreateAdvancedConfigView() {
        extrasAdapter = ExtrasAdapter(
            addExtraClickedListener = { showExtraDialog(viewModel.getNewExtra()) },
            extraClickedListener = { extra, index ->  showExtraDialog(extra, index) },
        )

        viewBinding.advancedLayout.apply {
            intentSendingTypeButton.apply {
                setButtonsText(
                    left = R.string.dialog_button_intent_start_activity,
                    right = R.string.dialog_button_intent_send_broadcast,
                )
                addOnCheckedListener { checkedId ->
                    viewModel.setIsBroadcast(checkedId == R.id.right_button)
                }
            }

            editActionText.addTextChangedListener(object : OnAfterTextChangedListener() {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setIntentAction(s.toString())
                }
            })

            editFlagsText.addTextChangedListener(object : OnAfterTextChangedListener() {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setIntentFlags(
                        try { if (!s.isNullOrEmpty()) s.toString().toInt() else null }
                        catch (nfe: NumberFormatException) { null }
                    )
                }
            })

            editComponentNameText.addTextChangedListener(object : OnAfterTextChangedListener() {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setComponentName(s.toString())
                }
            })

            extrasList.adapter = extrasAdapter
        }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateClickName) }
                launch { viewModel.isAdvanced.collect(::updateConfigurationType) }
                launch { viewModel.activityInfo.collect(::updateActivityInfo) }
                launch { viewModel.isBroadcast.collect(::updateIsBroadcast) }
                launch { viewModel.action.collect(::updateIntentAction) }
                launch { viewModel.flags.collect(::updateIntentFlags) }
                launch { viewModel.componentName.collect(::updateComponentName) }
                launch { viewModel.extras.collect(extrasAdapter::submitList) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.saveLastConfig()
        onConfirmClicked(viewModel.getConfiguredIntent())
        dismiss()
    }

    private fun onDeleteButtonClicked() {
        onDeleteClicked(intent)
        dismiss()
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameText.setText(newName)
    }

    private fun updateConfigurationType(isAdvanced: Boolean) {
        viewBinding.apply {
            if (isAdvanced) {
                configurationTypeButtonGroup.setChecked(advancedConfigurationButton.id)
                simpleLayout.root.visibility = View.GONE
                advancedLayout.root.visibility = View.VISIBLE
            } else {
                configurationTypeButtonGroup.setChecked(simpleConfigurationButton.id)
                simpleLayout.root.visibility = View.VISIBLE
                advancedLayout.root.visibility = View.GONE
            }
        }
    }

    private fun updateActivityInfo(activityInfo: ActivityDisplayInfo?) {
        viewBinding.simpleLayout.apply {
            if (activityInfo == null) {
                selectedApplicationLayout.root.visibility = View.GONE
                selectApplicationButton.visibility = View.VISIBLE
            } else {
                selectedApplicationLayout.root.visibility = View.VISIBLE
                selectedApplicationLayout.bind(activityInfo)
                selectApplicationButton.visibility = View.GONE
            }
        }
    }

    private fun updateIsBroadcast(isBroadcast: Boolean) {
        viewBinding.apply {
            if (isBroadcast) {
                advancedLayout.intentSendingTypeButton.setChecked(
                    R.id.right_button,
                    R.string.dialog_action_config_intent_advanced_send_type_broadcast,
                )
            } else {
                advancedLayout.intentSendingTypeButton.setChecked(
                    R.id.left_button,
                    R.string.dialog_action_config_intent_advanced_send_type_start_app,
                )
            }
        }
    }

    private fun updateIntentAction(action: String?) {
        viewBinding.advancedLayout.editActionText.setText(action)
    }

    private fun updateIntentFlags(flags: String) {
        viewBinding.advancedLayout.editFlagsText.setText(flags)
    }

    private fun updateComponentName(componentName: String?) {
        viewBinding.advancedLayout.editComponentNameText.setText(componentName)
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun showApplicationSelectionDialog() {
        showSubOverlay(
            overlayController = ActivitySelectionDialog(
                context = context,
                onApplicationSelected = { componentName ->
                    viewModel.setActivitySelected(componentName)
                }
            ),
            false,
        )
    }

    private fun showExtraDialog(extra: IntentExtra<out Any>, index: Int = -1) {
        showSubOverlay(
            overlayController = ExtraConfigDialog(
                context = context,
                extra = extra,
                onConfigComplete = { configuredExtra ->
                    viewModel.addUpdateExtra(configuredExtra, index)
                },
                onDeleteClicked = { if (index != -1) viewModel.deleteExtra(index) }
            ),
            hideCurrent = false,
        )
    }
}