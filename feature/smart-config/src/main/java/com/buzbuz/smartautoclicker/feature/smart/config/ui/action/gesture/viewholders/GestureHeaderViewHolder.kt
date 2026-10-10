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
import com.google.android.material.color.MaterialColors

import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setFingerCount
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setOnAddClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setOnFingerClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGestureHeaderBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.GestureDialogUiStateItem

import kotlinx.coroutines.Job

import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR


class GestureHeaderViewHolder(
    private val viewBinding: ItemGestureHeaderBinding,
    onNameChanged: (String) -> Unit,
    onStartTimeChanged: (String) -> Unit,
    onFingerSelected: (fingerIndex: Int) -> Unit,
    onFingerAddClicked: () -> Unit,
    onFingerCaptureClicked: () -> Unit,
    onFingerDeleteClicked: () -> Unit,
) : BaseGestureViewHolder<GestureDialogUiStateItem.Header>(viewBinding) {

    init {
        viewBinding.fieldName.apply {
            setLabel(R.string.generic_name)
            setOnTextChangedListener { editable -> onNameChanged(editable.toString()) }
        }

        viewBinding.fieldFingerSelector.apply {
            setOnAddClickedListener { onFingerAddClicked() }
            setOnFingerClickedListener { index -> onFingerSelected(index) }
        }

        viewBinding.fieldStartTime.apply {
            setLabel(R.string.item_header_finger_start_at)
            setOnTextChangedListener { editable -> onStartTimeChanged(editable.toString()) }
        }

        viewBinding.buttonCapture.setOnClickListener { onFingerCaptureClicked() }
        viewBinding.buttonDelete.setOnClickListener { onFingerDeleteClicked() }

    }

    override fun onBind(item: GestureDialogUiStateItem.Header): Job? {
        viewBinding.fieldName.setText(item.actionName)

        viewBinding.fieldFingerSelector.apply {
            setFingerCount(item.fingerCount)
            setChecked(item.fingerIndex, item.fingerErrorState)
        }

        viewBinding.buttonDelete.isEnabled = item.fingerIndex != 0
        viewBinding.fingerTitle.text = viewBinding.root.context
            .getString(R.string.item_header_finger_title, item.fingerIndex + 1)
        updateFingerDesc(item)

        viewBinding.fieldStartTime.apply {
            if (item.fingerIndex == 0) {
                setText("0", InputType.TYPE_CLASS_NUMBER)
                root.isEnabled = false
            } else {
                setText(item.currentFingerStartTimeMs.toString(), InputType.TYPE_CLASS_NUMBER)
                root.isEnabled = true
            }
        }

        return null
    }

    fun updateFingerDesc(item: GestureDialogUiStateItem.Header) {
        val isOverDuration = item.fingerGestureDurationMs >= item.maxFingerGestureDurationsMs

        viewBinding.fingerDesc.apply {
            text = context.getString(
                R.string.item_header_finger_desc,
                item.fingerGestureDurationMs,
                item.maxFingerGestureDurationsMs,
            )
            setTextColor(
                if (isOverDuration) MaterialColors.getColor(this, AppCompatR.attr.colorError)
                else MaterialColors.getColor(this, MaterialR.attr.colorOnSurface)
            )
        }
    }
}