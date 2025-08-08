
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.MultiChoiceDialog
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogTriggerConditionsBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTriggerCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy.ConditionCopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.broadcast.BroadcastReceivedConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.counter.CounterReachedConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger.timer.TimerReachedConditionDialog

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch


class TriggerConditionListDialog() : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: TriggerConditionListViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { triggerConditionsViewModel() },
    )

    private lateinit var viewBinding: DialogTriggerConditionsBinding

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogTriggerConditionsBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                dialogTitle.setText(R.string.dialog_title_trigger_event)

                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            buttonNew.setDebouncedOnClickListener { showTriggerConditionTypeSelectionDialog() }
            buttonCopy.setDebouncedOnClickListener { showCopyDialog() }

            layoutLoadableList.apply {
                setEmptyText(
                    id = R.string.message_empty_trigger_condition_list_title,
                    secondaryId = R.string.message_empty_trigger_condition_list_desc,
                )

                list.apply {
                    adapter = TriggerConditionAdapter(::showTriggerConditionDialog)
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.canCopyCondition.collect(::updateCopyButton) }
                launch { viewModel.configuredTriggerConditions.collect(::updateConditionList) }
            }
        }
    }

    private fun updateCopyButton(visible: Boolean) {
        viewBinding.buttonCopy.visibility = if (visible) View.VISIBLE else View.GONE
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateConditionList(newItems: List<UiTriggerCondition>?) {
        viewBinding.layoutLoadableList.apply {
            updateState(newItems)
            (list.adapter as ListAdapter<UiTriggerCondition, RecyclerView.ViewHolder>).submitList(newItems)
        }
    }

    private fun showTriggerConditionTypeSelectionDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = MultiChoiceDialog(
                theme = R.style.AppTheme,
                dialogTitleText = R.string.dialog_title_trigger_condition_type,
                choices = allTriggerConditionChoices(),
                onChoiceSelected = { choice ->
                    showTriggerConditionDialog(viewModel.createNewTriggerCondition(context, choice))
                },
            ),
            hideCurrent = false,
        )
    }

    private fun showCopyDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ConditionCopyDialog(
                onConditionSelected = { conditionSelected ->
                    (conditionSelected as? TriggerCondition)?.let {
                        showTriggerConditionDialog(viewModel.createNewTriggerConditionFromCopy(it))
                    }
                },
            ),
        )
    }

    private fun showTriggerConditionDialog(condition: TriggerCondition) {
        viewModel.startConditionEdition(condition)

        val conditionConfigDialogListener: OnConditionConfigCompleteListener by lazy {
            object : OnConditionConfigCompleteListener {
                override fun onConfirmClicked() { viewModel.upsertEditedCondition() }
                override fun onDeleteClicked() { viewModel.removeEditedCondition() }
                override fun onDismissClicked() { viewModel.dismissEditedCondition() }
            }
        }

        val configOverlay = when (condition) {
            is TriggerCondition.OnBroadcastReceived ->
                BroadcastReceivedConditionDialog(conditionConfigDialogListener)
            is TriggerCondition.OnCounterCountReached ->
                CounterReachedConditionDialog(conditionConfigDialogListener)
            is TriggerCondition.OnTimerReached ->
                TimerReachedConditionDialog(conditionConfigDialogListener)
        }

        overlayManager.navigateTo(
            context = context,
            newOverlay = configOverlay,
            hideCurrent = true,
        )
    }
}