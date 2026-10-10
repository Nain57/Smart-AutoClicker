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

import android.graphics.Bitmap
import androidx.core.view.isVisible
import com.buzbuz.smartautoclicker.core.base.extensions.setLeftCompoundDrawable
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckboxClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setup
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGesturePressBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.GestureDialogUiStateItem.Press
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.bindings.bindCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.bindings.enableClearCounterOnType
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.bindings.setPositionValue
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.UiGesturePosition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.getGestureUiStateItemDescription
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.getOffsetDescription
import kotlinx.coroutines.Job


class GesturePressViewHolder(
    private val viewBinding: ItemGesturePressBinding,
    private val bitmapProvider: (ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    onCollapseClicked: (item: Press) -> Unit,
    onSetCounterXClicked: (item: Press) -> Unit,
    onSetCounterYClicked: (item: Press) -> Unit,
    onSelectPositionClicked: (item: Press) -> Unit,
    onSelectConditionClicked: (item: Press) -> Unit,
    onRemoveConditionClicked: (item: Press) -> Unit,
    onSetOffsetClicked: (item: Press) -> Unit,
    onXValueChanged: (item: Press, value: String) -> Unit,
    onYValueChanged: (item: Press, value: String) -> Unit,
) : BaseGestureViewHolder<Press>(viewBinding) {

    init {
        viewBinding.apply {
            root.setOnClickListener {
                boundItem?.let { if (it.isCollapsed) onCollapseClicked(it) }
            }
            buttonExpandCollapse.setOnClickListener { boundItem?.let { onCollapseClicked(it) } }
            fieldPosition.fieldX.apply {
                setup(
                    label = R.string.field_title_position_x,
                    icon = R.drawable.ic_append_counter,
                    disableInputWithCheckbox = false,
                )
                enableClearCounterOnType()
                setOnTextChangedListener { editable ->
                    boundItem?.let { onXValueChanged(it, editable.toString()) }
                }
                setOnCheckboxClickedListener { boundItem?.let { onSetCounterXClicked(it) } }
            }
            fieldPosition.fieldY.apply {
                setup(
                    label = R.string.field_title_position_y,
                    icon = R.drawable.ic_append_counter,
                    disableInputWithCheckbox = false,
                )
                enableClearCounterOnType()
                setOnTextChangedListener { editable ->
                    boundItem?.let { onYValueChanged(it, editable.toString()) }
                }
                setOnCheckboxClickedListener { boundItem?.let { onSetCounterYClicked(it) } }
            }
            fieldPosition.buttonSelectPosition.setOnClickListener {
                boundItem?.let { onSelectPositionClicked(it) }
            }
            fieldPosition.buttonSelectCondition.setOnClickListener {
                boundItem?.let { item ->
                    if (item.position is UiGesturePosition.Condition) onRemoveConditionClicked(item)
                    else onSelectConditionClicked(item)
                }
            }
            fieldPosition.conditionSplash.conditionCard.setOnClickListener {
                boundItem?.let { item ->
                    if (item.position is UiGesturePosition.Condition
                        && item.position.condition != null && !item.position.inError) {
                        onSelectConditionClicked(item)
                    }
                }
            }
            fieldOffset.setOnClickListener { boundItem?.let { onSetOffsetClicked(it) } }

            viewBinding.fieldOffset.setTitle(itemView.context.getString(R.string.field_click_offset_title))
        }
    }

    override fun onBind(item: Press): Job? {
        var displayWorkJob: Job? = null

        viewBinding.apply {
            motionTitle.setLeftCompoundDrawable(
                if (item.position is UiGesturePosition.Condition && item.position.inError)
                    R.drawable.ic_badge_error
                else null
            )

            if (item.isCollapsed) {
                viewBinding.root.isClickable = true

                motionDesc.isVisible = true
                fieldPosition.root.isVisible = false
                fieldOffset.root.isVisible = false
                dividerTop.isVisible = false
                dividerMiddle.isVisible = false

                buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_down)
                motionDesc.text = root.context.getGestureUiStateItemDescription(item)
            } else {
                viewBinding.root.isClickable = false

                motionDesc.isVisible = false
                fieldPosition.root.isVisible = true
                fieldOffset.root.isVisible = true
                dividerTop.isVisible = true
                dividerMiddle.isVisible = true

                when (item.position) {
                    is UiGesturePosition.Condition -> {
                        fieldPosition.buttonSelectCondition.setText(R.string.button_title_condition_remove)
                        fieldPosition.buttonSelectCondition.setIconResource(R.drawable.ic_cancel)
                        displayWorkJob = fieldPosition.conditionSplash.bindCondition(
                            condition = item.position.condition,
                            inError = item.position.inError,
                            bitmapProvider = bitmapProvider,
                        )
                    }


                    is UiGesturePosition.Position -> {
                        fieldPosition.conditionSplash.root.isVisible = false
                        fieldPosition.buttonSelectCondition.setText(R.string.button_title_condition_pick)
                        fieldPosition.buttonSelectCondition.setIconResource(R.drawable.ic_screen_event)
                        fieldPosition.fieldX.setPositionValue(item.position.xValue)
                        fieldPosition.fieldY.setPositionValue(item.position.yValue)
                    }
                }

                buttonExpandCollapse.setIconResource(R.drawable.ic_chevron_up)
                fieldOffset.setDescription(root.context.getOffsetDescription(item.offsetPosition))
            }
        }

        return displayWorkJob
    }
}