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

import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.actions.GESTURE_DURATION_MAX_VALUE
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.PositionSelectorMenu
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckboxClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setup
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.ClickDescription
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigActionClickBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.bindings.bindCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.bindings.enableClearCounterOnType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.bindings.setPositionValue
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.offset.GestureOffsetDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.UiGesturePosition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.selection.ScreenConditionSelectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.selection.CounterSelectionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.getOffsetDescription

class ClickDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CLICK.name

    /** The view model for this dialog. */
    private val viewModel: ClickViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { clickViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionClickBinding
    /** Loading of the bitmap of the condition to click on, if any. */
    private var conditionBitmapLoadingJob: Job? = null

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigActionClickBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_click)

                buttonDismiss.setDebouncedOnClickListener { back() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onDeleteButtonClicked() }
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

            fieldPosition.fieldX.apply {
                setup(
                    label = R.string.field_title_position_x,
                    icon = R.drawable.ic_append_counter,
                    disableInputWithCheckbox = false,
                )
                enableClearCounterOnType()
                setOnTextChangedListener { viewModel.setPositionX(it.toString()) }
                setOnCheckboxClickedListener {
                    showCounterSelectionDialog(viewModel::setPositionXCounter)
                }
            }
            hideSoftInputOnFocusLoss(fieldPosition.fieldX.textField)

            fieldPosition.fieldY.apply {
                setup(
                    label = R.string.field_title_position_y,
                    icon = R.drawable.ic_append_counter,
                    disableInputWithCheckbox = false,
                )
                enableClearCounterOnType()
                setOnTextChangedListener { viewModel.setPositionY(it.toString()) }
                setOnCheckboxClickedListener {
                    showCounterSelectionDialog(viewModel::setPositionYCounter)
                }
            }
            hideSoftInputOnFocusLoss(fieldPosition.fieldY.textField)

            fieldPosition.buttonSelectPosition.setDebouncedOnClickListener { showPositionSelector() }
            fieldPosition.buttonSelectCondition.setDebouncedOnClickListener { onSelectConditionClicked() }
            fieldPosition.conditionSplash.conditionCard.setDebouncedOnClickListener {
                val position = viewModel.uiState.value?.positionState?.position
                if (position is UiGesturePosition.Condition && position.condition != null && !position.inError) {
                    showConditionSelector()
                }
            }

            fieldClickOffset.apply {
                setTitle(context.getString(R.string.field_click_offset_title))
                setOnClickListener { showClickOffsetDialog() }
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
                launch { viewModel.uiState.collect(::updateUi) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorViews(
            selectConditionButton = viewBinding.fieldPosition.buttonSelectCondition,
            selectPositionButton = viewBinding.fieldPosition.buttonSelectPosition,
            saveButton = viewBinding.layoutTopBar.buttonSave,
        )
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog {
                listener.onDismissClicked()
                super.back()
            }
            return
        }

        listener.onDismissClicked()
        super.back()
    }

    private fun onSaveButtonClicked() {
        viewModel.saveLastConfig()
        listener.onConfirmClicked()
        super.back()
    }

    private fun onDeleteButtonClicked() {
        listener.onDeleteClicked()
        super.back()
    }

    private fun updateUi(state: ClickUiState?) {
        state ?: return

        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, state.canBeSaved)
            fieldName.setText(state.name)
            fieldName.setError(state.nameError)
            fieldPressDuration.setText(state.pressDuration, InputType.TYPE_CLASS_NUMBER)
            fieldPressDuration.setError(state.pressDurationError)
        }

        updateClickPositionUiState(state.positionState)
    }

    private fun updateClickPositionUiState(state: ClickPositionUiState?) {
        state ?: return

        viewBinding.fieldPosition.apply {
            buttonSelectCondition.isVisible = state.isConditionSelectionVisible

            when (val position = state.position) {
                is UiGesturePosition.Condition -> {
                    buttonSelectCondition.setText(R.string.button_title_condition_remove)
                    buttonSelectCondition.setIconResource(R.drawable.ic_cancel)

                    conditionBitmapLoadingJob?.cancel()
                    conditionBitmapLoadingJob = conditionSplash.bindCondition(
                        condition = position.condition,
                        inError = position.inError,
                        bitmapProvider = { condition, onBitmapLoaded ->
                            viewModel.getConditionBitmap(condition, onBitmapLoaded)
                        },
                    )
                }

                is UiGesturePosition.Position -> {
                    conditionSplash.root.isVisible = false
                    buttonSelectCondition.setText(R.string.button_title_condition_pick)
                    buttonSelectCondition.setIconResource(R.drawable.ic_screen_event)
                    fieldX.setPositionValue(position.xValue)
                    fieldY.setPositionValue(position.yValue)
                }
            }
        }

        viewBinding.fieldClickOffset.apply {
            setDescription(state.clickOffsetDescription)

            root.isVisible = state.isClickOffsetVisible
            viewBinding.dividerClickOffset.isVisible = state.isClickOffsetVisible
        }
    }

    private fun onSelectConditionClicked() {
        if (viewModel.uiState.value?.positionState?.position is UiGesturePosition.Condition) {
            viewModel.removeClickOnCondition()
            return
        }

        if (viewModel.shouldSelectCondition()) showConditionSelector()
        else viewModel.setClickOnDetectedCondition()
    }

    private fun showPositionSelector() {
        viewModel.getEditedClick()?.let { click ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = PositionSelectorMenu(
                    tutorialMonitoringTag = MonitoredOverlayType.CLICK_POSITION.name,
                    itemBriefDescription = ClickDescription(
                        position = click.getLiteralPosition()?.toPointF(),
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
            newOverlay = ScreenConditionSelectionDialog(
                conditionList = viewModel.uiState.value?.availableConditions ?: emptyList(),
                onConditionSelected = viewModel::setConditionToBeClicked,
            ),
            hideCurrent = false,
        )

    private fun showCounterSelectionDialog(onCounterSelected: (String) -> Unit) =
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterSelectionDialog(onCounterSelected),
            hideCurrent = false,
        )

    private fun showClickOffsetDialog() =
        overlayManager.navigateTo(
            context = context,
            newOverlay = GestureOffsetDialog(
                currentOffset = viewModel.getClickOffset(),
                onNewOffset = viewModel::setClickOffset,
            ),
            hideCurrent = false,
        )

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing ClickDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "ClickDialog"
