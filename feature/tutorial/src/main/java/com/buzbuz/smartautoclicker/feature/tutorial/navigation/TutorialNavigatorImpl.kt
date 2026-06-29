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
package com.buzbuz.smartautoclicker.feature.tutorial.navigation

import android.content.Context
import android.content.Intent

import com.buzbuz.smartautoclicker.core.common.navigation.TutorialNavigator
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager.Companion.showAsOverlay
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tip
import com.buzbuz.smartautoclicker.feature.tutorial.ui.TutorialActivity
import com.buzbuz.smartautoclicker.feature.tutorial.ui.dialogs.createStopWithVolumeDownTutorialDialog

import javax.inject.Inject

internal class TutorialNavigatorImpl @Inject constructor(
    private val tutorialRepository: TutorialRepository,
) : TutorialNavigator {

    override fun startTutorialActivity(context: Context) {
        context.startActivity(Intent(context, TutorialActivity::class.java))
    }

    override fun showTipDialog(context: Context, tip: Tip, onDismissed: (() -> Unit)?) {
        when (tip) {
            Tip.STOP_WITH_VOLUME_DOWN -> context.createStopWithVolumeDownTutorialDialog(
                tutorialRepository = tutorialRepository,
                onDismissed = onDismissed,
            ).showAsOverlay()
        }
    }

}
