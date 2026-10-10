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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.viewholders

import android.text.InputType
import androidx.core.view.isVisible

import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setInputFilter
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGestureWaitBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.GestureDialogUiStateItem.Wait
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.getGestureUiStateItemDescription
import kotlinx.coroutines.Job


class GestureWaitViewHolder(
    private val viewBinding: ItemGestureWaitBinding,
    onCollapseClicked: (item: Wait) -> Unit,
    onDeleteClicked: (item: Wait) -> Unit,
    onMoveUpClicked: (item: Wait) -> Unit,
    onMoveDownClicked: (item: Wait) -> Unit,
    onDurationMsChanged: (item: Wait, value: String) -> Unit,
) : BaseGestureViewHolder<Wait>(viewBinding) {

    init {
        viewBinding.root.setOnClickListener {
            boundItem?.let { if (it.isCollapsed) onCollapseClicked(it) }
        }
        viewBinding.buttonExpandCollapse.setOnClickListener { boundItem?.let { onCollapseClicked(it) } }
        viewBinding.buttonDelete.setOnClickListener { boundItem?.let { onDeleteClicked(it) } }
        viewBinding.buttonMoveUp.setOnClickListener { boundItem?.let { onMoveUpClicked(it) } }
        viewBinding.buttonMoveDown.setOnClickListener { boundItem?.let { onMoveDownClicked(it) } }
        viewBinding.fieldPressDuration.apply {
            setLabel(R.string.field_title_duration_ms)
            setInputFilter(MinMaxInputFilter(min = 1, max = Gesture.GESTURE_MAXIMUM_DURATION_MS.toInt() - 1))
            setOnTextChangedListener { editable ->
                boundItem?.let { onDurationMsChanged(it, editable.toString()) }
            }
        }
    }

    override fun onBind(item: Wait): Job? {
        viewBinding.apply {
            if (item.isCollapsed) {
                viewBinding.root.isClickable = true

                motionDesc.isVisible = true
                fieldPressDuration.root.isVisible = false
                buttonDelete.isVisible = false
                buttonMoveUp.isVisible = false
                buttonMoveDown.isVisible = false
                dividerTop.isVisible = false

                buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_down)
                motionDesc.text = root.context.getGestureUiStateItemDescription(item)
            } else {
                viewBinding.root.isClickable = false

                motionDesc.isVisible = false
                fieldPressDuration.root.isVisible = true
                buttonDelete.isVisible = true
                buttonMoveUp.isVisible = true
                buttonMoveDown.isVisible = true
                buttonMoveUp.isEnabled = item.moveableUp
                buttonMoveDown.isEnabled = item.moveableDown
                dividerTop.isVisible = true

                buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_up)
                fieldPressDuration.setText(item.durationMs.toString(), InputType.TYPE_CLASS_NUMBER)
            }
        }

        return null
    }
}