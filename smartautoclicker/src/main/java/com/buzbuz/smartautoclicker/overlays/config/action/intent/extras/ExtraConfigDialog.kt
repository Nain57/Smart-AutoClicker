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
package com.buzbuz.smartautoclicker.overlays.config.action.intent.extras

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.domain.IntentExtra
import com.buzbuz.smartautoclicker.databinding.DialogConfigActionIntentExtraBinding
import com.buzbuz.smartautoclicker.overlays.base.dialog.MultiChoiceDialog
import com.buzbuz.smartautoclicker.overlays.base.bindings.*

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText

import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * [OverlayDialogController] implementation for displaying an intent extra and providing a button to delete it.
 *
 * This dialog is generic for all extra value types. The UI will change according to the selected type.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param extra the intent extra that will be edited.
 * @param onConfigComplete the listener called when the user presses the ok button.
 * @param onDeleteClicked the listener called when the user presses the delete button.
 */
class ExtraConfigDialog(
    context: Context,
    private val extra: IntentExtra<out Any>,
    private val onConfigComplete: (IntentExtra<out Any>) -> Unit,
    private val onDeleteClicked: (() -> Unit)? = null,
) : OverlayDialogController(context, R.style.AppTheme) {

    /** The view model for the data displayed in this dialog. */
    private val viewModel: ExtraConfigModel by lazy {
        ViewModelProvider(this).get(ExtraConfigModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionIntentExtraBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setConfigExtra(extra)

        viewBinding = DialogConfigActionIntentExtraBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_extra_config)

                buttonDismiss.setOnClickListener { destroy() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
                }
            }

            editKeyLayout.apply {
                setLabel(R.string.dialog_action_config_intent_advanced_extras_config_key)
                setOnTextChangedListener { viewModel.setKey(it.toString()) }
            }

            buttonSelectType.setOnClickListener { showExtraTypeSelectionDialog() }
            textValueType.setOnClickListener { showExtraTypeSelectionDialog() }

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
        onConfigComplete(viewModel.getConfiguredExtra())
        destroy()
    }

    private fun onDeleteButtonClicked() {
        onDeleteClicked?.invoke()
        destroy()
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
        when (valueState) {
            ExtraValueInputState.NoTypeSelected -> toNoExtraTypeSelected()

            is ExtraValueInputState.TypeSelected -> {
                viewBinding.apply {
                    layoutValueInput.visibility = View.VISIBLE
                    buttonSelectType.visibility = View.GONE
                    textValueType.apply {
                        visibility = View.VISIBLE
                        setText(valueState.typeText)
                    }
                }

                when (valueState) {
                    is ExtraValueInputState.BooleanInputTypeSelected -> toExtraTypeBooleanSelected(valueState)
                    is ExtraValueInputState.TextInputTypeSelected -> toExtraTypeTextInputSelected(valueState)
                }
            }
        }
    }

    private fun toNoExtraTypeSelected() {
        viewBinding.apply {
            textValueType.visibility = View.GONE
            layoutValueInput.visibility = View.GONE
            buttonSelectType.visibility = View.VISIBLE
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
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidExtra)
    }

    private fun showExtraTypeSelectionDialog() {
        showSubOverlay(
            overlayController = MultiChoiceDialog(
                context = context,
                dialogTitleText = R.string.dialog_action_config_intent_advanced_extras_config_value_type,
                choices = ExtraTypeChoice.getAllChoices(),
                onChoiceSelected = viewModel::setType
            ),
            hideCurrent = false,
        )
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