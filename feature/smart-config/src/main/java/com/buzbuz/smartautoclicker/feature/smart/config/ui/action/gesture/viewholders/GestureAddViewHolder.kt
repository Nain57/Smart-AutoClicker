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

import android.os.Build
import android.view.View

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemGestureAddBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.GestureDialogUiStateItem

class GestureAddViewHolder(
    viewBinding: ItemGestureAddBinding,
    onAddMoveClicked: () -> Unit,
    onAddWaitClicked: () -> Unit,
) : BaseGestureViewHolder<GestureDialogUiStateItem.Add>(viewBinding) {

    init {
        viewBinding.apply {
            buttonAddMove.setOnClickListener { onAddMoveClicked() }

            // Holding a finger still requires stroke continuation, only available from Android O.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                buttonAddWait.setOnClickListener { onAddWaitClicked() }
            } else {
                buttonAddWait.visibility = View.GONE
            }
        }
    }
}

