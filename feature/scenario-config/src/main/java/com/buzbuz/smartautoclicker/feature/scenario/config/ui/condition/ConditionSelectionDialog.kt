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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.DialogBaseSelectionBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ItemConditionBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.bind

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.Job

class ConditionSelectionDialog(
    private val conditionList: List<Condition>,
    bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val onConditionSelected: (Condition) -> Unit,
): OverlayDialog(R.style.ScenarioConfigTheme) {

    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseSelectionBinding

    /** Adapter for the list of condition. */
    private val conditionsAdapter = ConditionsAdapter(
        bitmapProvider = bitmapProvider,
        onConditionSelected = ::onConditionClicked,
        itemViewBound = ::onConditionItemBound,
    )

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseSelectionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_condition_selection)
                buttonSave.visibility = View.GONE
                buttonDismiss.setOnClickListener { debounceUserInteraction { back() } }
            }
        }

        viewBinding.layoutLoadableList.apply {
            setEmptyText(R.string.message_empty_conditions)
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

    private fun onConditionClicked(condition: Condition) {
        onConditionSelected(condition)
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

/**
 * Adapter for the list of condition.
 * @param onConditionSelected listener on user click on a condition.
 */
private class ConditionsAdapter(
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val onConditionSelected: (Condition) -> Unit,
    private val itemViewBound: ((Int, View?) -> Unit),
) : ListAdapter<Condition, ConditionViewHolder>(ConditionsDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConditionViewHolder =
        ConditionViewHolder(
            ItemConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
            onConditionSelected,
        )

    override fun onBindViewHolder(holder: ConditionViewHolder, position: Int) {
        holder.onBind(getItem(position))
        itemViewBound(position, holder.itemView)
    }

    override fun onViewRecycled(holder: ConditionViewHolder) {
        holder.onUnbind()
        itemViewBound(holder.bindingAdapterPosition, null)
        super.onViewRecycled(holder)
    }
}

/** DiffUtil Callback comparing two items when updating the [ConditionsAdapter] list. */
private object ConditionsDiffUtilCallback: DiffUtil.ItemCallback<Condition>() {
    override fun areItemsTheSame(oldItem: Condition, newItem: Condition): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Condition, newItem: Condition): Boolean = oldItem == newItem
}

/**
 * ViewHolder for an Condition.
 *
 * @param viewBinding the view binding for this view holder views.
 * @param bitmapProvider provides the conditions bitmap.
 * @param onConditionSelected called when the user select a condition.
 */
private class ConditionViewHolder(
    private val viewBinding: ItemConditionBinding,
    private val bitmapProvider: (Condition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val onConditionSelected: (Condition) -> Unit,
): RecyclerView.ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    fun onBind(condition: Condition) {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = viewBinding.bind(
            condition,
            bitmapProvider,
            onConditionSelected,
        )
    }

    /** Unbind this view holder for a previously bound data model. */
    fun onUnbind() {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = null
    }
}