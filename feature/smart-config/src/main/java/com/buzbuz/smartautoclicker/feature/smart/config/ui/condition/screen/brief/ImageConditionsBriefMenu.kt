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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.brief

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.MoveToDialog
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBriefMenu
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayImageConditionsBriefMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showDeleteConditionsWithAssociatedActionsDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy.ConditionCopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.image.ImageConditionCaptureMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.image.ImageConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay.TryImageConditionOverlayMenu

import kotlinx.coroutines.launch


class ImageConditionsBriefMenu(
    initialFocusedIndex: Int,
) : ItemBriefMenu(
    theme = R.style.AppTheme,
    noItemText = R.string.brief_empty_image_conditions,
    initialItemIndex = initialFocusedIndex,
) {

    /** The view model for this dialog. */
    private val viewModel: ImageConditionsBriefViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { imageConditionsBriefViewModel() }
    )

    private lateinit var viewBinding: OverlayImageConditionsBriefMenuBinding

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.conditionBriefList.collect(::updateItemList) }
                launch { viewModel.conditionVisualization.collect(::updateActionVisualisation) }
                launch { viewModel.isTutorialModeEnabled.collect(::updateTutorialModeState) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewBinding = OverlayImageConditionsBriefMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateBriefItemViewHolder(parent: ViewGroup, orientation: Int): ImageConditionBriefViewHolder =
        ImageConditionBriefViewHolder(LayoutInflater.from(parent.context), orientation, parent)

    override fun onBriefItemViewBound(index: Int, itemView: View?) {
        if (index != 0) return

        if (itemView != null) viewModel.monitorBriefFirstItemView(itemView)
        else viewModel.stopBriefFirstItemMonitoring()
    }

    override fun onStart() {
        super.onStart()
        viewModel.monitorViews(
            createMenuButton = viewBinding.btnAdd,
            saveMenuButton = viewBinding.btnSave,
        )
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopAllViewMonitoring()
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_save -> back()
            R.id.btn_add -> showNewCaptureOverlay()
            R.id.btn_copy -> showImageConditionCopyDialog()
        }
    }

    override fun onMoveItemClicked(from: Int, to: Int) {
        viewModel.swapConditions(from, to)
    }

    override fun onItemPositionCardClicked(index: Int, itemCount: Int) {
        if (itemCount < 2) return
        showMoveToDialog(index, itemCount)
    }

    override fun onItemBriefClicked(index: Int, item: ItemBrief) {
        showImageConditionConfigDialog((item.data as UiImageCondition).condition)
    }

    override fun onDeleteItemClicked(index: Int) {
        if (!viewModel.deleteImageCondition(index)) {
            context.showDeleteConditionsWithAssociatedActionsDialog {
                viewModel.deleteImageCondition(index, force = true)
            }
        }
    }

    override fun onPlayItemClicked(index: Int) {
        showTryConditionOverlay()
    }

    override fun onFocusedItemChanged(index: Int) {
        super.onFocusedItemChanged(index)
        viewModel.setFocusedItemIndex(index)
    }

    private fun updateActionVisualisation(visualization: ItemBriefDescription?) {
        briefViewBinding.viewBrief.setDescription(visualization, true)
    }

    private fun updateTutorialModeState(isTutorialEnabled: Boolean) {
        setBriefPanelAutoHide(!isTutorialEnabled)
    }

    private fun showTryConditionOverlay() {
        viewModel.getEditedScenario()?.let { scenario ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = TryImageConditionOverlayMenu(
                    scenario = scenario,
                    imageCondition = (getFocusedItemBrief().data as UiImageCondition).condition,
                    onNewThresholdSelected = { threshold ->
                        viewModel.updateConditionThreshold(threshold)
                    }
                ),
                hideCurrent = true,
            )
        }
    }

    private fun showImageConditionCopyDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ConditionCopyDialog(
                onConditionSelected = { conditionSelected ->
                    if (conditionSelected !is ImageCondition) return@ConditionCopyDialog
                    showImageConditionConfigDialog(
                        viewModel.createNewImageConditionFromCopy(conditionSelected),
                    )
                },
            ),
        )
    }

    private fun showNewCaptureOverlay() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ImageConditionCaptureMenu { capturedCondition ->
                showImageConditionConfigDialog(capturedCondition)
            },
            hideCurrent = true,
        )
    }

    private fun showImageConditionConfigDialog(condition: ImageCondition) {
        viewModel.startConditionEdition(condition)

        val conditionConfigDialogListener: OnConditionConfigCompleteListener by lazy {
            object : OnConditionConfigCompleteListener {
                override fun onConfirmClicked() {
                    viewModel.upsertEditedCondition()
                }
                override fun onDeleteClicked() { viewModel.removeEditedCondition() }
                override fun onDismissClicked() { viewModel.dismissEditedCondition() }
            }
        }

        overlayManager.navigateTo(
            context = context,
            newOverlay = ImageConditionDialog(conditionConfigDialogListener),
            hideCurrent = true,
        )
    }

    private fun showMoveToDialog(index: Int, itemCount: Int) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = MoveToDialog(
                theme = R.style.ScenarioConfigTheme,
                defaultValue = index + 1,
                itemCount = itemCount,
                onValueSelected = { value ->
                    if (value - 1 == index) return@MoveToDialog
                    viewModel.moveConditions(index, value - 1)
                }
            ),
        )
    }
}
