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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click

import android.graphics.PointF
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.core.graphics.toPoint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.GESTURE_DURATION_MAX_VALUE
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.PositionSelectorMenu
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.MultiStateButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setEnabled
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setIconBitmap
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setupDescriptions
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ClickDescription
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionClickBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.ImageConditionSelectionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ClickDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ClickViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { clickViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionClickBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionClickBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_click)

                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        listener.onDismissClicked()
                        back()
                    }
                }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
                }
            }

            fieldName.apply {
                setLabel(R.string.generic_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(fieldName.textField)

            fieldPressDuration.apply {
                textField.filters = arrayOf(MinMaxInputFilter(1, GESTURE_DURATION_MAX_VALUE.toInt()))
                setLabel(R.string.input_field_label_click_press_duration)
                setOnTextChangedListener {
                    viewModel.setPressDuration(if (it.isNotEmpty()) it.toString().toLong() else null)
                }
            }
            hideSoftInputOnFocusLoss(fieldPressDuration.textField)

            fieldClickType.apply {
                setTitle(context.getString(R.string.field_click_type_title))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.field_click_type_desc_on_position),
                        context.getString(R.string.field_click_type_desc_on_condition),
                    )
                )
                setButtonConfig(
                    MultiStateButtonConfig(
                        icons = listOf(R.drawable.ic_click_on_condition, R.drawable.ic_condition),
                        singleSelection = true,
                        selectionRequired = true,
                    )
                )
                setOnCheckedListener { checkedId ->
                    viewModel.setClickOnCondition(
                        if (checkedId == 0) Action.Click.PositionType.USER_SELECTED
                        else Action.Click.PositionType.ON_DETECTED_CONDITION
                    )
                }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingAction.collect(::onActionEditingStateChanged) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateClickName) }
                launch { viewModel.nameError.collect(viewBinding.fieldName::setError)}
                launch { viewModel.pressDuration.collect(::updateClickDuration) }
                launch { viewModel.pressDurationError.collect(viewBinding.fieldPressDuration::setError)}
                launch { viewModel.positionStateUi.collect(::updateClickPositionUiState) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.apply {
            monitorSaveButtonView(viewBinding.layoutTopBar.buttonSave)
            monitorFieldSelectPositionView(viewBinding.fieldClickSelection.root)
            monitorFieldPositionTypeItemOnConditionView(viewBinding.fieldClickType.multiStateButton.buttonMiddle)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    private fun onSaveButtonClicked() {
        debounceUserInteraction {
            viewModel.saveLastConfig()
            listener.onConfirmClicked()
            back()
        }
    }

    private fun onDeleteButtonClicked() {
        debounceUserInteraction {
            listener.onDeleteClicked()
            back()
        }
    }

    private fun updateClickName(newName: String?) {
        viewBinding.fieldName.setText(newName)
    }

    private fun updateClickDuration(newDuration: String?) {
        viewBinding.fieldPressDuration.setText(newDuration, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateClickPositionUiState(state: ClickPositionUiState?) {
        state ?: return

        viewBinding.fieldClickType.apply {
            val checkIndex = if (state.positionType == Action.Click.PositionType.USER_SELECTED) 0 else 1
            setChecked(checkIndex)
            setDescription(checkIndex)

            root.visibility = if (state.isTypeFieldVisible) View.VISIBLE else View.GONE
        }

        viewBinding.fieldClickSelection.apply {
            setTitle(state.selectorTitle)
            setDescription(state.selectorDescription)
            setEnabled(state.isSelectorEnabled)
            setIconBitmap(state.selectorBitmap)

            when (state.positionType) {
                Action.Click.PositionType.USER_SELECTED ->
                    setOnClickListener { debounceUserInteraction { showPositionSelector() } }
                Action.Click.PositionType.ON_DETECTED_CONDITION ->
                    setOnClickListener { debounceUserInteraction { showConditionSelector() } }
            }
        }
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun showPositionSelector() {
        viewModel.getEditedClick()?.let { click ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = PositionSelectorMenu(
                    actionDescription = ClickDescription(
                        position = click.getEditionPosition(),
                        pressDurationMs = click.pressDuration ?: 1L,
                    ),
                    onConfirm = { description ->
                        (description as ClickDescription).position?.let {
                            viewModel.setPosition(it.toPoint())
                        }
                    },
                ),
                hideCurrent = true,
            )
        }
    }

    private fun showConditionSelector() =
        overlayManager.navigateTo(
            context = context,
            newOverlay = ImageConditionSelectionDialog(
                conditionList = viewModel.availableConditions.value,
                bitmapProvider = viewModel::getConditionBitmap,
                onConditionSelected = viewModel::setConditionToBeClicked,
            ),
            hideCurrent = false,
        )

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing ClickDialog because there is no action edited")
            finish()
        }
    }

    private fun Action.Click.getEditionPosition(): PointF? =
        if (x == null || y == null) null
        else PointF(x!!.toFloat(), y!!.toFloat())
}

private const val TAG = "ClickDialog"