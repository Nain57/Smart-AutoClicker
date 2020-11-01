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
package com.buzbuz.smartautoclicker.overlays

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.overlays.OverlayMenuController
import com.buzbuz.smartautoclicker.database.ClickInfo
import com.buzbuz.smartautoclicker.dialogs.ClickListDialog
import com.buzbuz.smartautoclicker.model.DetectorModel

/**
 * [OverlayMenuController] implementation for displaying the main menu overlay.
 *
 * This is the menu displayed once the service is started via the [com.buzbuz.smartautoclicker.activity.ScenarioActivity]
 * once the user has selected a scenario to be used. It allows the user to start the detection on the currently loaded
 * scenario, as well as editing the attached list of clicks.
 *
 * There is no overlay views attached to this overlay menu, meaning that the user will always be able to clicks on the
 * Activities displayed below it.
 *
 * @param context the Android Context for the overlay menu shown by this controller.
 * @param detectionListener listener notified upon click detection.
 */
class MainMenu(context: Context, private val detectionListener: (ClickInfo) -> Unit) : OverlayMenuController(context) {

    /** Animation from play to pause. */
    private val playToPauseDrawable =
        AnimatedVectorDrawableCompat.create(context, R.drawable.anim_play_pause)!!
    /** Animation from pause to play. */
    private val pauseToPlayDrawable =
        AnimatedVectorDrawableCompat.create(context, R.drawable.anim_pause_play)!!

    /** Tells if the detecting state have never been updated. Use to skip animation the first time. */
    private var isFirstStateUpdate = true

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        layoutInflater.inflate(R.layout.overlay_menu, null) as ViewGroup

    override fun onCreate() {
        super.onCreate()
        DetectorModel.get().apply {
            scenarioClicks.observe(this@MainMenu, ::onClickListChanged)
            detecting.observe(this@MainMenu, ::onDetectionStateChanged)
        }
    }

    override fun onDismissed() {
        super.onDismissed()
        DetectorModel.get().apply {
            scenarioClicks.removeObservers(this@MainMenu)
            detecting.removeObservers(this@MainMenu)
        }
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_play -> {
                DetectorModel.get().apply {
                    if (detecting.value!!) stopDetection() else startDetection(detectionListener)
                }
            }
            R.id.btn_click_list -> showSubOverlay(ClickListDialog(ContextThemeWrapper(context, R.style.AppTheme)), true)
            R.id.btn_stop -> dismiss()
        }
    }

    /**
     * Handles changes on the click list.
     * Refresh the play menu item according to the click count.
     */
    private fun onClickListChanged(clicks: List<ClickInfo>?) =
        setMenuItemViewEnabled(R.id.btn_play, !clicks.isNullOrEmpty())

    /**
     * Handles the changes in the detection state.
     * Animate the detection icon according to the new state if that's not the first start.
     *
     * @param enabled true if we are detecting, false if not.
     */
    private fun onDetectionStateChanged(enabled: Boolean) {
        if (enabled) {
            setMenuItemViewEnabled(R.id.btn_click_list, false)
            if (isFirstStateUpdate) {
                setMenuItemViewImageResource(R.id.btn_play, R.drawable.ic_pause)
                isFirstStateUpdate = false
            } else {
                setMenuItemViewDrawable(R.id.btn_play, playToPauseDrawable)
                playToPauseDrawable.start()
            }
        } else {
            setMenuItemViewEnabled(R.id.btn_click_list, true)
            if (isFirstStateUpdate) {
                setMenuItemViewImageResource(R.id.btn_play, R.drawable.ic_play_arrow)
                isFirstStateUpdate = false
            } else {
                setMenuItemViewDrawable(R.id.btn_play, pauseToPlayDrawable)
                pauseToPlayDrawable.start()
            }
        }
    }
}