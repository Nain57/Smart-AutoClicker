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
package com.buzbuz.smartautoclicker.activity.creation

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeDialogNavigationTopBarBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeInputFieldTextBinding
import com.buzbuz.smartautoclicker.databinding.DialogScenarioCreationBinding
import com.buzbuz.smartautoclicker.databinding.IncludeScenarioTypeViewBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_DISABLED_ITEM
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_ENABLED_ITEM

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ScenarioCreationDialog : DialogFragment() {

    companion object {
        /** Tag for pro mode scenario creation dialog fragment. */
        const val FRAGMENT_TAG = "ScenarioCreationDialog"
    }

    private val viewModel: ScenarioCreationViewModel by viewModels()

    /** The view binding on the views of this dialog. */
    private lateinit var viewBinding: DialogScenarioCreationBinding
    /** The Android InputMethodManger, for managing the keyboard dismiss. */
    private lateinit var inputMethodManager: InputMethodManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inputMethodManager = requireContext().getSystemService(InputMethodManager::class.java)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::setInitialScenarioName) }
                launch { viewModel.nameError.collect(viewBinding.scenarioNameInputLayout::setError) }
                launch { viewModel.scenarioTypeSelectionState.collect(::updateTypeSelection) }
                launch { viewModel.creationState.collect(::updateCreationState) }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = DialogScenarioCreationBinding.inflate(layoutInflater).apply {
            layoutTopBar.initTopBar()
            scenarioNameInputLayout.initScenarioNameField()
            scenarioTypeDumb.initScenarioTypeCard(ScenarioTypeSelection.DUMB)
            scenarioTypeSmart.initScenarioTypeCard(ScenarioTypeSelection.SMART)
        }

        return createDialog(viewBinding.root)
    }

    private fun createDialog(root: View): BottomSheetDialog =
        BottomSheetDialog(requireContext()).apply {
            setContentView(root)
            setCancelable(false)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    this@ScenarioCreationDialog.dismiss()
                    true
                } else false
            }

            create()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    private fun IncludeDialogNavigationTopBarBinding.initTopBar() {
        dialogTitle.setText(R.string.dialog_title_add_scenario)

        buttonDismiss.apply {
            visibility = View.VISIBLE
            setOnClickListener { dismiss() }
        }
        buttonSave.apply {
            visibility = View.VISIBLE
            setOnClickListener { viewModel.createScenario(requireContext()) }
        }
        buttonDelete.visibility = View.GONE
    }

    private fun IncludeInputFieldTextBinding.initScenarioNameField() {
        setLabel(R.string.input_field_label_scenario_name)
        setOnTextChangedListener { viewModel.setName(it.toString()) }
        textField.filters = arrayOf<InputFilter>(
            InputFilter.LengthFilter(requireContext().resources.getInteger(R.integer.name_max_length))
        )
    }

    private fun IncludeScenarioTypeViewBinding.initScenarioTypeCard(type: ScenarioTypeSelection) {
        when (type) {
            ScenarioTypeSelection.DUMB -> {
                titleScenarioType.setText(R.string.item_title_dumb_scenario)
                imageScenarioType.setImageResource(R.drawable.ic_dumb)
            }
            ScenarioTypeSelection.SMART -> {
                titleScenarioType.setText(R.string.item_title_smart_scenario)
                imageScenarioType.setImageResource(R.drawable.ic_smart)
            }
        }

        root.setOnClickListener { viewModel.setSelectedType(type) }
    }

    private fun setInitialScenarioName(name: String?) {
        viewBinding.scenarioNameInputLayout.setText(name)
    }

    private fun updateTypeSelection(state: ScenarioTypeSelectionState) {
        viewBinding.apply {
            scenarioTypeDumb.apply {
                root.isChecked = state.selectedItem == ScenarioTypeSelection.DUMB
                titleScenarioType.setText(state.dumbItem.titleRes)
                imageScenarioType.setImageResource(state.dumbItem.iconRes)

                root.setOnClickListener { viewModel.setSelectedType(ScenarioTypeSelection.DUMB) }
            }

            scenarioTypeSmart.apply {
                root.isChecked = state.selectedItem == ScenarioTypeSelection.SMART
                titleScenarioType.setText(state.smartItem.titleRes)
                imageScenarioType.setImageResource(state.smartItem.iconRes)

                if (state.smartItemEnabled) {
                    root.setOnClickListener { viewModel.setSelectedType(ScenarioTypeSelection.SMART) }
                    root.alpha = ALPHA_ENABLED_ITEM
                } else {
                    root.setOnClickListener{ viewModel.onScenarioCountReachedAddCopyClicked(requireContext()) }
                    root.alpha = ALPHA_DISABLED_ITEM
                }
            }

            scenarioTypeDescription.setText(
                when (state.selectedItem) {
                    ScenarioTypeSelection.DUMB -> state.dumbItem.descriptionText
                    ScenarioTypeSelection.SMART -> state.smartItem.descriptionText
                }
            )
        }
    }

    private fun updateCreationState(state: CreationState) {
        viewBinding.layoutTopBar.apply {
            when (state) {
                CreationState.CONFIGURING_INVALID ->
                    setButtonEnabledState(DialogNavigationButton.SAVE, false)
                CreationState.CONFIGURING ->
                    setButtonEnabledState(DialogNavigationButton.SAVE, true)
                CreationState.CREATING ->
                    setButtonEnabledState(DialogNavigationButton.SAVE, false)
                CreationState.SAVED ->
                    dismiss()
            }
        }
    }
}