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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event.conditions

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.MultiChoiceDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.viewModels
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.ImageConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.CaptureMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy.ConditionCopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.allTriggerConditionChoices
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.broadcast.BroadcastReceivedConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.counter.CounterReachedConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.timer.TimerReachedConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.utils.ALPHA_DISABLED_ITEM
import com.buzbuz.smartautoclicker.feature.smart.config.utils.ALPHA_ENABLED_ITEM

import kotlinx.coroutines.launch

class ConditionsContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: ConditionsViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { conditionsViewModel() },
    )
    /** View binding for all views in this content. */
    private lateinit var viewBinding: IncludeLoadableListBinding

    private val conditionConfigDialogListener: OnConditionConfigCompleteListener by lazy {
        object : OnConditionConfigCompleteListener {
            override fun onConfirmClicked() { viewModel.upsertEditedCondition() }
            override fun onDeleteClicked() { viewModel.removeEditedCondition() }
            override fun onDismissClicked() { viewModel.dismissEditedCondition() }
        }
    }

    /** Tells if the billing flow has been triggered by the condition count limit. */
    private var conditionLimitReachedClick: Boolean = false

    override fun createCopyButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = IncludeLoadableListBinding.inflate(LayoutInflater.from(context), container, false)

        when (viewModel.getEditedEvent()) {
            is ImageEvent -> setupImageEventView()
            is TriggerEvent -> setupTriggerEventView()
            null -> dialogController.back()
        }

        return viewBinding.root
    }

    private fun setupImageEventView() {
        viewBinding.apply {
            setEmptyText(
                id = R.string.message_empty_image_conditions,
                secondaryId = R.string.message_empty_secondary_image_condition_list,
            )
            list.apply {
                adapter = ImageConditionAdapter(
                    conditionClickedListener = ::onImageConditionClicked,
                    bitmapProvider = viewModel::getConditionBitmap,
                    itemViewBound = ::onConditionItemBound,
                )
                layoutManager = GridLayoutManager(context, 2)
            }
        }
    }

    private fun setupTriggerEventView() {
        viewBinding.apply {
            setEmptyText(
                id = R.string.message_empty_trigger_conditions,
                secondaryId = R.string.message_empty_secondary_trigger_condition_list,
            )
            list.apply {
                adapter = TriggerConditionAdapter(
                    conditionClickedListener = ::onTriggerConditionClicked,
                )
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            }
        }
    }

    override fun onViewCreated() {
        // When the billing flow is not longer displayed, restore the dialogs states
        lifecycleScope.launch {
            repeatOnLifecycle((Lifecycle.State.CREATED)) {
                viewModel.isBillingFlowDisplayed.collect { isDisplayed ->
                    if (!isDisplayed) {
                        if (conditionLimitReachedClick) {
                            dialogController.show()
                            conditionLimitReachedClick = false
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isConditionLimitReached.collect(::updateConditionLimitationVisibility) }
                launch { viewModel.canCopyCondition.collect(::updateCopyButtonVisibility) }
                launch { viewModel.configuredEventConditions.collect(::updateConditionList) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorCreateConditionView(dialogController.createCopyButtons.buttonNew)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopAllViewMonitoring()
    }

    override fun onCreateButtonClicked() {
        debounceUserInteraction {
            when (viewModel.getEditedEvent()) {
                is ImageEvent -> showImageConditionCaptureOverlay()
                is TriggerEvent -> showTriggerConditionTypeSelectionDialog()
                null -> return@debounceUserInteraction
            }
        }
    }

    override fun onCopyButtonClicked() {
        debounceUserInteraction {
            showCopyDialog()
        }
    }

    private fun onCreateCopyClickedWhileLimited() {
        debounceUserInteraction {
            conditionLimitReachedClick = true

            dialogController.hide()
            viewModel.onConditionCountReachedAddCopyClicked(context)
        }
    }

    private fun onImageConditionClicked(condition: ImageCondition) {
        debounceUserInteraction {
            showImageConditionConfigDialog(condition)
        }
    }

    private fun onTriggerConditionClicked(condition: TriggerCondition) {
        debounceUserInteraction {
            showTriggerConditionDialog(condition)
        }
    }

    private fun onConditionItemBound(index: Int, itemView: View?) {
        if (index != 0) return

        if (itemView != null) viewModel.monitorFirstConditionView(itemView)
        else viewModel.stopFirstConditionViewMonitoring()
    }

    private fun updateConditionLimitationVisibility(isVisible: Boolean) {
        dialogController.createCopyButtons.apply {
            if (isVisible) {
                root.alpha = ALPHA_DISABLED_ITEM
                buttonNew.setOnClickListener { onCreateCopyClickedWhileLimited() }
                buttonCopy.setOnClickListener { onCreateCopyClickedWhileLimited() }
            } else {
                root.alpha = ALPHA_ENABLED_ITEM
                buttonNew.setOnClickListener { onCreateButtonClicked() }
                buttonCopy.setOnClickListener { onCopyButtonClicked() }
            }
        }
    }

    private fun updateCopyButtonVisibility(isVisible: Boolean) {
        dialogController.createCopyButtons.buttonCopy.apply {
            if (isVisible) show() else hide()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateConditionList(newItems: List<Condition>?) {
        viewBinding.apply {
            updateState(newItems)
            (list.adapter as ListAdapter<Condition, RecyclerView.ViewHolder>).submitList(newItems)
        }
    }

    private fun showImageConditionCaptureOverlay() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = CaptureMenu(onConditionSelected = ::showImageConditionConfigDialog),
            hideCurrent = true,
        )
    }

    private fun showImageConditionConfigDialog(condition: ImageCondition) {
        viewModel.startConditionEdition(condition)

        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = ImageConditionDialog(conditionConfigDialogListener),
            hideCurrent = true,
        )
    }

    private fun showTriggerConditionTypeSelectionDialog() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = MultiChoiceDialog(
                theme = R.style.AppTheme,
                dialogTitleText = R.string.dialog_overlay_title_trigger_condition_type,
                choices = allTriggerConditionChoices(),
                onChoiceSelected = { choice ->
                    showTriggerConditionDialog(viewModel.createNewTriggerCondition(context, choice))
                },
            ),
            hideCurrent = false,
        )
    }

    private fun showTriggerConditionDialog(condition: TriggerCondition) {
        viewModel.startConditionEdition(condition)

        val configOverlay = when (condition) {
            is TriggerCondition.OnBroadcastReceived ->
                BroadcastReceivedConditionDialog(conditionConfigDialogListener)
            is TriggerCondition.OnCounterCountReached ->
                CounterReachedConditionDialog(conditionConfigDialogListener)
            is TriggerCondition.OnTimerReached ->
                TimerReachedConditionDialog(conditionConfigDialogListener)
        }

        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = configOverlay,
            hideCurrent = true,
        )
    }

    private fun showCopyDialog() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = ConditionCopyDialog(
                onConditionSelected = { conditionSelected ->
                    when (conditionSelected) {
                        is ImageCondition -> showImageConditionConfigDialog(
                            viewModel.createNewImageConditionFromCopy(conditionSelected)
                        )
                        is TriggerCondition -> showTriggerConditionDialog(
                            viewModel.createNewTriggerConditionFromCopy(conditionSelected)
                        )
                    }
                },
            ),
        )
    }
}