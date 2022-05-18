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
package com.buzbuz.smartautoclicker.overlays.mainmenu

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.view.WindowManager
import android.widget.TextView

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.menu.OverlayMenuController
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.overlays.eventlist.EventListDialog

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * [OverlayMenuController] implementation for displaying the main menu overlay.
 *
 * This is the menu displayed once the service is started via the [com.buzbuz.smartautoclicker.activity.ScenarioActivity]
 * once the user has selected a scenario to be used. It allows the user to start the detection on the currently loaded
 * scenario, as well as editing the attached list of events.
 *
 * There is no overlay views attached to this overlay menu, meaning that the user will always be able to clicks on the
 * Activities displayed below it.
 *
 * @param context the Android Context for the overlay menu shown by this controller.
 */
class MainMenu(context: Context, private val scenario: Scenario) : OverlayMenuController(context) {

    /** The view model for this menu. */
    private var viewModel: MainMenuModel? = MainMenuModel(context).apply {
        attachToLifecycle(this@MainMenu)
    }

    /** Animation from play to pause. */
    private val playToPauseDrawable =
        AnimatedVectorDrawableCompat.create(context, R.drawable.anim_play_pause)!!
    /** Animation from pause to play. */
    private val pauseToPlayDrawable =
        AnimatedVectorDrawableCompat.create(context, R.drawable.anim_pause_play)!!

    /** Tells if the detecting state have never been updated. Use to skip animation the first time. */
    private var isFirstStateUpdate = true
    /** The coroutine job for the observable used in debug mode. Null when not in debug mode. */
    private var debugObservableJob: Job? = null

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        layoutInflater.inflate(R.layout.overlay_menu, null) as ViewGroup

    override fun onCreateOverlayView(): DebugView = DebugView(context)

    override fun onCreateOverlayViewLayoutParams(): WindowManager.LayoutParams =
        super.onCreateOverlayViewLayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }

    override fun onCreate() {
        super.onCreate()

        // Ensure the debug view state is correct
        getMenuItemView<View>(R.id.layout_debug)?.visibility = View.GONE
        setOverlayViewVisibility(View.GONE)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel?.eventList?.collect { onEventListChanged(it) } }
                launch { viewModel?.detectionState?.collect { onDetectionStateChanged(it) } }

                launch {
                    viewModel?.isDebugging?.collect { isDebugging ->
                        changeDebugState(isDebugging)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        getMenuItemView<View>(R.id.btn_play)?.setOnLongClickListener {
            viewModel?.toggleDetection(true)
            true
        }
    }

    override fun onDismissed() {
        super.onDismissed()
        viewModel = null
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_play -> viewModel?.toggleDetection()
            R.id.btn_click_list -> showSubOverlay(
                EventListDialog(ContextThemeWrapper(context, R.style.AppTheme), scenario), true)
            R.id.btn_stop -> dismiss()
        }
    }

    /**
     * Handles changes on the event list.
     * Refresh the play menu item according to the event count.
     */
    private fun onEventListChanged(events: List<Event>?) =
        setMenuItemViewEnabled(R.id.btn_play, !events.isNullOrEmpty())

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

    /**
     * Change the debug state of this UI.
     * @param isDebugging true is the debug mode is active, false if not.
     */
    private fun changeDebugState(isDebugging: Boolean) {
        if (isDebugging && debugObservableJob == null) {
            getMenuItemView<View>(R.id.layout_debug)?.visibility = View.VISIBLE
            setOverlayViewVisibility(View.VISIBLE)
            debugObservableJob = observeDebugValues()

        } else if (!isDebugging && debugObservableJob != null) {
            debugObservableJob?.cancel()
            debugObservableJob = null

            getMenuItemView<View>(R.id.layout_debug)?.visibility = View.GONE
            setOverlayViewVisibility(View.GONE)
            (screenOverlayView as DebugView).clear()
        }
    }

    /**
     * Observe the values for the debug and update the debug views.
     * @return the coroutine job for the observable. Can be cancelled to stop the observation.
     */
    private fun observeDebugValues() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                viewModel?.debugLastPositive?.collect { debugInfo ->
                    getMenuItemView<TextView>(R.id.debug_event_name)?.text = debugInfo.eventName
                    getMenuItemView<TextView>(R.id.debug_condition_name)?.text = debugInfo.conditionName
                    getMenuItemView<TextView>(R.id.debug_confidence_rate)?.text = debugInfo.confidenceRateText
                }
            }

            launch {
                viewModel?.debugLastPositiveCoordinates?.collect { coordinates ->
                    (screenOverlayView as DebugView).setPositiveResult(coordinates)
                }
            }

            launch {
                viewModel?.debugLastConfidenceRate?.collect { confRate ->
                    getMenuItemView<TextView>(R.id.debug_current_confidence_rate)?.text = confRate
                }
            }
        }
    }
}