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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.intent

import android.content.Context
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayController
import com.buzbuz.smartautoclicker.databinding.IncludeIntentConfigBinding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Binds the [IncludeIntentConfigBinding] to the [IntentConfigModel] using the dialog lifecycle. */
fun IncludeIntentConfigBinding.setupIntentUi(
    context: Context,
    intentModel: IntentConfigModel,
    lifecycleOwner: LifecycleOwner,
    lifecycleScope: CoroutineScope,
    showSubOverlay: (OverlayController, Boolean) -> Unit,
) {
    actionConfigLayoutIntent.visibility = View.VISIBLE

    textConfigType.setOnClickListener {
        intentModel.toggleIsAdvanced()
    }

    lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                intentModel.isAdvanced.collect { isAdvanced ->
                    setConfigModeVisibility(isAdvanced)
                }
            }

            setupSimpleUi(context, this, intentModel, showSubOverlay)
        }
    }
}

/**
 * Change the visibility/content of the views according to the edition mode.
 * @param isAdvanced if true, edition mode is advanced. If false, edition mode is simple.
 *
 */
private fun IncludeIntentConfigBinding.setConfigModeVisibility(isAdvanced: Boolean) {
    if (isAdvanced) {
        layoutSimple.visibility = View.GONE
        textConfigType.setText(R.string.dialog_action_config_intent_config_type_advanced)
    } else {
        layoutSimple.visibility = View.VISIBLE
        textConfigType.setText(R.string.dialog_action_config_intent_config_type_simple)
    }
}

/**
 * Setup the ui for the simple edition mode.
 * Only the application to start selection is displayed.
 */
private fun IncludeIntentConfigBinding.setupSimpleUi(
    context: Context,
    coroutineScope: CoroutineScope,
    intentModel: IntentConfigModel,
    showSubOverlay: (OverlayController, Boolean) -> Unit,
) {
    textApplicationToStart.setOnClickListener {
        showSubOverlay(
            ActivitySelectionDialog(
                context = context,
                onApplicationSelected = { componentName ->
                    intentModel.setActivitySelected(componentName)
                }
            ),
            false,
        )
    }

    coroutineScope.launch {
        intentModel.activityInfo.collect { applicationInfo ->
            if (applicationInfo == null) {
                iconApplicationToStart.visibility = View.GONE
                textApplicationToStart.setText(R.string.dialog_action_config_intent_simple_start_app_no_app)
            } else {
                textApplicationToStart.text = applicationInfo.name
                iconApplicationToStart.apply {
                    visibility = View.VISIBLE
                    setImageDrawable(applicationInfo.icon)
                }
            }
        }
    }
}