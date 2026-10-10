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

import android.text.InputFilter
import android.text.InputType

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_COUNT_MAX_VALUE
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_COUNT_MIN_VALUE
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_DELAY_MAX_MS
import com.buzbuz.smartautoclicker.core.dumb.domain.model.REPEAT_DELAY_MIN_MS
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setFingerCount
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setOnAddClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.setOnFingerClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setInputFilter
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setNumericValue
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnCheckboxClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setup
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbGestureHeaderBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.gesture.DumbGestureDialogUiStateItem.Header

import com.google.android.material.color.MaterialColors

import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR


class DumbGestureHeaderViewHolder(
    private val viewBinding: ItemDumbGestureHeaderBinding,
    onNameChanged: (String) -> Unit,
    onRepeatCountChanged: (String) -> Unit,
    onRepeatInfiniteClicked: () -> Unit,
    onRepeatDelayChanged: (String) -> Unit,
    onStartTimeChanged: (String) -> Unit,
    onFingerSelected: (fingerIndex: Int) -> Unit,
    onFingerAddClicked: () -> Unit,
    onFingerCaptureClicked: () -> Unit,
    onFingerDeleteClicked: () -> Unit,
) : BaseDumbGestureViewHolder<Header>(viewBinding) {

    init {
        viewBinding.fieldName.apply {
            setLabel(R.string.input_field_label_name)
            setInputFilter(InputFilter.LengthFilter(itemView.resources.getInteger(R.integer.name_max_length)))
            setOnTextChangedListener { editable -> onNameChanged(editable.toString()) }
        }

        viewBinding.fieldRepeat.apply {
            textField.filters = arrayOf(MinMaxInputFilter(REPEAT_COUNT_MIN_VALUE, REPEAT_COUNT_MAX_VALUE))
            setup(R.string.input_field_label_repeat_count, R.drawable.ic_infinite, disableInputWithCheckbox = true)
            setOnTextChangedListener { editable -> onRepeatCountChanged(editable.toString()) }
            setOnCheckboxClickedListener(onRepeatInfiniteClicked)
        }

        viewBinding.fieldRepeatDelay.apply {
            setLabel(R.string.input_field_label_repeat_delay)
            setInputFilter(MinMaxInputFilter(REPEAT_DELAY_MIN_MS.toInt(), REPEAT_DELAY_MAX_MS.toInt()))
            setOnTextChangedListener { editable -> onRepeatDelayChanged(editable.toString()) }
        }

        viewBinding.fieldFingerSelector.apply {
            setOnAddClickedListener { onFingerAddClicked() }
            setOnFingerClickedListener { index -> onFingerSelected(index) }
        }

        viewBinding.fieldStartTime.apply {
            setLabel(R.string.item_header_finger_start_at)
            setInputFilter(
                MinMaxInputFilter(min = 0, max = DumbAction.DumbGesture.GESTURE_MAXIMUM_DURATION_MS.toInt() - 1)
            )
            setOnTextChangedListener { editable -> onStartTimeChanged(editable.toString()) }
        }

        viewBinding.buttonCapture.setOnClickListener { onFingerCaptureClicked() }
        viewBinding.buttonDelete.setOnClickListener { onFingerDeleteClicked() }
    }

    override fun onBind(item: Header) {
        viewBinding.fieldName.apply {
            setText(item.actionName)
            setError(item.nameError)
        }

        viewBinding.fieldRepeat.apply {
            setNumericValue(item.repeatCount.toString())
            setChecked(item.isRepeatInfinite)
            setError(item.repeatCountError)
        }

        viewBinding.fieldRepeatDelay.apply {
            setText(item.repeatDelayMs.toString(), InputType.TYPE_CLASS_NUMBER)
            setError(item.repeatDelayError)
        }

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
    }

    fun updateFingerDesc(item: Header) {
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
