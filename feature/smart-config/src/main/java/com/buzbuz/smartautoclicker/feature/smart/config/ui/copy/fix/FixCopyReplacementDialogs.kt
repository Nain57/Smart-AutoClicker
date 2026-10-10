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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.toggleevent.EventTogglesDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.selection.ScreenConditionSelectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.selection.CounterSelectionDialog


internal fun OverlayDialog.showReplaceScreenConditionDialog(
    candidates: List<ScreenCondition>,
    onScreenConditionSelected: (ScreenCondition) -> Unit,
) {
    overlayManager.navigateTo(
        context = context,
        hideCurrent = true,
        newOverlay = ScreenConditionSelectionDialog(
            conditionList = candidates,
            onConditionSelected = onScreenConditionSelected,
        ),
    )
}

/**
 * [EventTogglesDialog] requires an edited action in the edition repository to create the toggles.
 * Callers must start it before calling this method, and stop it in both callbacks.
 */
internal fun OverlayDialog.showReplaceEventToggleDialog(
    scenarioEvents: List<Event>,
    action: ToggleEvent,
    onEventTogglesSelected: (List<EventToggle>) -> Unit,
    onDismissed: () -> Unit,
) {
    overlayManager.navigateTo(
        context = context,
        hideCurrent = true,
        newOverlay = EventTogglesDialog(
            scenarioEvents = scenarioEvents,
            toggleEventAction = action,
            onConfirmClicked = onEventTogglesSelected,
            onDismissed = onDismissed,
        ),
    )
}

internal fun OverlayDialog.showReplaceCounterDialog(onCounterSelected: (String) -> Unit) {
    overlayManager.navigateTo(
        context = context,
        hideCurrent = true,
        newOverlay = CounterSelectionDialog(
            onCounterSelected = onCounterSelected,
        ),
    )
}
