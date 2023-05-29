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
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager

import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.NavigationRequest
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.ConditionDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.ConditionSelectorMenu
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition.copy.ConditionCopyDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.EventDialogViewModel
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_DISABLED_ITEM
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_ENABLED_ITEM
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding

import kotlinx.coroutines.launch

class ConditionsContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for the container dialog. */
    private val dialogViewModel: EventDialogViewModel by lazy {
        ViewModelProvider(dialogController).get(EventDialogViewModel::class.java)
    }
    /** View model for this content. */
    private val viewModel: ConditionsViewModel by lazy {
        ViewModelProvider(this).get(ConditionsViewModel::class.java)
    }

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

    override fun onCreateButtonClicked() {
        dialogViewModel.requestSubOverlay(newConditionSelectorNavigationRequest())
    }

    override fun onCopyButtonClicked() {
        dialogViewModel.requestSubOverlay(newConditionCopyNavigationRequest())
    }

    private fun onCreateCopyClickedWhileLimited() {
        conditionLimitReachedClick = true

        dialogController.hide()
        viewModel.onConditionCountReachedAddCopyClicked(context)
    }

    private fun onConditionClicked(condition: Condition) {
        showConditionConfigDialog(condition)
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

    private fun newConditionSelectorNavigationRequest() = NavigationRequest(
        overlay = ConditionSelectorMenu(
            context = context,
            onConditionSelected = { area, bitmap ->
                showConditionConfigDialog(viewModel.createCondition(context, area, bitmap))
            }
        ),
        hideCurrent = true,
    )

    private fun newConditionCopyNavigationRequest() = NavigationRequest(
        ConditionCopyDialog(
            context = context,
            onConditionSelected = { conditionSelected ->
                showConditionConfigDialog(viewModel.createNewConditionFromCopy(conditionSelected))
            }
        )
    )

    private fun showConditionConfigDialog(condition: Condition) {
        viewModel.startConditionEdition(condition)
        dialogViewModel.requestSubOverlay(
            NavigationRequest(
                ConditionDialog(
                    context = context,
                    onConfirmClicked = viewModel::upsertEditedCondition,
                    onDeleteClicked = viewModel::removeEditedCondition,
                    onDismissClicked = viewModel::dismissEditedCondition
                )
            )
        )
    }
}