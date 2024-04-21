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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image

import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Color
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.extensions.showAsOverlay
import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectorState
import com.buzbuz.smartautoclicker.core.ui.bindings.setElementTypeName
import com.buzbuz.smartautoclicker.core.ui.bindings.setEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.setText
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogConfigConditionImageBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay.TryElementOverlayMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ImageConditionDialog(
    private val listener: OnConditionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ImageConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { imageConditionViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigConditionImageBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigConditionImageBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_condition_config)

                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        listener.onDismissClicked()
                        back()
                    }
                }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        debounceUserInteraction {
                            listener.onConfirmClicked()
                            back()
                        }
                    }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { debounceUserInteraction { onDeleteClicked() } }
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

            conditionDetectionType.setItems(
                label = context.getString(R.string.dropdown_label_condition_detection_type),
                items = viewModel.detectionTypeItems,
                onItemSelected = viewModel::setDetectionType,
                onItemBound = ::onDetectionTypeDropdownItemBound,
                onSelectorClicked = ::showDetectionAreaSelector,
            )

            conditionShouldAppear.setItems(
                label = context.getString(R.string.dropdown_label_condition_visibility),
                items = viewModel.shouldBeDetectedItems,
                onItemSelected = viewModel::setShouldBeDetected,
            )

            seekbarDiffThreshold.apply {
                setLabelFormatter { "$it %" }
                addOnChangeListener {  _, value, fromUser ->
                    if (fromUser) viewModel.setThreshold(value.roundToInt())
                }
            }


            tryConditionCard.apply {
                setElementTypeName(context.getString(R.string.dialog_overlay_title_condition_config))
                setOnClickListener { debounceUserInteraction { showTryElementMenu() } }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingCondition.collect(::onConditionEditingStateChanged) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateConditionName) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError) }
                launch { viewModel.conditionBitmap.collect(::updateConditionBitmap) }
                launch { viewModel.shouldBeDetected.collect(::updateShouldBeDetected) }
                launch { viewModel.detectionType.collect(::updateConditionType) }
                launch { viewModel.threshold.collect(::updateThreshold) }
                launch { viewModel.conditionCanBeSaved.collect(::updateSaveButton) }
                launch { viewModel.canTryCondition.collect(viewBinding.tryConditionCard::setEnabledState) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorSaveButtonView(viewBinding.layoutTopBar.buttonSave)
        viewModel.monitorDetectionTypeDropdownView(viewBinding.conditionDetectionType.textLayout)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    private fun onDeleteClicked() {
        if (viewModel.isConditionRelatedToClick()) showAssociatedActionWarning()
        else confirmDelete()
    }

    private fun onDetectionTypeDropdownItemBound(item: DropdownItem, view: View?) {
        if (item == viewModel.detectionTypeScreen) {
            if (view != null) viewModel.monitorDropdownItemWholeScreenView(view)
            else viewModel.stopDropdownItemWholeScreenViewMonitoring()
        }
    }

    private fun updateConditionName(newName: String?) {
        viewBinding.editNameLayout.setText(newName)
    }

    private fun updateConditionBitmap(newBitmap: Bitmap?) {
        if (newBitmap != null) {
            viewBinding.imageCondition.setImageBitmap(newBitmap)
        } else {
            viewBinding.imageCondition.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_cancel)?.apply {
                    setTint(Color.RED)
                }
            )
        }
    }

    private fun updateShouldBeDetected(newValue: DropdownItem) {
        viewBinding.conditionShouldAppear.setSelectedItem(newValue)
    }

    private fun updateConditionType(newState: DetectionTypeState) {
        viewBinding.conditionDetectionType.apply {
            setSelectedItem(newState.dropdownItem)
            setSelectorState(newState.selectorState)
        }
    }

    private fun updateThreshold(newThreshold: Int) {
        viewBinding.apply {
            val isNotInitialized = seekbarDiffThreshold.value == 0f
            seekbarDiffThreshold.value = newThreshold.toFloat()

            if (isNotInitialized) {
                seekbarDiffThreshold.valueFrom = 0f
                seekbarDiffThreshold.valueTo = MAX_THRESHOLD
            }
        }
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun showAssociatedActionWarning() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.message_condition_delete_associated_action)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                confirmDelete()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .showAsOverlay()
    }

    private fun showDetectionAreaSelector() {
        debounceUserInteraction {
            overlayManager.navigateTo(
                context = context,
                newOverlay = ImageConditionAreaSelectorMenu(
                    onAreaSelected = viewModel::setDetectionArea,
                ),
                hideCurrent = true,
            )
        }
    }

    private fun confirmDelete() {
        listener.onDeleteClicked()
        back()
    }

    private fun onConditionEditingStateChanged(isEditingCondition: Boolean) {
        if (!isEditingCondition) {
            Log.e(TAG, "Closing ConditionDialog because there is no condition edited")
            finish()
        }
    }

    private fun showTryElementMenu() {
        viewModel.getTryInfo()?.let { (scenario, imageCondition) ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = TryElementOverlayMenu(scenario, imageCondition),
                hideCurrent = true,
            )
        }
    }
}

private const val TAG = "ConditionDialog"