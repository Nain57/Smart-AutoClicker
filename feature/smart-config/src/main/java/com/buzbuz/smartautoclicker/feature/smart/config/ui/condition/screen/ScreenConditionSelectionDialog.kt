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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.GridLayoutManager

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.ui.di.UiEntryPoint
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseSelectionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.adapters.ScreenConditionsAdapter
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition

import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.EntryPoints

import kotlinx.coroutines.Job

class ScreenConditionSelectionDialog(
    private val conditionList: List<UiScreenCondition>,
    bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val onConditionSelected: (ScreenCondition) -> Unit,
): OverlayDialog(R.style.ScenarioConfigTheme) {

    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager by lazy {
        EntryPoints.get(context.applicationContext, UiEntryPoint::class.java).monitoredViewManager()
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseSelectionBinding

    /** Adapter for the list of condition. */
    private val conditionsAdapter = ScreenConditionsAdapter(
        itemClickedListener = { condition, _ -> onConditionClicked(condition) },
        bitmapProvider = bitmapProvider,
        itemViewBound = ::onConditionItemBound,
    )

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseSelectionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_condition_selection)
                buttonSave.visibility = View.GONE
                buttonDismiss.setDebouncedOnClickListener { back() }
            }
        }

        viewBinding.layoutLoadableList.apply {
            setEmptyText(R.string.message_empty_screen_condition_list_title)
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

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewBinding.layoutLoadableList.updateState(conditionList)
        conditionsAdapter.submitList(conditionList)
    }

    private fun onConditionClicked(condition: UiScreenCondition) {
        onConditionSelected(condition.condition)
        back()
    }

    private fun onConditionItemBound(index: Int, itemView: View?) {
        if (index != 0) return

        if (itemView != null) {
            monitoredViewsManager.attach(MonitoredViewType.CONDITION_SELECTOR_DIALOG_ITEM_FIRST, itemView)
        } else {
            monitoredViewsManager.detach(MonitoredViewType.CONDITION_SELECTOR_DIALOG_ITEM_FIRST)
        }
    }
}
