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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.viewholders

import android.text.InputType

import androidx.core.view.isVisible

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setInputFilter
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbGestureMoveBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.DumbGestureDialogUiStateItem.Move
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.getDumbGestureUiStateItemDescription


class DumbGestureMoveViewHolder(
    private val viewBinding: ItemDumbGestureMoveBinding,
    onCollapseClicked: (item: Move) -> Unit,
    onDeleteClicked: (item: Move) -> Unit,
    onMoveUpClicked: (item: Move) -> Unit,
    onMoveDownClicked: (item: Move) -> Unit,
    onSelectPositionClicked: (item: Move) -> Unit,
    onXValueChanged: (item: Move, value: String) -> Unit,
    onYValueChanged: (item: Move, value: String) -> Unit,
    onDurationValueChanged: (item: Move, value: String) -> Unit,
) : BaseDumbGestureViewHolder<Move>(viewBinding) {

    init {
        viewBinding.apply {
            root.setOnClickListener {
                boundItem?.let { if (it.isCollapsed) onCollapseClicked(it) }
            }
            buttonExpandCollapse.setOnClickListener { boundItem?.let { onCollapseClicked(it) } }
            buttonDelete.setOnClickListener { boundItem?.let { onDeleteClicked(it) } }
            buttonMoveUp.setOnClickListener { boundItem?.let { onMoveUpClicked(it) } }
            buttonMoveDown.setOnClickListener { boundItem?.let { onMoveDownClicked(it) } }

            fieldMoveDuration.apply {
                setLabel(R.string.field_title_duration_ms)
                setInputFilter(
                    MinMaxInputFilter(min = 1, max = DumbAction.DumbGesture.GESTURE_MAXIMUM_DURATION_MS.toInt() - 1)
                )
                setOnTextChangedListener { editable ->
                    boundItem?.let { onDurationValueChanged(it, editable.toString()) }
                }
            }
            fieldMovePositionX.apply {
                setLabel(R.string.field_title_position_x)
                setOnTextChangedListener { editable ->
                    boundItem?.let { onXValueChanged(it, editable.toString()) }
                }
            }
            fieldMovePositionY.apply {
                setLabel(R.string.field_title_position_y)
                setOnTextChangedListener { editable ->
                    boundItem?.let { onYValueChanged(it, editable.toString()) }
                }
            }

            buttonSelectPosition.setOnClickListener { boundItem?.let { onSelectPositionClicked(it) } }
        }
    }

    override fun onBind(item: Move) {
        viewBinding.apply {
            if (item.isCollapsed) {
                root.isClickable = true

                motionDesc.isVisible = true
                dividerTop.isVisible = false
                fieldMoveDuration.root.isVisible = false
                fieldMovePositionX.root.isVisible = false
                fieldMovePositionY.root.isVisible = false
                buttonSelectPosition.isVisible = false
                buttonDelete.isVisible = false
                buttonMoveUp.isVisible = false
                buttonMoveDown.isVisible = false

                buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_down)
                motionDesc.text = root.context.getDumbGestureUiStateItemDescription(item)
            } else {
                root.isClickable = false

                motionDesc.isVisible = false
                dividerTop.isVisible = true
                fieldMoveDuration.root.isVisible = true
                fieldMovePositionX.root.isVisible = true
                fieldMovePositionY.root.isVisible = true
                buttonSelectPosition.isVisible = true
                buttonDelete.isVisible = true
                buttonMoveUp.isVisible = true
                buttonMoveDown.isVisible = true
                buttonMoveUp.isEnabled = item.moveableUp
                buttonMoveDown.isEnabled = item.moveableDown

                buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_up)
                fieldMoveDuration.setText(item.durationMs.toString(), InputType.TYPE_CLASS_NUMBER)
                fieldMovePositionX.setText(item.position.x.toString(), InputType.TYPE_CLASS_NUMBER)
                fieldMovePositionY.setText(item.position.y.toString(), InputType.TYPE_CLASS_NUMBER)
            }
        }
    }
}
