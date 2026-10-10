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

import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Release.Hold
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setInputFilter
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGestureReleaseBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.GestureDialogUiStateItem.Release
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.ReleaseWhenDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.releaseWhenDropdownItems
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.getGestureUiStateItemDescription

import kotlinx.coroutines.Job


class GestureReleaseViewHolder (
    private val viewBinding: ItemGestureReleaseBinding,
    onCollapseClicked: (item: Release) -> Unit,
    onWhenItemSelected: (item: Release, whenItem: ReleaseWhenDropdownItem) -> Unit,
    onHoldDurationChanged: (item: Release, value: String) -> Unit,
) : BaseGestureViewHolder<Release>(viewBinding) {

    init {
        viewBinding.root.setOnClickListener {
            boundItem?.let { if (it.isWhenFieldVisible && it.isCollapsed) onCollapseClicked(it) }
        }
        viewBinding.buttonExpandCollapse.setOnClickListener { boundItem?.let { onCollapseClicked(it) } }
        viewBinding.fieldWhen.setItems(
            label = viewBinding.root.context.getString(R.string.field_gesture_release_when_title),
            items = releaseWhenDropdownItems,
            onItemSelected = { whenItem -> boundItem?.let { onWhenItemSelected(it, whenItem) } },
        )
        viewBinding.fieldHoldDuration.apply {
            setLabel(R.string.field_gesture_release_hold_duration_title)
            setInputFilter(MinMaxInputFilter(min = 1, max = Hold.ForDuration.MAXIMUM_DURATION_MS.toInt()))
            setOnTextChangedListener { editable ->
                boundItem?.let { onHoldDurationChanged(it, editable.toString()) }
            }
        }
    }

    override fun onBind(item: Release): Job? {
        viewBinding.apply {
            val isExpanded = item.isWhenFieldVisible && !item.isCollapsed
            val isHoldDurationVisible = isExpanded && item.holdDurationMs != null

            root.isClickable = item.isWhenFieldVisible && item.isCollapsed
            buttonExpandCollapse.isVisible = item.isWhenFieldVisible
            dividerTop.isVisible = isExpanded
            fieldWhen.root.isVisible = isExpanded
            fieldHoldDuration.root.isVisible = isHoldDurationVisible
            cardHoldInfo.isVisible = isExpanded
            motionDesc.isVisible = !isExpanded
            if (!isExpanded) motionDesc.text = root.context.getGestureUiStateItemDescription(item)

            buttonExpandCollapse.setIconResource(if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down)
            if (isExpanded) {
                fieldWhen.setSelectedItem(item.whenItem)
                textReleaseDesc.text = item.getReleaseDescription()
            }
            if (isHoldDurationVisible) {
                fieldHoldDuration.setText(item.holdDurationMs.toString(), InputType.TYPE_CLASS_NUMBER)
                fieldHoldDuration.setError(item.isHoldDurationInError)
            }
        }

        return null
    }

    private fun Release.getReleaseDescription(): String {
        val context = viewBinding.root.context
        return when (whenItem) {
            ReleaseWhenDropdownItem.Now -> context.getString(R.string.field_gesture_release_desc_now)
            ReleaseWhenDropdownItem.UntilActionsEnd ->
                context.getString(R.string.field_gesture_release_desc_until_actions_end)
            ReleaseWhenDropdownItem.OnceUnfulfilled ->
                context.getString(R.string.field_gesture_release_desc_once_unfulfilled)
            ReleaseWhenDropdownItem.ForDuration ->
                context.getString(R.string.field_gesture_release_desc_for_duration, holdDurationMs ?: 0L)
        }
    }
}
