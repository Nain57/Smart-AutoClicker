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
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayScreenConditionsBriefMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.showDeleteConditionsWithAssociatedActionsDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.condition.ConditionCopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.selection.ScreenConditionTypeChoice
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.selection.allScreenConditionChoices
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.ColorConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.capture.ColorCaptureMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.image.CaptureMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.image.ImageConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.number.NumberConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.selection.ScreenConditionTypeSelectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.TextConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.conditiontry.TryImageConditionOverlayMenu

import kotlinx.coroutines.launch


class ScreenConditionsBriefMenu(
    initialFocusedIndex: Int,
) : ItemBriefMenu(
    theme = R.style.AppTheme,
    noItemText = R.string.brief_empty_image_conditions,
    initialItemIndex = initialFocusedIndex,
) {

    /** The view model for this dialog. */
    private val viewModel: ScreenConditionsBriefViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { screenConditionsBriefViewModel() }
    )

    private lateinit var viewBinding: OverlayScreenConditionsBriefMenuBinding

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
        viewBinding = OverlayScreenConditionsBriefMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateBriefItemViewHolder(parent: ViewGroup, orientation: Int): ScreenConditionBriefViewHolder =
        ScreenConditionBriefViewHolder(LayoutInflater.from(parent.context), orientation, parent)

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
            R.id.btn_add -> showScreenConditionTypeSelectionDialog()
            R.id.btn_copy -> showScreenConditionCopyDialog()
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
        showScreenConditionConfigDialog((item.data as UiScreenCondition).condition)
    }

    override fun onDeleteItemClicked(index: Int) {
        if (!viewModel.deleteScreenCondition(index)) {
            context.showDeleteConditionsWithAssociatedActionsDialog {
                viewModel.deleteScreenCondition(index, force = true)
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
        val focusedItem = getFocusedItemBrief() ?: return
        val condition = (focusedItem.data as? UiScreenCondition)?.condition ?: return

        viewModel.getEditedScenario()?.let { scenario ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = TryImageConditionOverlayMenu(
                    scenario = scenario,
                    imageCondition = condition,
                    onNewThresholdSelected = { threshold ->
                        viewModel.updateConditionThreshold(threshold)
                    }
                ),
                hideCurrent = true,
            )
        }
    }

    private fun showScreenConditionCopyDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ConditionCopyDialog(
                onConditionsCopied = { conditionsSelected ->
                    if (conditionsSelected.size != 1) return@ConditionCopyDialog
                    (conditionsSelected[0] as? ScreenCondition)?.let { condition ->
                        showScreenConditionConfigDialog(condition)
                    }
                },
                requestTriggerConditions = false,
            ),
        )
    }

    private fun showScreenConditionTypeSelectionDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ScreenConditionTypeSelectionDialog(
                choices = allScreenConditionChoices(),
                onChoiceSelectedListener = { choice ->
                    when (choice) {
                        ScreenConditionTypeChoice.OnColorDetected -> showNewColorCaptureOverlay()
                        ScreenConditionTypeChoice.OnImageDetected -> showNewImageCaptureOverlay()
                        ScreenConditionTypeChoice.OnNumberDetected -> viewModel.createNumberCondition(context) { condition ->
                            showScreenConditionConfigDialog(condition)
                        }

                        ScreenConditionTypeChoice.OnTextDetected -> viewModel.createTextCondition(context) { condition ->
                            showScreenConditionConfigDialog(condition)
                        }
                    }
                },
                onCancelledListener = {},
            ),
            hideCurrent = false,
        )
    }

    private fun showNewColorCaptureOverlay() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ColorCaptureMenu { position, color ->
                viewModel.createColorCondition(context, position, color) { condition ->
                    showScreenConditionConfigDialog(condition)
                }
            },
            hideCurrent = true,
        )
    }

    private fun showNewImageCaptureOverlay() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CaptureMenu { capturedCondition ->
                showScreenConditionConfigDialog(capturedCondition)
            },
            hideCurrent = true,
        )
    }

    private fun showScreenConditionConfigDialog(condition: ScreenCondition) {
        viewModel.startConditionEdition(condition)

        val conditionConfigDialogListener: OnConditionConfigCompleteListener by lazy {
            object : OnConditionConfigCompleteListener {
                override fun onConfirmClicked() { viewModel.upsertEditedCondition() }
                override fun onDeleteClicked() { viewModel.removeEditedCondition() }
                override fun onDismissClicked() { viewModel.dismissEditedCondition() }
            }
        }

        overlayManager.navigateTo(
            context = context,
            newOverlay = when (condition) {
                is ScreenCondition.Color -> ColorConditionDialog(conditionConfigDialogListener)
                is ScreenCondition.Image -> ImageConditionDialog(conditionConfigDialogListener)
                is ScreenCondition.Number -> NumberConditionDialog(conditionConfigDialogListener)
                is ScreenCondition.Text -> TextConditionDialog(conditionConfigDialogListener)
            },
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
