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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.conditions

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager

import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.ConditionDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.ConditionSelectorMenu
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.copy.ConditionCopyDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_DISABLED_ITEM
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_ENABLED_ITEM
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.viewModels

import kotlinx.coroutines.launch

class ConditionsContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: ConditionsViewModel by viewModels()

    /** View binding for all views in this content. */
    private lateinit var viewBinding: IncludeLoadableListBinding
    /** Adapter for the list of conditions. */
    private lateinit var conditionsAdapter: ConditionAdapter

    /** Tells if the billing flow has been triggered by the condition count limit. */
    private var conditionLimitReachedClick: Boolean = false

    override fun createCopyButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        conditionsAdapter = ConditionAdapter(
            conditionClickedListener = ::onConditionClicked,
            bitmapProvider = viewModel::getConditionBitmap,
            itemViewBound = ::onConditionItemBound,
        )

        viewBinding = IncludeLoadableListBinding.inflate(LayoutInflater.from(context), container, false).apply {
            setEmptyText(
                id = R.string.message_empty_conditions,
                secondaryId = R.string.message_empty_secondary_condition_list,
            )
            list.apply {
                adapter = conditionsAdapter
                layoutManager = GridLayoutManager(
                    context,
                    2,
                )
            }
        }

        return viewBinding.root
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
            OverlayManager.getInstance(context).navigateTo(
                context = context,
                newOverlay = ConditionSelectorMenu(
                    onConditionSelected = { area, bitmap ->
                        showConditionConfigDialog(viewModel.createCondition(context, area, bitmap))
                    }
                ),
                hideCurrent = true,
            )
        }
    }

    override fun onCopyButtonClicked() {
        debounceUserInteraction {
            OverlayManager.getInstance(context).navigateTo(
                context = context,
                newOverlay = ConditionCopyDialog(
                    onConditionSelected = { conditionSelected ->
                        showConditionConfigDialog(viewModel.createNewConditionFromCopy(conditionSelected))
                    },
                ),
            )
        }
    }

    private fun onCreateCopyClickedWhileLimited() {
        debounceUserInteraction {
            conditionLimitReachedClick = true

            dialogController.hide()
            viewModel.onConditionCountReachedAddCopyClicked(context)
        }
    }

    private fun onConditionClicked(condition: Condition) {
        debounceUserInteraction {
            showConditionConfigDialog(condition)
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

    private fun updateConditionList(newItems: List<Condition>?) {
        viewBinding.updateState(newItems)
        conditionsAdapter.submitList(newItems)
    }

    private fun showConditionConfigDialog(condition: Condition) {
        viewModel.startConditionEdition(condition)

        OverlayManager.getInstance(context).navigateTo(
            context = context,
            newOverlay = ConditionDialog(
                onConfirmClickedListener = viewModel::upsertEditedCondition,
                onDeleteClickedListener = viewModel::removeEditedCondition,
                onDismissClickedListener = viewModel::dismissEditedCondition
            ),
            hideCurrent = true,
        )
    }
}