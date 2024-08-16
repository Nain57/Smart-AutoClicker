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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event

import android.content.DialogInterface
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.extensions.showAsOverlay
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.DualStateButtonTextConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setEnabled
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setupDescriptions
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogEventConfigBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief.SmartActionsBriefMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.brief.ImageConditionsBriefMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.TriggerConditionListDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay.TryElementOverlayMenu

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import kotlinx.coroutines.launch

class EventDialog(
    private val onConfigComplete: () -> Unit,
    private val onDelete: () -> Unit,
    private val onDismiss: () -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** View model for this dialog. */
    private val viewModel: EventDialogViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { eventDialogViewModel() },
    )

    private lateinit var viewBinding: DialogEventConfigBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogEventConfigBinding.inflate(LayoutInflater.from(context)).apply {
            setupNavBar()
            setupEventProperties()
            setupActionCard()
            setupConditionsCard()
        }

        return viewBinding.root
    }

    private fun DialogEventConfigBinding.setupNavBar() = layoutTopBar.apply {
        setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
        setButtonVisibility(DialogNavigationButton.DELETE, View.VISIBLE)

        dialogTitle.setText(
            if (viewModel.isConfiguringScreenEvent()) R.string.dialog_title_image_event
            else R.string.dialog_title_trigger_event
        )

        buttonDismiss.setDebouncedOnClickListener {
            onDismiss()
            back()
        }
        buttonSave.setDebouncedOnClickListener {
            onConfigComplete()
            back()
        }

        buttonDelete.setDebouncedOnClickListener {
            onDeleteButtonClicked()
        }
    }

    private fun DialogEventConfigBinding.setupEventProperties() {
        fieldEventName.apply {
            setLabel(R.string.generic_name)
            setOnTextChangedListener { viewModel.setEventName(it.toString()) }
            textField.filters = arrayOf<InputFilter>(
                InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
            )
        }
        hideSoftInputOnFocusLoss(fieldEventName.textField)

        fieldIsEnabled.apply {
            setTitle(context.resources.getString(R.string.field_event_state_title))
            setupDescriptions(
                listOf(
                    context.getString(R.string.field_event_state_desc_disabled),
                    context.getString(R.string.field_event_state_desc_enabled),
                )
            )
            setOnClickListener(viewModel::toggleEventState)
        }

        fieldTestEvent.apply {
            setTitle(
                context.getString(
                    R.string.item_title_try_element,
                    context.getString(R.string.dialog_title_image_event),
                )
            )
            setOnClickListener { debounceUserInteraction { showTryElementMenu() } }
        }
    }

    private fun DialogEventConfigBinding.setupConditionsCard() {
        if (viewModel.isConfiguringScreenEvent()) {
            fieldTriggerConditionsSelector.root.visibility = View.GONE
            fieldImageConditionsSelector.apply {
                root.visibility = View.VISIBLE
                setTitle(
                    titleRes = R.string.menu_item_title_conditions,
                    emptyTitleRes = R.string.message_empty_screen_condition_list_title,
                )
                setEmptyDescription(R.string.message_empty_screen_condition_list_desc)

                setAdapter(
                    EventImageConditionsAdapter(
                        itemClickedListener = ::showImageConditionsBriefMenu,
                        bitmapProvider = viewModel::getConditionBitmap,
                    ),
                )

                setOnClickListener { debounceUserInteraction { showImageConditionsBriefMenu() } }
            }
        } else {
            fieldImageConditionsSelector.root.visibility = View.GONE
            fieldTriggerConditionsSelector.apply {
                root.visibility = View.VISIBLE
                setTitle(
                    titleRes = R.string.menu_item_title_conditions,
                    emptyTitleRes = R.string.message_empty_trigger_condition_list_title,
                )
                setEmptyDescription(R.string.message_empty_trigger_condition_list_desc)

                setAdapter(EventChildrenCardsAdapter { showTriggerConditionsDialog() })
                setOnClickListener { debounceUserInteraction { showTriggerConditionsDialog() } }
            }
        }

        fieldConditionsOperator.apply {
            setTitle(context.getString(R.string.field_operator_title))
            setupDescriptions(
                listOf(
                    context.getString(R.string.field_operator_desc_and),
                    context.getString(R.string.field_operator_desc_or),
                )
            )
            setButtonConfig(
                DualStateButtonTextConfig(
                    textLeft = context.getString(R.string.field_operator_button_and),
                    textRight = context.getString(R.string.field_operator_button_or),
                    selectionRequired = true,
                    singleSelection = true,
                )
            )
            setOnCheckedListener { checkedId ->
                viewModel.setConditionOperator(if (checkedId == 0) AND else OR)
            }
        }
    }

    private fun DialogEventConfigBinding.setupActionCard() {
        fieldActionsSelector.apply {
            setTitle(
                titleRes = R.string.menu_item_title_actions,
                emptyTitleRes = R.string.message_empty_action_list_title,
            )
            setEmptyDescription(R.string.message_empty_action_list_desc)

            setAdapter(
                EventChildrenCardsAdapter(
                    itemClickedListener = ::showActionsBriefMenu,
                ),
            )

            setOnClickListener { debounceUserInteraction { showActionsBriefMenu() } }
        }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingEvent.collect(::onEventEditingStateChanged) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.eventCanBeSaved.collect(::updateSaveButton) }
                launch { viewModel.eventName.collect(viewBinding.fieldEventName::setText) }
                launch { viewModel.eventNameError.collect(viewBinding.fieldEventName::setError) }
                launch { viewModel.conditionOperator.collect(::updateConditionOperator) }
                launch { viewModel.eventEnabledOnStart.collect(::updateEnabledOnStart) }
                launch { viewModel.shouldShowTryCard.collect(::updateTryFieldVisibility) }
                launch { viewModel.canTryEvent.collect(::updateTryFieldEnabledState) }
                launch { viewModel.actionsDescriptions.collect(viewBinding.fieldActionsSelector::setItems) }

                if (viewModel.isConfiguringScreenEvent()) {
                    launch { viewModel.imageConditions.collect(::updateImageConditionsField) }
                } else {
                    launch { viewModel.triggerConditionsDescription.collect(viewBinding.fieldTriggerConditionsSelector::setItems) }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorViews(
            conditionsField = viewBinding.fieldImageConditionsSelector.root,
            conditionOperatorAndView = viewBinding.fieldConditionsOperator.dualStateButton.buttonLeft,
            actionsField = viewBinding.fieldActionsSelector.root,
            saveButton = viewBinding.layoutTopBar.buttonSave,
        )
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    private fun onEventEditingStateChanged(isEditingScenario: Boolean) {
        if (!isEditingScenario) {
            Log.e(TAG, "Closing EventDialog because there is no event edited")
            finish()
        }
    }

    private fun updateSaveButton(enabled: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, enabled)
    }

    private fun updateImageConditionsField(conditions: List<UiImageCondition>) {
        viewBinding.fieldImageConditionsSelector.setItems(conditions)
    }

    private fun updateConditionOperator(@ConditionOperator operator: Int) {
        viewBinding.fieldConditionsOperator.apply {
            val index = if (operator == AND) 0 else 1
            setChecked(index)
            setDescription(index)
        }
    }

    private fun updateEnabledOnStart(enabledOnStart: Boolean) {
        viewBinding.fieldIsEnabled.apply {
            setChecked(enabledOnStart)
            setDescription(if (enabledOnStart) 1 else 0)
        }
    }

    private fun updateTryFieldVisibility(isEnabled: Boolean) {
        viewBinding.fieldTestEvent.root.visibility = if (isEnabled) View.VISIBLE else View.GONE
        viewBinding.dividerTrySelector.visibility = if (isEnabled) View.VISIBLE else View.GONE
    }

    private fun updateTryFieldEnabledState(isEnabled: Boolean) {
        viewBinding.fieldTestEvent.setEnabled(isEnabled)
    }

    private fun onDeleteButtonClicked() {
        if (viewModel.isEventHaveRelatedActions()) {
            showMessageDialog(R.string.dialog_overlay_title_warning, R.string.warning_dialog_message_event_delete_associated_action) {
                onDelete()
                back()
            }
        } else {
            onDelete()
            back()
        }
    }

    private fun showMessageDialog(@StringRes title: Int, @StringRes message: Int, onOkPressed: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                onOkPressed()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .showAsOverlay()
    }

    private fun showImageConditionsBriefMenu(initialFocusedIndex: Int = 0) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ImageConditionsBriefMenu(initialFocusedIndex),
            hideCurrent = true,
        )
    }

    private fun showTriggerConditionsDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = TriggerConditionListDialog()
        )
    }

    private fun showActionsBriefMenu(initialFocusedIndex: Int = 0) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = SmartActionsBriefMenu(initialFocusedIndex),
            hideCurrent = true,
        )
    }

    private fun showTryElementMenu() {
        viewModel.getTryInfo()?.let { (scenario, imageEvent) ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = TryElementOverlayMenu(scenario, imageEvent),
                hideCurrent = true,
            )
        }
    }
}

private const val TAG = "EventDialog"