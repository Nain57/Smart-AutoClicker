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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.brief

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.extensions.showAsOverlay
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBriefMenu
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayImageConditionsBriefMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy.ConditionCopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.CaptureMenu
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.ImageConditionDialog
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.overlay.TryElementOverlayMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import kotlinx.coroutines.launch


class ImageConditionsBriefMenu(private val onConfigComplete: () -> Unit) : ItemBriefMenu(
    theme = R.style.ScenarioConfigTheme,
    noItemText = R.string.brief_empty_image_conditions,
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
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewBinding = OverlayImageConditionsBriefMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onMenuItemClicked(viewId: Int) {
        debounceUserInteraction {
            when (viewId) {
                R.id.btn_save -> {
                    onConfigComplete()
                    back()
                }
                R.id.btn_add -> showNewCaptureOverlay()
                R.id.btn_copy -> showImageConditionCopyDialog()
            }
        }
    }

    override fun onItemBriefClicked(index: Int, item: ItemBrief) {
        showImageConditionConfigDialog(item.data as ImageCondition, isNewCondition = false)
    }

    override fun onDeleteItem(index: Int) {
        if (!viewModel.deleteImageCondition(index)) {
            showAssociatedActionWarning(index)
        }
    }

    override fun onPlayItem(index: Int) {
        showTryConditionOverlay()
    }

    override fun onFocusedItemChanged(index: Int) {
        super.onFocusedItemChanged(index)
        viewModel.setFocusedItemIndex(index)
    }

    private fun updateActionVisualisation(visualization: ItemBriefDescription?) {
        briefViewBinding.viewBrief.setDescription(visualization, true)
    }

    private fun showTryConditionOverlay() {
        viewModel.getEditedScenario()?.let { scenario ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = TryElementOverlayMenu(scenario, getFocusedItemBrief().data as ImageCondition),
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
                        isNewCondition = true,
                    )
                },
            ),
        )
    }

    private fun showNewCaptureOverlay() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CaptureMenu { capturedCondition ->
                showImageConditionConfigDialog(capturedCondition, isNewCondition = true)
            },
            hideCurrent = true,
        )
    }

    private fun showImageConditionConfigDialog(condition: ImageCondition, isNewCondition: Boolean) {
        viewModel.startConditionEdition(condition)

        val conditionConfigDialogListener: OnConditionConfigCompleteListener by lazy {
            object : OnConditionConfigCompleteListener {
                override fun onConfirmClicked() {
                    if (isNewCondition) prepareItemInsertion()
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

    private fun showAssociatedActionWarning(index: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.warning_dialog_message_condition_delete_associated_action)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                viewModel.deleteImageCondition(index, force = true)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .showAsOverlay()
    }
}
