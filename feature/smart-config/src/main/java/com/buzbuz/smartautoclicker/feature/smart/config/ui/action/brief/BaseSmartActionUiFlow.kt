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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief

import android.content.Context
import com.buzbuz.smartautoclicker.core.common.overlays.base.BaseOverlay
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.starters.RequestNotificationPermissionActivity
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.BackButton
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.Swipe
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.OnActionConfigCompleteListener
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.changecounter.ChangeCounterDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click.ClickDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.copy.ActionCopyDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.IntentDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.notification.NotificationDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.pause.PauseDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection.ActionTypeChoice
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection.ActionTypeSelectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.swipe.SwipeDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent.ToggleEventDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.starters.newNotificationPermissionStarterOverlay


internal interface ActionConfigurator {
    fun getActionTypeChoices(): List<ActionTypeChoice>
    fun createAction(context: Context, choice: ActionTypeChoice): Action
    fun createActionFrom(action: Action): Action
    fun startActionEdition(action: Action)
    fun upsertEditedAction()
    fun removeEditedAction()
    fun dismissEditedAction()
}

internal fun BaseOverlay.showActionTypeSelectionDialog(configurator: ActionConfigurator) {
    overlayManager.navigateTo(
        context = context,
        newOverlay = ActionTypeSelectionDialog(
            choices = configurator.getActionTypeChoices(),
            onChoiceSelectedListener = { choiceClicked ->
                if (choiceClicked is ActionTypeChoice.Copy) {
                    showActionCopyDialog(configurator)
                    return@ActionTypeSelectionDialog
                }

                showActionConfigDialog(configurator, configurator.createAction(context, choiceClicked))
            },
        ),
    )
}

internal fun BaseOverlay.showActionCopyDialog(configurator: ActionConfigurator) {
    overlayManager.navigateTo(
        context = context,
        newOverlay = ActionCopyDialog(
            onActionSelected = { newCopyAction ->
                showActionConfigDialog(configurator, configurator.createActionFrom(newCopyAction))
            }
        ),
    )
}

internal fun BaseOverlay.showActionConfigDialog(configurator: ActionConfigurator, action: Action) {
    configurator.startActionEdition(action)

    val actionConfigDialogListener: OnActionConfigCompleteListener by lazy {
        object : OnActionConfigCompleteListener {
            override fun onConfirmClicked() { configurator.upsertEditedAction() }
            override fun onDeleteClicked() { configurator.removeEditedAction() }
            override fun onDismissClicked() { configurator.dismissEditedAction() }
        }
    }

    val overlay = when (action) {
        is Click -> ClickDialog(actionConfigDialogListener)
        is Swipe -> SwipeDialog(actionConfigDialogListener)
        is Pause -> PauseDialog(actionConfigDialogListener)
        is Intent -> IntentDialog(actionConfigDialogListener)
        is ToggleEvent -> ToggleEventDialog(actionConfigDialogListener)
        is ChangeCounter -> ChangeCounterDialog(actionConfigDialogListener)
        is Notification -> {
            if (PermissionPostNotification().checkIfGranted(context)) NotificationDialog(actionConfigDialogListener)
            else newNotificationPermissionStarterOverlay(context)
        }
        is BackButton -> {
            configurator.upsertEditedAction()
            return
        }
        else -> throw IllegalArgumentException("Unsupported action type")
    }


    overlayManager.navigateTo(
        context = context,
        newOverlay = overlay,
        hideCurrent = true,
    )
}
