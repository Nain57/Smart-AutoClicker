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

import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbGesturePressBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.DumbGestureDialogUiStateItem.Press
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.getDumbGestureUiStateItemDescription


class DumbGesturePressViewHolder(
    private val viewBinding: ItemDumbGesturePressBinding,
    onCollapseClicked: (item: Press) -> Unit,
    onSelectPositionClicked: (item: Press) -> Unit,
    onXValueChanged: (item: Press, value: String) -> Unit,
    onYValueChanged: (item: Press, value: String) -> Unit,
) : BaseDumbGestureViewHolder<Press>(viewBinding) {

    init {
        viewBinding.apply {
            root.setOnClickListener {
                boundItem?.let { if (it.isCollapsed) onCollapseClicked(it) }
            }
            buttonExpandCollapse.setOnClickListener { boundItem?.let { onCollapseClicked(it) } }

            fieldPressPositionX.apply {
                setLabel(R.string.field_title_position_x)
                setOnTextChangedListener { editable ->
                    boundItem?.let { onXValueChanged(it, editable.toString()) }
                }
            }
            fieldPressPositionY.apply {
                setLabel(R.string.field_title_position_y)
                setOnTextChangedListener { editable ->
                    boundItem?.let { onYValueChanged(it, editable.toString()) }
                }
            }

            buttonSelectPosition.setOnClickListener { boundItem?.let { onSelectPositionClicked(it) } }
        }
    }

    override fun onBind(item: Press) {
        viewBinding.apply {
            if (item.isCollapsed) {
                root.isClickable = true

                motionDesc.isVisible = true
                dividerTop.isVisible = false
                fieldPressPositionX.root.isVisible = false
                fieldPressPositionY.root.isVisible = false
                buttonSelectPosition.isVisible = false

                buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_down)
                motionDesc.text = root.context.getDumbGestureUiStateItemDescription(item)
            } else {
                root.isClickable = false

                motionDesc.isVisible = false
                dividerTop.isVisible = true
                fieldPressPositionX.root.isVisible = true
                fieldPressPositionY.root.isVisible = true
                buttonSelectPosition.isVisible = true

                buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_up)
                fieldPressPositionX.setText(item.position.x.toString(), InputType.TYPE_CLASS_NUMBER)
                fieldPressPositionY.setText(item.position.y.toString(), InputType.TYPE_CLASS_NUMBER)
            }
        }
    }
}
