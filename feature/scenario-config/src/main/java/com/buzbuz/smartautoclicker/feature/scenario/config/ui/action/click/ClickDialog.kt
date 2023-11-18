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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.click

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
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.DialogConfigActionClickBinding
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.PositionSelectorMenu
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ClickDescription
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.ConditionSelectionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ClickDialog(
    private val onConfirmClicked: () -> Unit,
    private val onDeleteClicked: () -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ClickViewModel by viewModels()

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionClickBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionClickBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_click)

                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        onDismissClicked()
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

            editNameLayout.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(editNameLayout.textField)

            editPressDurationLayout.apply {
                textField.filters = arrayOf(MinMaxInputFilter(1, GESTURE_DURATION_MAX_VALUE.toInt()))
                setLabel(R.string.input_field_label_click_press_duration)
                setOnTextChangedListener {
                    viewModel.setPressDuration(if (it.isNotEmpty()) it.toString().toLong() else null)
                }
            }
            hideSoftInputOnFocusLoss(editPressDurationLayout.textField)

            clickPositionField.setItems(
                label = context.getString(R.string.dropdown_label_click_position_type),
                items = viewModel.clickTypeItems,
                onItemSelected = viewModel::setClickOnCondition,
                onItemBound = ::onClickOnDropdownItemBound,
            )
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
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError)}
                launch { viewModel.pressDuration.collect(::updateClickDuration) }
                launch { viewModel.pressDurationError.collect(viewBinding.editPressDurationLayout::setError)}
                launch { viewModel.positionStateUi.collect(::updateClickPositionUiState) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.apply {
            monitorSaveButtonView(viewBinding.layoutTopBar.buttonSave)
            monitorSelectPositionView(viewBinding.layoutClickSelector)
            monitorClickOnDropdownView(viewBinding.clickPositionField.root)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    private fun onSaveButtonClicked() {
        debounceUserInteraction {
            viewModel.saveLastConfig()
            onConfirmClicked()
            back()
        }
    }

    private fun onDeleteButtonClicked() {
        debounceUserInteraction {
            onDeleteClicked()
            back()
        }
    }

    private fun onClickOnDropdownItemBound(item: DropdownItem, view: View?) {
        if (item == viewModel.clickTypeItemOnCondition) {
            if (view != null) viewModel.monitorDropdownItemConditionView(view)
            else viewModel.stopDropdownItemConditionViewMonitoring()
        }
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameLayout.setText(newName)
    }

    private fun updateClickDuration(newDuration: String?) {
        viewBinding.editPressDurationLayout.setText(newDuration, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateClickPositionUiState(state: ClickPositionUiState?) {
        state ?: return

        viewBinding.apply {
            clickPositionField.setSelectedItem(state.selectedChoice)
            clickSelectorTitle.text = state.selectorTitle

            if (state.selectorSubText != null) {
                clickSelectorSubtext.text = state.selectorSubText
                clickSelectorSubtext.visibility = View.VISIBLE
            } else {
                clickSelectorSubtext.text = null
                clickSelectorSubtext.visibility = View.GONE
            }

            if (state.selectorIcon != null) {
                clickSelectorConditionIcon.setImageBitmap(state.selectorIcon)
                clickSelectorConditionIcon.visibility = View.VISIBLE
            } else {
                clickSelectorConditionIcon.setImageIcon(null)
                clickSelectorConditionIcon.visibility = View.GONE
            }

            clickSelectorChevron.visibility = if (state.chevronIsVisible) View.VISIBLE else View.GONE

            layoutClickSelector.setOnClickListener {
                debounceUserInteraction {
                    when (state.action) {
                        ClickPositionSelectorAction.NONE -> Unit
                        ClickPositionSelectorAction.SELECT_POSITION -> showPositionSelector()
                        ClickPositionSelectorAction.SELECT_CONDITION -> showConditionSelector()
                    }
                }
            }
        }
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun showPositionSelector() {
        viewModel.getEditedClick()?.let { click ->
            OverlayManager.getInstance(context).navigateTo(
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
        OverlayManager.getInstance(context).navigateTo(
            context = context,
            newOverlay = ConditionSelectionDialog(
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