/*
 * Copyright (C) 2025 Kevin Buzeau
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
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief.SmartActionsLegacyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.adapters.ScreenConditionsAdapter
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showDeleteEventWithAssociatedActionsDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.brief.ScreenConditionsBriefMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.TriggerConditionListDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay.TryEventOverlayMenu

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
            fieldScreenConditionsSelector.apply {
                root.visibility = View.VISIBLE
                setTitle(
                    titleRes = R.string.menu_item_title_conditions,
                    emptyTitleRes = R.string.message_empty_screen_condition_list_title,
                )
                setEmptyDescription(R.string.message_empty_screen_condition_list_desc)

                setAdapter(
                    ScreenConditionsAdapter(
                        itemClickedListener = { _, idx -> showImageConditionsBriefMenu(idx) },
                        bitmapProvider = viewModel::getConditionBitmap,
                    ),
                )

                setOnClickListener { debounceUserInteraction { showImageConditionsBriefMenu() } }
            }
        } else {
            fieldScreenConditionsSelector.root.visibility = View.GONE
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
                    itemClickedListener = ::showActionsOverlay,
                ),
            )

            setOnClickListener { debounceUserInteraction { showActionsOverlay() } }
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
                launch { viewModel.keepDetecting.collect(::updateKeepDetecting) }
                launch { viewModel.isScreenEvent.collect(::updateImageEventSpecificViewsVisibility) }
                launch { viewModel.canTryEvent.collect(::updateTryFieldEnabledState) }
                launch { viewModel.actionsDescriptions.collect(viewBinding.fieldActionsSelector::setItems) }

                if (viewModel.isConfiguringScreenEvent()) {
                    launch { viewModel.screenConditions.collect(::updateScreenConditionsField) }
                } else {
                    launch { viewModel.triggerConditionsDescription.collect(viewBinding.fieldTriggerConditionsSelector::setItems) }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorViews(
            conditionsField = viewBinding.fieldScreenConditionsSelector.root,
            conditionOperatorAndView = viewBinding.fieldConditionsOperator.dualStateButton.buttonLeft,
            actionsField = viewBinding.fieldActionsSelector.root,
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

    private fun onEventEditingStateChanged(isEditingScenario: Boolean) {
        if (!isEditingScenario) {
            Log.e(TAG, "Closing EventDialog because there is no event edited")
            finish()
        }
    }

    private fun updateSaveButton(enabled: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, enabled)
    }

    private fun updateScreenConditionsField(conditions: List<UiScreenCondition>) {
        viewBinding.fieldScreenConditionsSelector.setItems(conditions)
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

    private fun updateKeepDetecting(keepDetecting: Boolean) {
        viewBinding.fieldKeepDetecting.apply {
            setChecked(keepDetecting)
            setDescription(if (keepDetecting) 1 else 0)
        }
    }

    private fun updateImageEventSpecificViewsVisibility(isEnabled: Boolean) {
        viewBinding.apply {
            fieldKeepDetecting.root.visibility =  if (isEnabled) View.VISIBLE else View.GONE
            dividerKeepDetecting.visibility =  if (isEnabled) View.VISIBLE else View.GONE
            fieldTestEvent.root.visibility = if (isEnabled) View.VISIBLE else View.GONE
            dividerTrySelector.visibility = if (isEnabled) View.VISIBLE else View.GONE
        }
    }

    private fun updateTryFieldEnabledState(isEnabled: Boolean) {
        viewBinding.fieldTestEvent.setEnabled(isEnabled)
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