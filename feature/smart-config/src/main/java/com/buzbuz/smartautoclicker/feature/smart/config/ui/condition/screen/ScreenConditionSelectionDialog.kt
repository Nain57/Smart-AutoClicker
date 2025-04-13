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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.ui.di.UiEntryPoint
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseSelectionBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageConditionGridBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.bind
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiImageCondition

import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.EntryPoints

import kotlinx.coroutines.Job

class ScreenConditionSelectionDialog(
    private val conditionList: List<UiImageCondition>,
    bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val onConditionSelected: (ImageCondition) -> Unit,
): OverlayDialog(R.style.ScenarioConfigTheme) {

    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager by lazy {
        EntryPoints.get(context.applicationContext, UiEntryPoint::class.java).monitoredViewManager()
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseSelectionBinding

    /** Adapter for the list of condition. */
    private val conditionsAdapter = ImageConditionsAdapter(
        bitmapProvider = bitmapProvider,
        onConditionSelected = ::onConditionClicked,
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

    private fun onConditionClicked(condition: ImageCondition) {
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
private class ImageConditionsAdapter(
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val onConditionSelected: (ImageCondition) -> Unit,
    private val itemViewBound: ((Int, View?) -> Unit),
) : ListAdapter<UiImageCondition, ImageConditionViewHolder>(ImageConditionsDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageConditionViewHolder =
        ImageConditionViewHolder(
            ItemImageConditionGridBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bitmapProvider,
            onConditionSelected,
        )

    override fun onBindViewHolder(holder: ImageConditionViewHolder, position: Int) {
        holder.onBind(getItem(position))
        itemViewBound(position, holder.viewBinding.cardImageCondition.root)
    }

    override fun onViewRecycled(holder: ImageConditionViewHolder) {
        holder.onUnbind()
        itemViewBound(holder.bindingAdapterPosition, null)
        super.onViewRecycled(holder)
    }
}

/** DiffUtil Callback comparing two items when updating the [ImageConditionsAdapter] list. */
private object ImageConditionsDiffUtilCallback: DiffUtil.ItemCallback<UiImageCondition>() {
    override fun areItemsTheSame(oldItem: UiImageCondition, newItem: UiImageCondition): Boolean =
        oldItem.condition.id == newItem.condition.id
    override fun areContentsTheSame(oldItem: UiImageCondition, newItem: UiImageCondition): Boolean =
        oldItem == newItem
}

/**
 * ViewHolder for an Condition.
 *
 * @param viewBinding the view binding for this view holder views.
 * @param bitmapProvider provides the conditions bitmap.
 * @param onConditionSelected called when the user select a condition.
 */
private class ImageConditionViewHolder(
    val viewBinding: ItemImageConditionGridBinding,
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val onConditionSelected: (ImageCondition) -> Unit,
): RecyclerView.ViewHolder(viewBinding.root) {

    /** Job for the loading of the condition bitmap. Null until bound. */
    private var bitmapLoadingJob: Job? = null

    fun onBind(condition: UiImageCondition) {
        bitmapLoadingJob?.cancel()
        bitmapLoadingJob = viewBinding.cardImageCondition.bind(
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