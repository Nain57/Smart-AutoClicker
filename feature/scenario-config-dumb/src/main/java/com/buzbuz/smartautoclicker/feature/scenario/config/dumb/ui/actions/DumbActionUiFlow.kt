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

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.MultiChoiceDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.ClickSwipeSelectorMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.CoordinatesSelector
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.click.DumbClickDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.copy.DumbActionCopyDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.pause.DumbPauseDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.swipe.DumbSwipeDialog

internal fun OverlayManager.startDumbActionCreationUiFlow(
    context: Context,
    creator: DumbActionCreator,
    listener: DumbActionUiFlowListener,
): Unit = navigateTo(
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

internal fun OverlayManager.startDumbActionCopyUiFlow(
    context: Context,
    creator: DumbActionCreator,
    listener: DumbActionUiFlowListener,
): Unit = navigateTo(
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

internal fun OverlayManager.startDumbActionEditionUiFlow(
    context: Context,
    dumbAction: DumbAction,
    listener: DumbActionUiFlowListener,
) {
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
    if (!dumbClick.isValid()) return

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
): Unit = navigateTo(
    context = context,
    newOverlay = ClickSwipeSelectorMenu(
        selector = CoordinatesSelector.One(),
        onCoordinatesSelected = { selector ->
            (selector as? CoordinatesSelector.One)?.coordinates?.let { position ->
                startDumbClickEditionUiFlow(
                    context = context,
                    dumbClick = creator.createNewDumbClick(position),
                    listener = listener,
                )
            }
        },
        onDismiss = listener.onDumbActionCreationCancelled,
    ),
    hideCurrent = true,
)

private fun OverlayManager.startDumbSwipeEditionFlow(
    context: Context,
    dumbSwipe: DumbAction.DumbSwipe,
    listener: DumbActionUiFlowListener,
) {
    if (!dumbSwipe.isValid()) return

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

private fun OverlayManager.startDumbPauseEditionFlow(
    context: Context,
    dumbPause: DumbAction.DumbPause,
    listener: DumbActionUiFlowListener,
): Unit = navigateTo(
    context = context,
    newOverlay = DumbPauseDialog(
        dumbPause = dumbPause,
        onConfirmClicked = listener.onDumbActionSaved,
        onDeleteClicked = listener.onDumbActionDeleted,
        onDismissClicked = listener.onDumbActionCreationCancelled,
    ),
    hideCurrent = true,
)

private fun OverlayManager.onDumbSwipeCreationSelected(
    context: Context,
    creator: DumbActionCreator,
    listener: DumbActionUiFlowListener,
): Unit = navigateTo(
    context = context,
    newOverlay = ClickSwipeSelectorMenu(
        selector = CoordinatesSelector.Two(),
        onCoordinatesSelected = { selector ->
            (selector as? CoordinatesSelector.Two)?.let { two ->
                if (two.coordinates1 == null || two.coordinates2 == null) return@let
                startDumbSwipeEditionFlow(
                    context = context,
                    dumbSwipe = creator.createNewDumbSwipe(two.coordinates1!!, two.coordinates2!!),
                    listener = listener,
                )
            }
        },
        onDismiss = listener.onDumbActionCreationCancelled,
    ),
    hideCurrent = true,
)



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
