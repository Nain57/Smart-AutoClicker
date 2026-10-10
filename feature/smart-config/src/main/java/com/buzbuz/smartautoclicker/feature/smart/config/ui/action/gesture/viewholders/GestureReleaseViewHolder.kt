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

import androidx.core.view.isVisible

import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGestureReleaseBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.GestureDialogUiStateItem.Release
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.ReleaseWhenDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.releaseWhenDropdownItems

import kotlinx.coroutines.Job


class GestureReleaseViewHolder (
    private val viewBinding: ItemGestureReleaseBinding,
    onCollapseClicked: (item: Release) -> Unit,
    onWhenItemSelected: (item: Release, whenItem: ReleaseWhenDropdownItem) -> Unit,
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
    }

    override fun onBind(item: Release): Job? {
        viewBinding.apply {
            val isExpanded = item.isWhenFieldVisible && !item.isCollapsed

            root.isClickable = item.isWhenFieldVisible && item.isCollapsed
            buttonExpandCollapse.isVisible = item.isWhenFieldVisible
            dividerTop.isVisible = isExpanded
            fieldWhen.root.isVisible = isExpanded

            buttonExpandCollapse.setIconResource(if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down)
            if (isExpanded) fieldWhen.setSelectedItem(item.whenItem)
        }

        return null
    }
}
