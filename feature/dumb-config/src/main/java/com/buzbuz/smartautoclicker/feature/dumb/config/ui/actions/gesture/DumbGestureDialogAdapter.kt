/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbGestureAddBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbGestureHeaderBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbGestureMoveBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbGesturePressBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbGestureReleaseBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbGestureWaitBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.viewholders.BaseDumbGestureViewHolder
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.viewholders.DumbGestureAddViewHolder
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.viewholders.DumbGestureHeaderViewHolder
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.viewholders.DumbGestureMoveViewHolder
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.viewholders.DumbGesturePressViewHolder
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.viewholders.DumbGestureReleaseViewHolder
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.viewholders.DumbGestureWaitViewHolder


class DumbGestureDialogAdapter(
    private val onFingerClicked: (fingerIndex: Int) -> Unit,
    private val onFingerAddClicked: () -> Unit,
    private val onFingerDeleteClicked: () -> Unit,
    private val onFingerRecordClicked: () -> Unit,
    private val onAddMoveClicked: () -> Unit,
    private val onAddWaitClicked: () -> Unit,
    private val onExpandCollapseClicked: (DumbGestureDialogUiStateItem) -> Unit,
    private val onDeleteMotionClicked: (DumbGestureDialogUiStateItem) -> Unit,
    private val onMoveMotionUpClicked: (DumbGestureDialogUiStateItem) -> Unit,
    private val onMoveMotionDownClicked: (DumbGestureDialogUiStateItem) -> Unit,
    private val onSelectPositionClicked: (DumbGestureDialogUiStateItem) -> Unit,
    private val onNameChanged: (String) -> Unit,
    private val onRepeatCountChanged: (String) -> Unit,
    private val onRepeatInfiniteClicked: () -> Unit,
    private val onRepeatDelayChanged: (String) -> Unit,
    private val onStartTimeChanged: (String) -> Unit,
    private val onXValueChanged: (DumbGestureDialogUiStateItem, String) -> Unit,
    private val onYValueChanged: (DumbGestureDialogUiStateItem, String) -> Unit,
    private val onDurationChanged: (DumbGestureDialogUiStateItem, String) -> Unit,
) : ListAdapter<DumbGestureDialogUiStateItem, BaseDumbGestureViewHolder<*>>(DumbGestureMotionItemDiffCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is DumbGestureDialogUiStateItem.Header -> R.layout.item_dumb_gesture_header
            is DumbGestureDialogUiStateItem.Release -> R.layout.item_dumb_gesture_release
            is DumbGestureDialogUiStateItem.Add -> R.layout.item_dumb_gesture_add
            is DumbGestureDialogUiStateItem.Move -> R.layout.item_dumb_gesture_move
            is DumbGestureDialogUiStateItem.Press -> R.layout.item_dumb_gesture_press
            is DumbGestureDialogUiStateItem.Wait -> R.layout.item_dumb_gesture_wait
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseDumbGestureViewHolder<*> {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_dumb_gesture_header -> DumbGestureHeaderViewHolder(
                viewBinding = ItemDumbGestureHeaderBinding.inflate(inflater, parent, false),
                onNameChanged = onNameChanged,
                onRepeatCountChanged = onRepeatCountChanged,
                onRepeatInfiniteClicked = onRepeatInfiniteClicked,
                onRepeatDelayChanged = onRepeatDelayChanged,
                onStartTimeChanged = onStartTimeChanged,
                onFingerSelected = onFingerClicked,
                onFingerAddClicked = onFingerAddClicked,
                onFingerCaptureClicked = onFingerRecordClicked,
                onFingerDeleteClicked = onFingerDeleteClicked,
            )

            R.layout.item_dumb_gesture_press -> DumbGesturePressViewHolder(
                ItemDumbGesturePressBinding.inflate(inflater, parent, false),
                onCollapseClicked = onExpandCollapseClicked,
                onSelectPositionClicked = onSelectPositionClicked,
                onXValueChanged = onXValueChanged,
                onYValueChanged = onYValueChanged,
            )

            R.layout.item_dumb_gesture_move -> DumbGestureMoveViewHolder(
                ItemDumbGestureMoveBinding.inflate(inflater, parent, false),
                onCollapseClicked = onExpandCollapseClicked,
                onDeleteClicked = onDeleteMotionClicked,
                onMoveUpClicked = onMoveMotionUpClicked,
                onMoveDownClicked = onMoveMotionDownClicked,
                onSelectPositionClicked = onSelectPositionClicked,
                onXValueChanged = onXValueChanged,
                onYValueChanged = onYValueChanged,
                onDurationValueChanged = onDurationChanged,
            )

            R.layout.item_dumb_gesture_wait -> DumbGestureWaitViewHolder(
                ItemDumbGestureWaitBinding.inflate(inflater, parent, false),
                onCollapseClicked = onExpandCollapseClicked,
                onDeleteClicked = onDeleteMotionClicked,
                onMoveUpClicked = onMoveMotionUpClicked,
                onMoveDownClicked = onMoveMotionDownClicked,
                onDurationMsChanged = onDurationChanged,
            )

            R.layout.item_dumb_gesture_release -> DumbGestureReleaseViewHolder(
                ItemDumbGestureReleaseBinding.inflate(inflater, parent, false),
            )

            R.layout.item_dumb_gesture_add -> DumbGestureAddViewHolder(
                viewBinding = ItemDumbGestureAddBinding.inflate(inflater, parent, false),
                onAddMoveClicked = onAddMoveClicked,
                onAddWaitClicked = onAddWaitClicked,
            )

            else -> throw IllegalArgumentException("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseDumbGestureViewHolder<*>, position: Int, payloads: MutableList<Any>) {
        if (payloads.any { it === DumbGestureMotionItemDiffCallback.HEADER_FINGER_DESC_CHANGED }) {
            if (holder is DumbGestureHeaderViewHolder) {
                (getItem(position) as? DumbGestureDialogUiStateItem.Header)?.let { holder.updateFingerDesc(it) }
            }
            return
        }

        // For TEXT_FIELD_CHANGED the non-null payload already suppresses DefaultItemAnimator's cross-fade.
        // We still do a full rebind so an externally-set value (e.g. from the position selector) is
        // displayed; setText skips the update when the field has focus, so there is no flicker
        // while the user is typing.
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: BaseDumbGestureViewHolder<*>, position: Int) {
        when (holder) {
            is DumbGestureHeaderViewHolder ->
                (getItem(position) as? DumbGestureDialogUiStateItem.Header)?.let { holder.bind(it) }
            is DumbGestureMoveViewHolder ->
                (getItem(position) as? DumbGestureDialogUiStateItem.Move)?.let { holder.bind(it) }
            is DumbGesturePressViewHolder ->
                (getItem(position) as? DumbGestureDialogUiStateItem.Press)?.let { holder.bind(it) }
            is DumbGestureWaitViewHolder ->
                (getItem(position) as? DumbGestureDialogUiStateItem.Wait)?.let { holder.bind(it) }

            is DumbGestureAddViewHolder,
            is DumbGestureReleaseViewHolder -> Unit
        }
    }

    override fun onViewRecycled(holder: BaseDumbGestureViewHolder<*>) {
        holder.unbind()
        super.onViewRecycled(holder)
    }
}

private object DumbGestureMotionItemDiffCallback : DiffUtil.ItemCallback<DumbGestureDialogUiStateItem>() {

    // Returned as payload when only the text-input fields (x, y, duration) changed. A non-null payload
    // makes DefaultItemAnimator reuse the ViewHolder instead of cross-fading a new one in, so the field
    // being typed in keeps its focus and the list does not blink. isCollapsed changes are excluded: they
    // still get the cross-fade.
    val TEXT_FIELD_CHANGED = Any()

    // Returned when only the Header's currentFingerStartTimeMs changed.
    // The payloads override updates fingerDesc without touching fieldStartTime,
    // preventing a blink on the input the user is actively editing.
    val HEADER_FINGER_DESC_CHANGED = Any()

    override fun areItemsTheSame(oldItem: DumbGestureDialogUiStateItem, newItem: DumbGestureDialogUiStateItem): Boolean =
        if (oldItem::class != newItem::class) false
        else when (oldItem) {
            is DumbGestureDialogUiStateItem.Collapsable ->
                newItem is DumbGestureDialogUiStateItem.Collapsable && oldItem.id == newItem.id

            is DumbGestureDialogUiStateItem.Add ->
                newItem is DumbGestureDialogUiStateItem.Add && oldItem.fingerIndex == newItem.fingerIndex

            is DumbGestureDialogUiStateItem.Release ->
                newItem is DumbGestureDialogUiStateItem.Release && oldItem.fingerIndex == newItem.fingerIndex

            is DumbGestureDialogUiStateItem.Header -> true
        }

    override fun areContentsTheSame(oldItem: DumbGestureDialogUiStateItem, newItem: DumbGestureDialogUiStateItem): Boolean =
        oldItem == newItem

    override fun getChangePayload(oldItem: DumbGestureDialogUiStateItem, newItem: DumbGestureDialogUiStateItem): Any? =
        when (oldItem) {
            is DumbGestureDialogUiStateItem.Press if newItem is DumbGestureDialogUiStateItem.Press &&
                    oldItem.isCollapsed == newItem.isCollapsed -> TEXT_FIELD_CHANGED

            is DumbGestureDialogUiStateItem.Move if newItem is DumbGestureDialogUiStateItem.Move &&
                    oldItem.isCollapsed == newItem.isCollapsed -> TEXT_FIELD_CHANGED

            is DumbGestureDialogUiStateItem.Wait if newItem is DumbGestureDialogUiStateItem.Wait &&
                    oldItem.isCollapsed == newItem.isCollapsed -> TEXT_FIELD_CHANGED

            is DumbGestureDialogUiStateItem.Header if newItem is DumbGestureDialogUiStateItem.Header &&
                    oldItem.fingerIndex == newItem.fingerIndex &&
                    oldItem.fingerCount == newItem.fingerCount &&
                    oldItem.maxFingerGestureDurationsMs == newItem.maxFingerGestureDurationsMs ->
                if (oldItem.copy(currentFingerStartTimeMs = 0, fingerGestureDurationMs = 0) ==
                    newItem.copy(currentFingerStartTimeMs = 0, fingerGestureDurationMs = 0)) HEADER_FINGER_DESC_CHANGED
                else TEXT_FIELD_CHANGED

            else -> null
        }
}
