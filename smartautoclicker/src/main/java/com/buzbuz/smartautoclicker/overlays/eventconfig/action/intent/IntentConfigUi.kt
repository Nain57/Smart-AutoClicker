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
import android.text.Editable
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayController
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.IntentExtra
import com.buzbuz.smartautoclicker.databinding.IncludeIntentConfigBinding
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

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
    textIsBroadcast.setOnClickListener {
        intentModel.toggleIsBroadcast()
    }

    editIntentAction.apply {
        setSelectAllOnFocus(true)
        addTextChangedListener(object : OnAfterTextChangedListener() {
            override fun afterTextChanged(s: Editable?) {
                intentModel.setIntentAction(s.toString())
            }
        })
    }
    editIntentFlags.apply {
        setSelectAllOnFocus(true)
        addTextChangedListener(object : OnAfterTextChangedListener() {
            override fun afterTextChanged(s: Editable?) {
                val flags = try {
                    if (!s.isNullOrEmpty()) s.toString().toInt() else null
                } catch (nfe: NumberFormatException) { null }
                intentModel.setFlagsAction(flags)
            }
        })
    }
    editIntentComponentName.apply {
        setSelectAllOnFocus(true)
        addTextChangedListener(object : OnAfterTextChangedListener() {
            override fun afterTextChanged(s: Editable?) {
                intentModel.setComponentName(s.toString())
            }
        })
    }

    listExtras.adapter = ExtrasAdapter(
        addExtraClickedListener = {
            showSubOverlay(createExtraDialog(context, intentModel, intentModel.getNewExtra()), false)
        },
        extraClickedListener = { extra, index ->
            showSubOverlay(createExtraDialog(context, intentModel, extra, index), false)
        }
    )

    lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                intentModel.isAdvanced.collect { isAdvanced ->
                    if (isAdvanced) {
                        refreshAdvancedUi(intentModel)
                    } else {
                        refreshSimpleUi()
                    }
                }
            }

            launch {
                intentModel.isBroadcast.collect { isBroadcast ->
                    textIsBroadcast.setText(
                        if (isBroadcast) R.string.dialog_action_config_intent_advanced_send_type_broadcast
                        else R.string.dialog_action_config_intent_advanced_send_type_start_app
                    )
                }
            }

            launch {
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

            launch {
                intentModel.extras.collect { extras ->
                    (listExtras.adapter as ExtrasAdapter).submitList(extras)
                }
            }

            launch {
                intentModel.action.collect {
                    editIntentAction.setText(it)
                }
            }

            launch {
                intentModel.flags.collect {
                    editIntentFlags.setText(it)
                }
            }

            launch {
                intentModel.componentName.collect {
                    editIntentComponentName.setText(it)
                }
            }
        }
    }
}

/**
 * Creates the extra configuration dialog.
 * @param context the Android context.
 * @param intentModel the model for the configured action intent.
 * @param extra the extra to be configured.
 * @param index the index of the configured extra in the list. -1 for a new extra.
 */
private fun createExtraDialog(
    context: Context,
    intentModel: IntentConfigModel,
    extra: IntentExtra<out Any>,
    index: Int = -1,
) = ExtraConfigDialog(
    context = context,
    extra = extra,
    onConfigComplete = { configuredExtra ->
        if (index == -1) intentModel.addNewExtra(configuredExtra)
        else intentModel.updateExtra(configuredExtra, index)
    },
    onDeleteClicked = if (index != -1) { { intentModel.deleteExtra(index) } } else null
)

/**
 * Setup the ui for the simple edition mode.
 * Only the application to start selection is displayed.
 */
private fun IncludeIntentConfigBinding.refreshSimpleUi() {
    layoutSimple.visibility = View.VISIBLE
    layoutAdvanced.visibility = View.GONE
    textConfigType.setText(R.string.dialog_action_config_intent_config_type_simple)
}

/** Setup the ui for the advanced edition mode. */
private fun IncludeIntentConfigBinding.refreshAdvancedUi(intentModel: IntentConfigModel) {
    layoutSimple.visibility = View.GONE
    layoutAdvanced.visibility = View.VISIBLE
    textConfigType.setText(R.string.dialog_action_config_intent_config_type_advanced)

    val action = intentModel.configuredAction.value
    if (action is Action.Intent) {
        editIntentAction.setText(action.intentAction)
        editIntentFlags.setText(action.flags.toString())
        editIntentComponentName.setText(action.componentName?.flattenToString())
    }
}