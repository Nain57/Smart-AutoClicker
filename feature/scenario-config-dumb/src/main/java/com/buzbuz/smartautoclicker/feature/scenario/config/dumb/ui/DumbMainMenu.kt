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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui

import android.view.LayoutInflater
import android.view.ViewGroup

import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.utils.AnimatedStatesImageButtonController
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.OverlayDumbMainMenuBinding

class DumbMainMenu(private val onStopClicked: () -> Unit) : OverlayMenu() {

    /** The view model for this menu. */
    private val viewModel: DumbMainMenuModel by viewModels()

    /** View binding for the content of the overlay. */
    private lateinit var viewBinding: OverlayDumbMainMenuBinding
    /** Controls the animations of the play/pause button. */
    private lateinit var playPauseButtonController: AnimatedStatesImageButtonController

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        playPauseButtonController = AnimatedStatesImageButtonController(
            context = context,
            state1StaticRes = R.drawable.ic_play_arrow,
            state2StaticRes = R.drawable.ic_pause,
            state1to2AnimationRes = R.drawable.anim_play_pause,
            state2to1AnimationRes = R.drawable.anim_pause_play,
        )

        viewBinding = OverlayDumbMainMenuBinding.inflate(layoutInflater)
        playPauseButtonController.attachView(viewBinding.btnPlay)

        return viewBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        playPauseButtonController.detachView()
    }
}