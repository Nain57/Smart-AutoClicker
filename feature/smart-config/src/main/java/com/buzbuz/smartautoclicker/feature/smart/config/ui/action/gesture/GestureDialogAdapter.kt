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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGestureAddBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGestureHeaderBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGestureMoveBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGesturePressBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGestureReleaseBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGestureWaitBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.viewholders.BaseGestureViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.viewholders.GestureAddViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.viewholders.GestureHeaderViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.viewholders.GestureMoveViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.viewholders.GesturePressViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.viewholders.GestureReleaseViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.viewholders.GestureWaitViewHolder
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.UiGesturePosition

import kotlinx.coroutines.Job


class GestureDialogAdapter(
    private val onFingerClicked: (fingerIndex: Int) -> Unit,
    private val onFingerAddClicked: () -> Unit,
    private val onFingerDeleteClicked: () -> Unit,
    private val onFingerRecordClicked: () -> Unit,
    private val onAddMoveClicked: () -> Unit,
    private val onAddWaitClicked: () -> Unit,
    private val onExpandCollapseClicked: (GestureDialogUiStateItem) -> Unit,
    private val onDeleteMotionClicked: (GestureDialogUiStateItem) -> Unit,
    private val onMoveMotionUpClicked: (GestureDialogUiStateItem) -> Unit,
    private val onMoveMotionDownClicked: (GestureDialogUiStateItem) -> Unit,
    private val onSetCounterXClicked: (GestureDialogUiStateItem) -> Unit,
    private val onSetCounterYClicked: (GestureDialogUiStateItem) -> Unit,
    private val onSelectPositionClicked: (GestureDialogUiStateItem) -> Unit,
    private val onSelectConditionClicked: (GestureDialogUiStateItem) -> Unit,
    private val onRemoveConditionClicked: (GestureDialogUiStateItem) -> Unit,
    private val onOffsetFieldClicked: (GestureDialogUiStateItem) -> Unit,
    private val onNameChanged: (String) -> Unit,
    private val onStartTimeChanged: (String) -> Unit,
    private val onXValueChanged: (GestureDialogUiStateItem, String) -> Unit,
    private val onYValueChanged: (GestureDialogUiStateItem, String) -> Unit,
    private val onDurationChanged: (GestureDialogUiStateItem, String) -> Unit,
    private val bitmapProvider: (ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
) : ListAdapter<GestureDialogUiStateItem, BaseGestureViewHolder<*>>(GestureMotionItemDiffCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is GestureDialogUiStateItem.Header -> R.layout.item_gesture_header
            is GestureDialogUiStateItem.Release -> R.layout.item_gesture_release
            is GestureDialogUiStateItem.Add -> R.layout.item_gesture_add
            is GestureDialogUiStateItem.Move -> R.layout.item_gesture_move
            is GestureDialogUiStateItem.Press -> R.layout.item_gesture_press
            is GestureDialogUiStateItem.Wait -> R.layout.item_gesture_wait
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseGestureViewHolder<*> {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_gesture_header -> GestureHeaderViewHolder(
                viewBinding = ItemGestureHeaderBinding.inflate(inflater, parent, false),
                onNameChanged = onNameChanged,
                onStartTimeChanged = onStartTimeChanged,
                onFingerSelected = onFingerClicked,
                onFingerAddClicked = onFingerAddClicked,
                onFingerCaptureClicked = onFingerRecordClicked,
                onFingerDeleteClicked = onFingerDeleteClicked,
            )

            R.layout.item_gesture_press -> GesturePressViewHolder(
                ItemGesturePressBinding.inflate(inflater, parent, false),
                onCollapseClicked = onExpandCollapseClicked,
                onSetCounterXClicked = onSetCounterXClicked,
                onSetCounterYClicked = onSetCounterYClicked,
                onSelectPositionClicked = onSelectPositionClicked,
                onSelectConditionClicked = onSelectConditionClicked,
                onRemoveConditionClicked = onRemoveConditionClicked,
                onSetOffsetClicked = onOffsetFieldClicked,
                onXValueChanged = onXValueChanged,
                onYValueChanged = onYValueChanged,
                bitmapProvider = bitmapProvider,
            )

            R.layout.item_gesture_move -> GestureMoveViewHolder(
                ItemGestureMoveBinding.inflate(inflater, parent, false),
                onCollapseClicked = onExpandCollapseClicked,
                onDeleteClicked = onDeleteMotionClicked,
                onMoveUpClicked = onMoveMotionUpClicked,
                onMoveDownClicked = onMoveMotionDownClicked,
                onSetCounterXClicked = onSetCounterXClicked,
                onSetCounterYClicked = onSetCounterYClicked,
                onSelectPositionClicked = onSelectPositionClicked,
                onSelectConditionClicked = onSelectConditionClicked,
                onRemoveConditionClicked = onRemoveConditionClicked,
                onSetOffsetClicked = onOffsetFieldClicked,
                onXValueChanged = onXValueChanged,
                onYValueChanged = onYValueChanged,
                onDurationValueChanged = onDurationChanged,
                bitmapProvider = bitmapProvider,
            )

            R.layout.item_gesture_wait -> GestureWaitViewHolder(
                ItemGestureWaitBinding.inflate(inflater, parent, false),
                onCollapseClicked = onExpandCollapseClicked,
                onDeleteClicked = onDeleteMotionClicked,
                onMoveUpClicked = onMoveMotionUpClicked,
                onMoveDownClicked = onMoveMotionDownClicked,
                onDurationMsChanged = onDurationChanged,
            )

            R.layout.item_gesture_release -> GestureReleaseViewHolder(
                ItemGestureReleaseBinding.inflate(inflater, parent, false),
            )

            R.layout.item_gesture_add -> GestureAddViewHolder(
                viewBinding = ItemGestureAddBinding.inflate(inflater, parent, false),
                onAddMoveClicked = onAddMoveClicked,
                onAddWaitClicked = onAddWaitClicked,
            )

            else -> throw IllegalArgumentException("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseGestureViewHolder<*>, position: Int, payloads: MutableList<Any>) {
        if (payloads.any { it === GestureMotionItemDiffCallback.HEADER_FINGER_DESC_CHANGED }) {
            if (holder is GestureHeaderViewHolder) {
                (getItem(position) as? GestureDialogUiStateItem.Header)?.let { holder.updateFingerDesc(it) }
            }
            return
        }

        // For TEXT_FIELD_CHANGED the non-null payload already suppresses DefaultItemAnimator's cross-fade.
        // We still do a full rebind so an externally-set value (e.g. from GestureEditorOverlayMenu) is
        // displayed; setNumericValue skips the update when the field has focus, so there is no flicker
        // while the user is typing.
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: BaseGestureViewHolder<*>, position: Int) {
        when (holder) {
            is GestureHeaderViewHolder ->
                (getItem(position) as? GestureDialogUiStateItem.Header)?.let { holder.bind(it) }
            is GestureMoveViewHolder ->
                (getItem(position) as? GestureDialogUiStateItem.Move)?.let { holder.bind(it) }
            is GesturePressViewHolder ->
                (getItem(position) as? GestureDialogUiStateItem.Press)?.let { holder.bind(it) }
            is GestureWaitViewHolder ->
                (getItem(position) as? GestureDialogUiStateItem.Wait)?.let { holder.bind(it) }

            is GestureAddViewHolder,
            is GestureReleaseViewHolder -> Unit
        }
    }

    override fun onViewRecycled(holder: BaseGestureViewHolder<*>) {
        holder.unbind()
        super.onViewRecycled(holder)
    }
}

private object GestureMotionItemDiffCallback : DiffUtil.ItemCallback<GestureDialogUiStateItem>() {

    // Returned as payload when only the text-input fields (x, y, duration) changed, whether they hold
    // a literal or a counter value. A non-null payload makes DefaultItemAnimator reuse the ViewHolder
    // instead of cross-fading a new one in, so the field being typed in keeps its focus and the list
    // does not blink. isCollapsed changes are excluded: they still get the cross-fade.
    val TEXT_FIELD_CHANGED = Any()

    // Returned when only the Header's currentFingerStartTimeMs changed.
    // The payloads override updates fingerDesc without touching fieldStartTime,
    // preventing a blink on the input the user is actively editing.
    val HEADER_FINGER_DESC_CHANGED = Any()

    override fun areItemsTheSame(oldItem: GestureDialogUiStateItem, newItem: GestureDialogUiStateItem): Boolean =
        if (oldItem::class != newItem::class) false
        else when (oldItem) {
            is GestureDialogUiStateItem.Collapsable ->
                newItem is GestureDialogUiStateItem.Collapsable && oldItem.id == newItem.id

            is GestureDialogUiStateItem.Add ->
                newItem is GestureDialogUiStateItem.Add && oldItem.fingerIndex == newItem.fingerIndex

            is GestureDialogUiStateItem.Release ->
                newItem is GestureDialogUiStateItem.Release && oldItem.fingerIndex == newItem.fingerIndex

            is GestureDialogUiStateItem.Header -> true
        }

    override fun areContentsTheSame(oldItem: GestureDialogUiStateItem, newItem: GestureDialogUiStateItem): Boolean =
        oldItem == newItem

    override fun getChangePayload(oldItem: GestureDialogUiStateItem, newItem: GestureDialogUiStateItem): Any? =
        when (oldItem) {
            is GestureDialogUiStateItem.Press if newItem is GestureDialogUiStateItem.Press &&
                    oldItem.isCollapsed == newItem.isCollapsed &&
                    oldItem.offsetPosition == newItem.offsetPosition &&
                    oldItem.position.hasOnlyLiteralValues() &&
                    newItem.position.hasOnlyLiteralValues() -> TEXT_FIELD_CHANGED

            is GestureDialogUiStateItem.Move if newItem is GestureDialogUiStateItem.Move &&
                    oldItem.isCollapsed == newItem.isCollapsed &&
                    oldItem.offsetPosition == newItem.offsetPosition &&
                    (oldItem.position.hasOnlyLiteralValues() && newItem.position.hasOnlyLiteralValues() ||
                     oldItem.position == newItem.position) -> TEXT_FIELD_CHANGED

            is GestureDialogUiStateItem.Wait if newItem is GestureDialogUiStateItem.Wait &&
                    oldItem.isCollapsed == newItem.isCollapsed -> TEXT_FIELD_CHANGED

            is GestureDialogUiStateItem.Header if newItem is GestureDialogUiStateItem.Header &&
                    oldItem.actionName == newItem.actionName &&
                    oldItem.fingerIndex == newItem.fingerIndex &&
                    oldItem.fingerCount == newItem.fingerCount &&
                    oldItem.maxFingerGestureDurationsMs == newItem.maxFingerGestureDurationsMs -> HEADER_FINGER_DESC_CHANGED

            else -> null
        }

    private fun UiGesturePosition.hasOnlyLiteralValues(): Boolean =
        this is UiGesturePosition.Position && !xValue.startsWith("{") && !yValue.startsWith("{")

}
