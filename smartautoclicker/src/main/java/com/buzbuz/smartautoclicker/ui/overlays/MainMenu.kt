/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.ui.overlays

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.overlays.OverlayMenuController

/**
 * [OverlayMenuController] implementation for displaying the main menu overlay.
 *
 * This is the menu displayed once the service is started via the [com.buzbuz.smartautoclicker.ui.activity.MainActivity]
 * once the user has selected a scenario to be used. It allows the user to start the detection on the currently loaded
 * scenario, as well as editing the attached list of clicks.
 *
 * There is no overlay views attached to this overlay menu, meaning that the user will always be able to clicks on the
 * Activities displayed below it.
 *
 * @param context the Android Context for the overlay menu shown by this controller.
 * @param clickListClickedListener listener called when the user clicks on the click list menu item.
 * @param playPauseClickedListener listener called when the user clicks on the play/pause menu item.
 * @param stopClickedListener listener called when the user clicks on the stop menu item.
 */
class MainMenu(
    context: Context,
    private val clickListClickedListener: () -> Unit,
    private val playPauseClickedListener: (Boolean) -> Unit,
    private val stopClickedListener: () -> Unit
) : OverlayMenuController(context) {

    /** Tells if the menu should tells if we are playing the scenario to detect or not. */
    private var isPlaying: Boolean = false

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        layoutInflater.inflate(R.layout.overlay_menu, null) as ViewGroup

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_play -> {
                changeDisplayMode(!isPlaying)
                playPauseClickedListener.invoke(isPlaying)
            }
            R.id.btn_click_list -> clickListClickedListener.invoke()
            R.id.btn_stop -> stopClickedListener.invoke()
        }
    }

    /**
     * Toggle the display mode between playing/paused.
     *
     * @param playing true to go to playing mode, false for paused mode.
     */
    private fun changeDisplayMode(playing: Boolean) {
        isPlaying = playing

        if (playing) {
            setMenuItemViewEnabled(R.id.btn_click_list, false)
            setMenuItemViewImageResource(R.id.btn_play, R.drawable.ic_pause)
        } else {
            setMenuItemViewEnabled(R.id.btn_click_list, true)
            setMenuItemViewImageResource(R.id.btn_play, R.drawable.ic_play_arrow)
        }
    }
}