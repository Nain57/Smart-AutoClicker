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
package com.buzbuz.smartautoclicker.service

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.view.ContextThemeWrapper
import android.view.Display
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.clicks.ClickInfo
import com.buzbuz.smartautoclicker.clicks.ClickRepository
import com.buzbuz.smartautoclicker.clicks.database.ScenarioEntity
import com.buzbuz.smartautoclicker.detection.ScreenRecorder
import com.buzbuz.smartautoclicker.ui.dialogs.ClickListDialog
import com.buzbuz.smartautoclicker.ui.overlays.MainMenu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Central class for the background click detection.
 *
 * This class creates the base overlay menu and handles the communication between the overlaid Ui, the click repository
 * and the detector.
 *
 * @param context the Android context.
 * @param display the display on which the detection will occur.
 * @param detectionCallback the object notified upon detection of a click to be performed.
 * @param stoppedCallback the object notified upon request to stop the detection.
 */
class Detector(
    private val context: Context,
    private val display: Display,
    private val detectionCallback: (ClickInfo) -> Unit,
    private val stoppedCallback: () -> Unit
) {

    /** LiveData observer upon the clicks of the currently loaded scenario. */
    private val clicksObserver = Observer<List<ClickInfo>?> { clicks ->
        clickListDialog?.onClickListChanged(clicks)
        screenRecorder?.apply {
            if (isDetecting) {
                startDetection(clicks ?: emptyList(), detectionCallback)
            }
        }
    }

    /** Coroutine scope executing all async operations on the click repository and the attached database. */
    private var scope: CoroutineScope? = null
    /** The repository of the clicks for the current scenario. */
    private var clickRepository: ClickRepository? = null
    /** The list of clicks for the current scenario. */
    private var clicks: LiveData<List<ClickInfo>>? = null
    /** Object recording the screen of the display to detect on and trying to match the current [clicks] on it. */
    private var screenRecorder: ScreenRecorder? = null
    /** The overlay menu providing the user interface to control the detection. */
    private var overlayMenu: MainMenu? = null
    /** The dialog providing the user interface for the clicks creation/edition. */
    private var clickListDialog: ClickListDialog? = null

    /** Tells if this object has been initialized via [init] or released via [release]. */
    var isInitialized: Boolean = false
        private set

    /**
     * Initialize the detector.
     *
     * This will start the overlay menu and initialize of elements for the screen detection. This requires the media
     * projection permission code and its data intent, they both can be retrieved using the results of the activity
     * intent provided by [MediaProjectionManager.createScreenCaptureIntent] (this Intent shows the dialog warning
     * about screen recording privacy). Any attempt to call this method without the correct screen capture intent
     * result will leads to a crash.
     *
     * @param coroutineScope the scope executing all async database access.
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param scenario the identifier of the scenario of clicks to be used for detection.
     */
    fun init(coroutineScope: CoroutineScope, resultCode: Int, data: Intent, scenario: ScenarioEntity) {
        if (isInitialized) {
            return
        }

        scope = coroutineScope
        clickRepository = ClickRepository.getRepository(context).apply {
            clicks = loadScenario(coroutineScope.coroutineContext, scenario.id)
            clicks!!.observeForever(clicksObserver)
        }

        screenRecorder = ScreenRecorder(context, ::onStopClicked).apply {
            startScreenRecord(resultCode, data)
        }
        overlayMenu = MainMenu(context, ::onOpenListClicked, ::onPlayPauseClicked, ::onStopClicked).apply {
            show()
        }

        isInitialized = true
    }

    /**
     * Release the detector and all associated resources.
     * This will dismiss the overlay menu, cleanup the cache of the click repository, and release all resources
     * required for screen detection, meaning that the media projection permission for screen recording is no longer
     * needed.
     */
    fun release() {
        if (!isInitialized) {
            return
        }

        isInitialized = false

        clickListDialog?.dismissDialog()
        clickListDialog = null
        overlayMenu?.dismiss()
        overlayMenu = null
        screenRecorder?.stopScreenRecord()
        screenRecorder = null

        clicks?.removeObserver(clicksObserver)
        clicks = null

        clickRepository?.apply {
            scope!!.launch { cleanupCache() }
        }
        clickRepository = null
        scope = null
    }

    /**
     * Called when the user clicks on the open click list button in the overlay menu.
     * This will open the dialog showing the current list of clicks.
     */
    private fun onOpenListClicked() {
        if (!isInitialized) {
            return
        }

        overlayMenu!!.dismiss()
        clickListDialog = ClickListDialog(
            ContextThemeWrapper(this@Detector.context, R.style.AppTheme),
            clicks!!.value ?: emptyList(),
            { area, callback -> screenRecorder!!.captureArea(area) { callback.invoke(it) } },
            { click -> scope!!.launch { clickRepository!!.addClick(click) } },
            { click -> scope!!.launch { clickRepository!!.updateClick(click) } },
            { click -> scope!!.launch { clickRepository!!.deleteClick(click) } },
            { clicks -> scope!!.launch { clickRepository!!.updateClicksPriority(clicks) } }
        ).apply {
            showDialog(::onClickListDialogDismissed)
        }
    }

    /**
     * Called when the user clicks on the play/pause button in the overlay menu.
     * This will start/stop the screen detection for clicks.
     *
     * @param isPlaying true to start playing, false to stop.
     */
    private fun onPlayPauseClicked(isPlaying: Boolean) {
        if (!isInitialized) {
            return
        }

        screenRecorder!!.let {
            if (isPlaying) {
                it.startDetection(clicks!!.value ?: emptyList(), detectionCallback)
            } else {
                it.stopDetection()
            }
        }
    }

    /**
     * Called when the user clicks on the stop button in the overlay menu.
     * Release this object and notify the [stoppedCallback].
     */
    private fun onStopClicked() {
        release()
        stoppedCallback.invoke()
    }

    /**
     * Called when the user has dismissed the click list dialog.
     * This will display back the overlay menu.
     */
    private fun onClickListDialogDismissed() {
        if (!isInitialized) {
            return
        }

        clickListDialog = null
        overlayMenu!!.show()
    }
}
