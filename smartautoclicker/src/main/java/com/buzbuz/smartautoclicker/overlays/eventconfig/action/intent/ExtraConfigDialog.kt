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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.intent

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.view.View

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.domain.IntentExtra
import com.buzbuz.smartautoclicker.databinding.DialogIntentExtraConfigBinding
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

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
    extra: IntentExtra<out Any>,
    private val onConfigComplete: (IntentExtra<out Any>) -> Unit,
    private val onDeleteClicked: (() -> Unit)? = null,
) : OverlayDialogController(context) {

    /** The view model for the data displayed in this dialog. */
    private var viewModel: ExtraConfigModel? = ExtraConfigModel(context).apply {
        attachToLifecycle(this@ExtraConfigDialog)
        setConfigExtra(extra)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogIntentExtraConfigBinding

    /** The currently selected type for the extra value. */
    private var currentType: KClass<out Any>? = null

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogIntentExtraConfigBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_action_config_intent_advanced_extras_config_title)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)

        if (onDeleteClicked != null) {
            builder.setNeutralButton(R.string.dialog_condition_delete) { _, _ -> onDeleteClicked.invoke() }
        }

        return builder
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDialogCreated(dialog: AlertDialog) {
        viewBinding.apply {
            root.setOnTouchListener(hideSoftInputTouchListener)

            editKey.apply {
                setSelectAllOnFocus(true)
                addTextChangedListener(object : OnAfterTextChangedListener() {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel?.setKey(text.toString())
                    }
                })
            }

            editValue.apply {
                setSelectAllOnFocus(true)
                addTextChangedListener(object : OnAfterTextChangedListener() {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel?.setValue(text.toString())
                    }
                })
            }

            editBooleanValue.setOnClickListener {
                viewModel?.toggleBooleanValue()
            }

            textValueType.setOnClickListener {
                showSubOverlay(
                    ExtraTypeSelectionDialog(
                        context = context,
                        onTypeSelected = { type -> viewModel?.setType(type) }
                    )
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel?.key?.collect { key ->
                        viewBinding.editKey.apply {
                            setText(key)
                            setSelection(key?.length ?: 0)
                        }
                    }
                }

                launch {
                    viewModel?.valueInputState?.collect { typeState ->
                        updateValueInputViews(typeState)
                    }
                }

                launch {
                    viewModel?.isExtraValid?.collect { isExtraValid ->
                        changeButtonState(
                            button = dialog.getButton(AlertDialog.BUTTON_POSITIVE),
                            visibility = if (isExtraValid) View.VISIBLE else View.INVISIBLE,
                            listener = { onOkClicked() }
                        )
                    }
                }
            }
        }
    }

    /**
     * Update the value input views according to the new state.
     * For each extra value type, a different configuration is applied (different IME flags, filters ...) to provide
     * the correct experience to the user.
     *
     * @param state the new state of the input views.
     */
    private fun updateValueInputViews(state: ExtraValueInputState) = viewBinding.apply {
        textValueType.text = state.typeSelectionText

        when (state) {
            is ExtraValueInputState.NoTypeSelected -> {
                layoutValue.visibility = View.GONE
                currentType = null
            }

            is ExtraValueInputState.BooleanInputTypeSelected -> {
                layoutValue.visibility = View.VISIBLE
                editValue.visibility = View.GONE
                editBooleanValue.apply {
                    visibility = View.VISIBLE
                    setText(
                        if (state.isTrue) R.string.dialog_action_config_intent_advanced_extras_config_boolean_true
                        else R.string.dialog_action_config_intent_advanced_extras_config_boolean_false
                    )
                }
                currentType = Boolean::class
            }

            is ExtraValueInputState.TextInputTypeSelected -> {
                layoutValue.visibility = View.VISIBLE
                editValue.apply {
                    visibility = View.VISIBLE
                    if (currentType != state.value::class) {
                        setText(state.valueStr)
                        inputType = state.inputType
                        filters = state.inputFilter?.let { arrayOf(it) } ?: emptyArray()
                    }
                }
                editBooleanValue.visibility = View.GONE
                currentType = state.value::class
            }
        }
    }

    /** Called when the user press OK. */
    private fun onOkClicked() {
        viewModel?.let {
            onConfigComplete(it.getConfiguredExtra())
        }

        dismiss()
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
    }
}