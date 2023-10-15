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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.extras

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.DialogConfigActionIntentExtraBinding

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText

import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * [OverlayDialog] implementation for displaying an intent extra and providing a button to delete it.
 *
 * This dialog is generic for all extra value types. The UI will change according to the selected type.
 *
 * @param onConfigComplete the listener called when the user presses the ok button.
 * @param onDeleteClicked the listener called when the user presses the delete button.
 */
class ExtraConfigDialog(
    private val onConfigComplete: () -> Unit,
    private val onDeleteClicked: () -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for the data displayed in this dialog. */
    private val viewModel: ExtraConfigModel by lazy {
        ViewModelProvider(this).get(ExtraConfigModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionIntentExtraBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionIntentExtraBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_extra_config)

                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        onDismissClicked()
                        back()
                    }
                }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { debounceUserInteraction { onSaveButtonClicked() } }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { debounceUserInteraction { onDeleteButtonClicked() } }
                }
            }

            editKeyLayout.apply {
                setLabel(R.string.input_field_label_intent_extra_key)
                setOnTextChangedListener { viewModel.setKey(it.toString()) }
            }
            hideSoftInputOnFocusLoss(editKeyLayout.textField)

            extraValueTypeField.setItems(
                label = context.getString(R.string.dropdown_label_intent_extra_value_type),
                items = viewModel.extraTypeDropdownItems,
                onItemSelected = viewModel::setType,
            )

            editValueField.setOnTextChangedListener { viewModel.setValue(it.toString()) }
            editBooleanValueField.setItems(
                items = viewModel.booleanItems,
                onItemSelected = viewModel::setBooleanValue,
            )
        }

        return viewBinding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingExtra.collect(::onExtraEditingStateChanged) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.key.collect(::updateExtraKey) }
                launch { viewModel.keyError.collect(viewBinding.editKeyLayout::setError) }
                launch { viewModel.valueInputState.collect(::updateExtraValue) }
                launch { viewModel.valueError.collect(viewBinding.editValueField::setError) }
                launch { viewModel.isExtraValid.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        onConfigComplete()
        back()
    }

    private fun onDeleteButtonClicked() {
        onDeleteClicked()
        back()
    }

    private fun updateExtraKey(newKey: String?) {
        viewBinding.editKeyLayout.setText(newKey)
    }

    /**
     * Update the value input views according to the new state.
     * For each extra value type, a different configuration is applied (different IME flags, filters ...) to provide
     * the correct experience to the user.
     *
     * @param valueState the new state of the input views.
     */
    private fun updateExtraValue(valueState: ExtraValueInputState) {
        viewBinding.apply {
            layoutValueInput.visibility = View.VISIBLE
            buttonSelectType.visibility = View.GONE
            extraValueTypeField.setSelectedItem(valueState.typeItem)
        }

        when (valueState) {
            is ExtraValueInputState.BooleanInputTypeSelected -> toExtraTypeBooleanSelected(valueState)
            is ExtraValueInputState.TextInputTypeSelected -> toExtraTypeTextInputSelected(valueState)
        }
    }

    private fun toExtraTypeBooleanSelected(state: ExtraValueInputState.BooleanInputTypeSelected) {
        viewBinding.editValueField.apply {
            root.visibility = View.GONE
            textField.clearInputClass()
        }

        viewBinding.editBooleanValueField.apply {
            root.visibility = View.VISIBLE
            setSelectedItem(state.value)
        }
    }

    private fun toExtraTypeTextInputSelected(state: ExtraValueInputState.TextInputTypeSelected) {
        viewBinding.editValueField.apply {
            root.visibility = View.VISIBLE
            textField.setInputClass(state)
        }

        viewBinding.editBooleanValueField.root.visibility = View.GONE
    }

    private fun updateSaveButton(isValidExtra: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton.SAVE, isValidExtra)
    }

    private fun onExtraEditingStateChanged(isEditingExtra: Boolean) {
        if (!isEditingExtra) {
            Log.e(TAG, "Closing ExtraConfigDialog because there is no intent extra edited")
            finish()
        }
    }

    private fun TextInputEditText.clearInputClass() {
        tag = null
    }

    private fun TextInputEditText.setInputClass(state: ExtraValueInputState.TextInputTypeSelected) {
        val currentClass = tag as KClass<out Any>?
        val newClass = state.value::class

        if (newClass != currentClass) {
            setText(state.valueStr)
            inputType = state.inputType
            filters = state.inputFilter?.let { arrayOf(it) } ?: emptyArray()

            tag = newClass
        }
    }
}

private const val TAG = "ExtraConfigDialog"