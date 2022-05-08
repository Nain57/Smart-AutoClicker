/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.swipe

import android.content.Context
import android.text.Editable
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayController
import com.buzbuz.smartautoclicker.databinding.IncludeSwipeConfigBinding
import com.buzbuz.smartautoclicker.extensions.setLeftRightCompoundDrawables
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.ClickSwipeSelectorMenu
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.CoordinatesSelector
import com.buzbuz.smartautoclicker.overlays.utils.DurationInputFilter
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Binds the [IncludeSwipeConfigBinding] to the [SwipeConfigModel] using the dialog lifecycle. */
fun IncludeSwipeConfigBinding.setupSwipeUi(
    context: Context,
    swipeModel: SwipeConfigModel,
    lifecycleOwner: LifecycleOwner,
    lifecycleScope: CoroutineScope,
    showSubOverlay: (OverlayController, Boolean) -> Unit
) {
    actionConfigLayoutSwipe.visibility = View.VISIBLE

    textSwipePosition.setOnClickListener {
        showSubOverlay(
            ClickSwipeSelectorMenu(
                context = context,
                selector = CoordinatesSelector.Two(),
                onCoordinatesSelected = { selector ->
                    (selector as CoordinatesSelector.Two).let {
                        swipeModel.setPositions(it.coordinates1!!, it.coordinates2!!)
                    }
                },
            ),
            true
        )
    }
    textSwipePosition.setLeftRightCompoundDrawables(R.drawable.ic_swipe, R.drawable.ic_chevron)

    editSwipeDuration.apply {
        setSelectAllOnFocus(true)
        filters = arrayOf(DurationInputFilter())
        addTextChangedListener(object : OnAfterTextChangedListener() {
            override fun afterTextChanged(s: Editable?) {
                swipeModel.setSwipeDuration(if (!s.isNullOrEmpty()) s.toString().toLong() else null)
            }
        })
    }

    lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                swipeModel.swipeDuration.collect { duration ->
                    editSwipeDuration.apply {
                        setText(duration.toString())
                        setSelection(text.length)
                    }
                }
            }

            launch {
                swipeModel.positions.collect { (from, to) ->
                    textSwipePosition.apply {
                        if (from == null || to == null) {
                            setText(R.string.dialog_action_config_swipe_position_none)
                        } else {
                            text = context.getString(
                                R.string.dialog_action_config_swipe_position,
                                from.x,
                                from.y,
                                to.x,
                                to.y
                            )
                        }
                    }
                }
            }
        }
    }
}