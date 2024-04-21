/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.bindings

import android.util.TypedValue
import android.view.View

import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.ItemDumbActionBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.actionlist.DumbActionDetails

fun ItemDumbActionBinding.onBind(
    details: DumbActionDetails,
    showHandles: Boolean,
    actionClickedListener: (DumbActionDetails) -> Unit,
) {
    root.setOnClickListener { actionClickedListener(details) }

    btnReorder.visibility = if (showHandles) View.VISIBLE else View.GONE
    actionName.visibility = View.VISIBLE
    actionTypeIcon.setImageResource(details.icon)
    actionName.text = details.name
    actionDuration.apply {
        text = details.detailsText

        val typedValue = TypedValue()
        val actionColorAttr = if (details.haveError) R.attr.colorError else R.attr.colorOnSurfaceVariant
        root.context.theme.resolveAttribute(actionColorAttr, typedValue, true)
        setTextColor(typedValue.data)
    }

    actionRepeat.apply {
        if (details.repeatCountText != null) {
            text = details.repeatCountText
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }
}