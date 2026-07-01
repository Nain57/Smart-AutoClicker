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

import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.DualStateButtonTextConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.TimeUnitDropDownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setButtonConfig
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
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
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief.SmartActionsLegacyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showDeleteEventWithAssociatedActionsDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.brief.ScreenConditionsBriefMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.TriggerConditionListDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.eventtry.TryEventOverlayMenu

import com.google.android.material.bottomsheet.BottomSheetDialog

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
            back()
        }
        buttonSave.setDebouncedOnClickListener {
            onConfigComplete()
            super.back()
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

        fieldKeepDetecting.apply {
            setTitle(context.resources.getString(R.string.field_event_keep_detecting_title))
            setupDescriptions(
                listOf(
                    context.getString(R.string.field_event_keep_detecting_desc_disabled),
                    context.getString(R.string.field_event_keep_detecting_desc_enabled),
                )
            )
            setOnClickListener(viewModel::toggleKeepDetectingState)
        }

        fieldCooldownCheckbox.apply {
            setTitle(context.getString(R.string.field_event_cooldown_title))
            setDescription(context.getString(R.string.field_event_cooldown_desc))
            setOnClickListener(viewModel::toggleCooldownState)
        }

        editCooldownValue.apply {
            setLabel(R.string.field_event_cooldown_edit_label)
            setOnTextChangedListener { viewModel.setCooldownValue(it.toString().toLongOrNull()) }
        }

        dropdownCooldownTimeUnit.setItems(
            label = context.getString(R.string.dropdown_label_time_unit),
            items = listOf(
                TimeUnitDropDownItem.Milliseconds,
                TimeUnitDropDownItem.Seconds,
                TimeUnitDropDownItem.Minutes
            ),
            onItemSelected = viewModel::setCooldownTimeUnit,
        )

        fieldTestEvent.text = context.getString(
            R.string.item_title_try_element,
            context.getString(R.string.dialog_title_image_event),
        )
        buttonTestEvent.setOnClickListener { debounceUserInteraction { showTryElementMenu() } }
    }

    private fun DialogEventConfigBinding.setupConditionsCard() {
        if (viewModel.isConfiguringScreenEvent()) {
            fieldTriggerConditionsSelector.root.visibility = View.GONE
            fieldImageConditionsSelector.apply {
                root.visibility = View.VISIBLE
                setAdapter(
                    EventImageConditionsAdapter(
                        itemClickedListener = ::showImageConditionsBriefMenu,
                        bitmapProvider = viewModel::getConditionBitmap,
                    ),
                )
                setOnClickListener { debounceUserInteraction { showImageConditionsBriefMenu() } }
            }
            fieldEmptyConditionsSelector.apply {
                setTitle(context.getString(R.string.message_empty_screen_condition_list_title))
                setDescription(context.getString(R.string.message_empty_screen_condition_list_desc))
                setOnClickListener { debounceUserInteraction { showImageConditionsBriefMenu() } }
            }
            layoutConditionSelector.setOnClickListener { debounceUserInteraction { showImageConditionsBriefMenu() } }
        } else {
            fieldImageConditionsSelector.root.visibility = View.GONE
            fieldTriggerConditionsSelector.apply {
                root.visibility = View.VISIBLE
                setAdapter(EventChildrenCardsAdapter { showTriggerConditionsDialog() })
                setOnClickListener { debounceUserInteraction { showTriggerConditionsDialog() } }
            }
            fieldEmptyConditionsSelector.apply {
                setTitle(context.getString(R.string.message_empty_trigger_condition_list_title))
                setDescription(context.getString(R.string.message_empty_trigger_condition_list_desc))
                setOnClickListener { debounceUserInteraction { showTriggerConditionsDialog() } }
            }
            layoutConditionSelector.setOnClickListener { debounceUserInteraction { showTriggerConditionsDialog() } }
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
                    textLeft = context.getString(R.string.condition_operator_and),
                    textRight = context.getString(R.string.condition_operator_or),
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
            setAdapter(
                EventChildrenCardsAdapter(
                    itemClickedListener = ::showActionsOverlay,
                ),
            )

            setOnClickListener { debounceUserInteraction { showActionsOverlay() } }
        }

        fieldEmptyActionsSelector.apply {
            fieldEmptyActionsSelector.apply {
                setTitle(context.getString(R.string.message_empty_action_list_title))
                setDescription(context.getString(R.string.message_empty_action_list_desc))
            }

            setOnClickListener { debounceUserInteraction { showActionsOverlay() } }
        }

        layoutActionsSelector.setOnClickListener { debounceUserInteraction { showActionsOverlay() } }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingEvent.collect(::onEventEditingStateChanged) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorViews(
            conditionsField = viewBinding.layoutConditionSelector,
            conditionOperatorAndView = viewBinding.fieldConditionsOperator.dualStateButton.buttonLeft,
            actionsField = viewBinding.layoutActionsSelector,
            saveButton = viewBinding.layoutTopBar.buttonSave,
        )
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog {
                onDismiss()
                super.back()
            }
            return
        }

        onDismiss()
        super.back()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
    }

    private fun updateUiState(state: EventDialogUiState?) {
        state ?: return

        // Generic views
        viewBinding.apply {
            layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, state.canBeSaved)

            fieldEventName.setText(state.name)
            fieldEventName.setError(state.nameError)

            fieldConditionsOperator.apply {
                val index = if (state.conditionOperator == AND) 0 else 1
                setChecked(index)
                setDescription(index)
            }

            fieldIsEnabled.apply {
                setChecked(state.enabledOnStart)
                setDescription(if (state.enabledOnStart) 1 else 0)
            }

            viewBinding.fieldActionsSelector.setItems(state.actionsItems)
            if (state.actionsItems.isEmpty()) {
                fieldActionsSelector.root.visibility = View.GONE
                fieldEmptyActionsSelector.root.visibility = View.VISIBLE
                fieldEmptyActionsSelector.setError(true)

            } else {
                fieldActionsSelector.root.visibility = View.VISIBLE
                fieldEmptyActionsSelector.root.visibility = View.GONE
                fieldEmptyActionsSelector.setError(false)
            }
        }

        // Specific views
        when (state) {
            is EventDialogUiState.ScreenEvent -> updateScreenEventUiState(state)
            is EventDialogUiState.TriggerEvent -> updateTriggerEventUiState(state)
        }
    }

    private fun updateScreenEventUiState(uiState: EventDialogUiState.ScreenEvent) {
        viewBinding.apply {
            fieldKeepDetecting.root.visibility = View.VISIBLE
            cooldownLayout.visibility = View.VISIBLE
            dividerCooldown.visibility = View.VISIBLE
            dividerKeepDetecting.visibility = View.VISIBLE
            cardEventTest.visibility = View.VISIBLE

            fieldKeepDetecting.apply {
                setChecked(uiState.keepDetecting)
                setDescription(if (uiState.keepDetecting) 1 else 0)
            }

            fieldCooldownCheckbox.setChecked(uiState.cooldownEnabled)
            editCooldownValue.textLayout.isEnabled = uiState.cooldownEnabled
            dropdownCooldownTimeUnit.textLayout.isEnabled = uiState.cooldownEnabled

            editCooldownValue.setText(uiState.cooldownValue, InputType.TYPE_CLASS_NUMBER)
            dropdownCooldownTimeUnit.setSelectedItem(uiState.cooldownUnit)

            buttonTestEvent.setEnabled(uiState.canTryEvent)

            fieldImageConditionsSelector.setItems(uiState.imageConditionsItems)
            if (uiState.imageConditionsItems.isEmpty()) {
                fieldImageConditionsSelector.root.visibility = View.GONE
                fieldEmptyConditionsSelector.root.visibility = View.VISIBLE
                fieldEmptyConditionsSelector.setError(true)

            } else {
                fieldImageConditionsSelector.root.visibility = View.VISIBLE
                fieldEmptyConditionsSelector.root.visibility = View.GONE
                fieldEmptyConditionsSelector.setError(false)
            }
        }
    }

    private fun updateTriggerEventUiState(uiState: EventDialogUiState.TriggerEvent) {
        viewBinding.apply {
            fieldKeepDetecting.root.visibility = View.GONE
            cooldownLayout.visibility = View.GONE
            dividerCooldown.visibility = View.GONE
            dividerKeepDetecting.visibility = View.GONE
            cardEventTest.visibility = View.GONE

            fieldTriggerConditionsSelector.setItems(uiState.triggerConditionsItems)
            if (uiState.triggerConditionsItems.isEmpty()) {
                fieldTriggerConditionsSelector.root.visibility = View.GONE
                fieldEmptyConditionsSelector.root.visibility = View.VISIBLE
                fieldEmptyConditionsSelector.setError(true)

            } else {
                fieldTriggerConditionsSelector.root.visibility = View.VISIBLE
                fieldEmptyConditionsSelector.root.visibility = View.GONE
                fieldEmptyConditionsSelector.setError(false)
            }
        }
    }

    private fun onEventEditingStateChanged(isEditingScenario: Boolean) {
        if (!isEditingScenario) {
            Log.e(TAG, "Closing EventDialog because there is no event edited")
            finish()
        }
    }

    private fun onDeleteButtonClicked() {
        if (viewModel.isEventHaveRelatedActions()) {
            context.showDeleteEventWithAssociatedActionsDialog {
                onDelete()
                super.back()
            }
            return
        }

        onDelete()
        super.back()
    }

    private fun showImageConditionsBriefMenu(initialFocusedIndex: Int = 0) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ScreenConditionsBriefMenu(initialFocusedIndex),
            hideCurrent = true,
        )
    }

    private fun showTriggerConditionsDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = TriggerConditionListDialog()
        )
    }

    private fun showActionsOverlay(initialFocusedIndex: Int = 0) {
        if (viewModel.isLegacyActionUiEnabled()) {
            overlayManager.navigateTo(
                context = context,
                newOverlay = SmartActionsLegacyDialog(),
                hideCurrent = true,
            )
        } else {
            overlayManager.navigateTo(
                context = context,
                newOverlay = SmartActionsBriefMenu(initialFocusedIndex),
                hideCurrent = true,
            )
        }
    }

    private fun showTryElementMenu() {
        viewModel.getTryInfo()?.let { (scenario, imageEvent) ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = TryEventOverlayMenu(scenario, imageEvent),
                hideCurrent = true,
            )
        }
    }
}

private const val TAG = "EventDialog"