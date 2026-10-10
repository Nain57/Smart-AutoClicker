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
package com.buzbuz.smartautoclicker.feature.smart.debugging.navigation

import android.content.Context
import com.buzbuz.smartautoclicker.core.common.navigation.DebuggingNavigator
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.conditiontry.TryImageConditionOverlayMenu
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.live.eventtry.TryEventOverlayMenu
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.DebugReportDialog
import javax.inject.Inject


internal class DebuggingNavigatorImpl @Inject constructor(
    private val overlayManager: OverlayManager,
) : DebuggingNavigator {

    override fun showDebugReportDialog(context: Context) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = DebugReportDialog(),
        )
    }

    override fun showTryEventOverlayMenu(context: Context, scenario: Scenario, triedElement: ScreenEvent) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = TryEventOverlayMenu(scenario, triedElement),
            hideCurrent = true,
        )
    }

    override fun showTryConditionOverlayMenu(
        context: Context,
        scenario: Scenario,
        imageCondition: ScreenCondition,
        onNewThresholdSelected: (Int) -> Unit,
    ) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = TryImageConditionOverlayMenu(
                scenario = scenario,
                imageCondition = imageCondition,
                onNewThresholdSelected = onNewThresholdSelected,
            ),
            hideCurrent = true,
        )
    }

}