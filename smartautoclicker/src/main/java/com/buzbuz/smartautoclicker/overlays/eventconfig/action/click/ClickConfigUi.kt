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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.click

import android.content.Context
import android.text.Editable
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayController
import com.buzbuz.smartautoclicker.databinding.IncludeClickConfigBinding
import com.buzbuz.smartautoclicker.extensions.setLeftRightCompoundDrawables
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.ClickSwipeSelectorMenu
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.ClickTargetChoice
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.CoordinatesSelector
import com.buzbuz.smartautoclicker.overlays.utils.DurationInputFilter
import com.buzbuz.smartautoclicker.overlays.utils.MultiChoiceDialog
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Binds the [IncludeClickConfigBinding] to the [ClickConfigModel] using the dialog lifecycle. */
fun IncludeClickConfigBinding.setupClickUi(
    context: Context,
    clickModel: ClickConfigModel,
    lifecycleOwner: LifecycleOwner,
    lifecycleScope: CoroutineScope,
    showSubOverlay: (OverlayController, Boolean) -> Unit
) {
    actionConfigLayoutClick.visibility = View.VISIBLE

    textClickPosition.setOnClickListener {
        showSubOverlay(
            MultiChoiceDialog(
                context = context,
                dialogTitle = R.string.dialog_condition_type_title,
                choices = listOf(ClickTargetChoice.OnCondition, ClickTargetChoice.AtPosition),
                onChoiceSelected = { choiceClicked ->
                    when (choiceClicked) {
                        ClickTargetChoice.OnCondition -> clickModel.setClickOnCondition(true)

                        ClickTargetChoice.AtPosition -> {
                            clickModel.setClickOnCondition(false)
                            showSubOverlay(
                                ClickSwipeSelectorMenu(
                                    context = context,
                                    selector = CoordinatesSelector.One(),
                                    onCoordinatesSelected = { selector ->
                                        clickModel.setPosition((selector as CoordinatesSelector.One).coordinates!!)
                                    },
                                ),
                                true
                            )
                        }
                    }
                }
            ),
            false,
        )
    }
    textClickPosition.setLeftRightCompoundDrawables(R.drawable.ic_click, R.drawable.ic_chevron)

    editPressDuration.apply {
        setSelectAllOnFocus(true)
        filters = arrayOf(DurationInputFilter())
        addTextChangedListener(object : OnAfterTextChangedListener() {
            override fun afterTextChanged(s: Editable?) {
                clickModel.setPressDuration(if (!s.isNullOrEmpty()) s.toString().toLong() else null)
            }
        })
    }

    lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                clickModel.pressDuration.collect { duration ->
                    editPressDuration.apply {
                        setText(duration.toString())
                        setSelection(text.length)
                    }
                }
            }

            launch {
                clickModel.position
                    .combine(clickModel.clickOnCondition) { position, clickOnCondition ->
                        textClickPosition.apply {
                            when {
                                clickOnCondition -> setText(R.string.dialog_action_config_click_position_on_condition)
                                position == null -> setText(R.string.dialog_action_config_click_position_none)
                                else -> {
                                    text = context.getString(
                                        R.string.dialog_action_config_click_position,
                                        position.x,
                                        position.y
                                    )
                                }
                            }
                        }
                    }.collect()
            }
        }
    }
}
