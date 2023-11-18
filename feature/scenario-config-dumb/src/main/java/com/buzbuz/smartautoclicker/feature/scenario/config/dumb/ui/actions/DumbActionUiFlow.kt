/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions

import android.content.Context
import android.graphics.Point
import android.util.Log

import androidx.core.graphics.toPoint

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.MultiChoiceDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.PositionSelectorMenu
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ClickDescription
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.SwipeDescription
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.click.DumbClickDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.copy.DumbActionCopyDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.pause.DumbPauseDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.swipe.DumbSwipeDialog

internal fun OverlayManager.startDumbActionCreationUiFlow(
    context: Context,
    creator: DumbActionCreator,
    listener: DumbActionUiFlowListener,
) {
    Log.d(TAG, "Starting dumb action creation ui flow")

    navigateTo(
        context = context,
        newOverlay = MultiChoiceDialog(
            theme = R.style.AppTheme,
            dialogTitleText = R.string.dialog_overlay_title_dumb_action_type,
            choices = allDumbActionChoices(),
            onChoiceSelected = { choice ->
                when (choice) {
                    DumbActionTypeChoice.Click -> onDumbClickCreationSelected(context, creator, listener)
                    DumbActionTypeChoice.Swipe -> onDumbSwipeCreationSelected(context, creator, listener)
                    DumbActionTypeChoice.Pause -> startDumbPauseEditionFlow(context, creator.createNewDumbPause(), listener)
                }
            },
            onCanceled = listener.onDumbActionCreationCancelled,
        )
    )
}

internal fun OverlayManager.startDumbActionCopyUiFlow(
    context: Context,
    creator: DumbActionCreator,
    listener: DumbActionUiFlowListener,
) {
    Log.d(TAG, "Starting dumb action copy ui flow")

    navigateTo(
        context = context,
        newOverlay = DumbActionCopyDialog(
            onActionSelected = { actionToCopy ->
                creator.createDumbActionCopy?.invoke(actionToCopy)?.let { copiedAction ->
                    startDumbActionEditionUiFlow(
                        context = context,
                        dumbAction = copiedAction,
                        listener = listener
                    )
                }
            }
        )
    )
}

internal fun OverlayManager.startDumbActionEditionUiFlow(
    context: Context,
    dumbAction: DumbAction,
    listener: DumbActionUiFlowListener,
) {
    Log.d(TAG, "Starting dumb action edition ui flow")

    when (dumbAction) {
        is DumbAction.DumbClick -> startDumbClickEditionUiFlow(context, dumbAction, listener)
        is DumbAction.DumbSwipe -> startDumbSwipeEditionFlow(context, dumbAction, listener)
        is DumbAction.DumbPause -> startDumbPauseEditionFlow(context, dumbAction, listener)
    }
}

private fun OverlayManager.startDumbClickEditionUiFlow(
    context: Context,
    dumbClick: DumbAction.DumbClick,
    listener: DumbActionUiFlowListener,
) {
    if (!dumbClick.isValid()) {
        Log.e(TAG, "Can't start dumb click edition ui flow, click is invalid: $dumbClick")
        listener.onDumbActionCreationCancelled()
        return
    }
    Log.d(TAG, "Starting dumb click edition ui flow: $dumbClick")

    navigateTo(
        context = context,
        newOverlay = DumbClickDialog(
            dumbClick = dumbClick,
            onConfirmClicked = listener.onDumbActionSaved,
            onDeleteClicked = listener.onDumbActionDeleted,
            onDismissClicked = listener.onDumbActionCreationCancelled,
        ),
        hideCurrent = true,
    )
}

private fun OverlayManager.onDumbClickCreationSelected(
    context: Context,
    creator: DumbActionCreator,
    listener: DumbActionUiFlowListener,
) {
    Log.d(TAG, "Dumb click creation selected, opening position selection menu")

    navigateTo(
        context = context,
        newOverlay = PositionSelectorMenu(
            actionDescription = ClickDescription(),
            onConfirm = { description ->
                (description as? ClickDescription)?.position?.let { position ->
                    startDumbClickEditionUiFlow(
                        context = context,
                        dumbClick = creator.createNewDumbClick(position.toPoint()),
                        listener = listener,
                    )
                } ?: listener.onDumbActionCreationCancelled()
            },
            onDismiss = listener.onDumbActionCreationCancelled,
        ),
        hideCurrent = true,
    )
}

private fun OverlayManager.startDumbSwipeEditionFlow(
    context: Context,
    dumbSwipe: DumbAction.DumbSwipe,
    listener: DumbActionUiFlowListener,
) {
    if (!dumbSwipe.isValid()) {
        Log.e(TAG, "Can't start dumb swipe edition ui flow, swipe is invalid: $dumbSwipe")
        listener.onDumbActionCreationCancelled()
        return
    }
    Log.d(TAG, "Starting dumb swipe edition ui flow: $dumbSwipe")

    navigateTo(
        context = context,
        newOverlay = DumbSwipeDialog(
            dumbSwipe = dumbSwipe,
            onConfirmClicked = listener.onDumbActionSaved,
            onDeleteClicked = listener.onDumbActionDeleted,
            onDismissClicked = listener.onDumbActionCreationCancelled,
        ),
        hideCurrent = true,
    )
}

private fun OverlayManager.onDumbSwipeCreationSelected(
    context: Context,
    creator: DumbActionCreator,
    listener: DumbActionUiFlowListener,
) {
    Log.d(TAG, "Dumb swipe creation selected, opening position selection menu")

    navigateTo(
        context = context,
        newOverlay = PositionSelectorMenu(
            actionDescription = SwipeDescription(),
            onConfirm = { description ->
                (description as? SwipeDescription)?.let { swipeDesc ->
                    if (swipeDesc.from == null || swipeDesc.to == null) {
                        listener.onDumbActionCreationCancelled()
                        return@let
                    }

                    startDumbSwipeEditionFlow(
                        context = context,
                        dumbSwipe = creator.createNewDumbSwipe(
                            swipeDesc.from?.toPoint()!!,
                            swipeDesc.to?.toPoint()!!,
                        ),
                        listener = listener,
                    )
                }
            },
            onDismiss = listener.onDumbActionCreationCancelled,
        ),
        hideCurrent = true,
    )
}

private fun OverlayManager.startDumbPauseEditionFlow(
    context: Context,
    dumbPause: DumbAction.DumbPause,
    listener: DumbActionUiFlowListener,
) {
    Log.d(TAG, "Dumb pause creation selected: $dumbPause")

    navigateTo(
        context = context,
        newOverlay = DumbPauseDialog(
            dumbPause = dumbPause,
            onConfirmClicked = listener.onDumbActionSaved,
            onDeleteClicked = listener.onDumbActionDeleted,
            onDismissClicked = listener.onDumbActionCreationCancelled,
        ),
        hideCurrent = true,
    )
}



internal class DumbActionUiFlowListener(
    val onDumbActionSaved: (dumbAction: DumbAction) -> Unit,
    val onDumbActionDeleted: (dumbAction: DumbAction) -> Unit,
    val onDumbActionCreationCancelled: () -> Unit,
)

internal class DumbActionCreator(
    val createNewDumbClick: (position: Point) -> DumbAction.DumbClick,
    val createNewDumbSwipe: (from: Point, to: Point) -> DumbAction.DumbSwipe,
    val createNewDumbPause: () -> DumbAction.DumbPause,
    val createDumbActionCopy: ((DumbAction) -> DumbAction)? = null,
)


private const val TAG = "DumbActionUiFlow"