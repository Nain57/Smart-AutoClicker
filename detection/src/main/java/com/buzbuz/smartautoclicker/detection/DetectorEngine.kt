/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.detection

import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log

import com.buzbuz.smartautoclicker.database.Repository
import com.buzbuz.smartautoclicker.database.domain.Event
import com.buzbuz.smartautoclicker.database.domain.Scenario

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** */
@OptIn(ExperimentalCoroutinesApi::class)
class DetectorEngine internal constructor(context: Context) {

    companion object {

        /** Tag for logs */
        private const val TAG = "DetectorEngine"
        /** Singleton preventing multiple instances of the repository at the same time. */
        @Volatile
        private var INSTANCE: DetectorEngine? = null

        /**
         * Get the engine singleton, or instantiates it if it wasn't yet.
         *
         * @param context the Android context.
         *
         * @return the engine singleton.
         */
        fun getDetectorEngine(context: Context): DetectorEngine {
            return INSTANCE ?: synchronized(this) {
                Log.i(TAG, "Instantiates new detector engine")
                val instance = DetectorEngine(context)
                INSTANCE = instance
                instance
            }
        }

        /** Clear this singleton instance, forcing to instantiates it again. */
        private fun cleanInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }

    /** The scope for all coroutines executed by this model. */
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    /** Object recording the screen of the display to detect on and trying to match the events from the scenario on it. */
    private val screenDetector = ScreenDetector(context, Repository.getRepository(context))

    /**
     * True if this model is detecting, false if not.
     * When detecting, scenario edition is not allowed. The callback provided in [startDetection] will be called
     * every time an event is detected on the screen. Is set back to false upon [stopDetection] call.
     */
    val detecting = screenDetector.isDetecting

    /** The current scenario. */
    private val _scenario = MutableStateFlow<Scenario?>(null)

    /** The list of events for the [_scenario]. */
    val scenarioEvents: StateFlow<List<Event>> = _scenario
        .flatMapLatest {
            it?.let { event ->
                Repository.getRepository(context).getCompleteEventList(event.id)
            } ?: flow { emit(emptyList<Event>()) }
        }
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    /**
     * Initialize the detector model.
     *
     * @param context the Android context.
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param scenario the scenario of events to be used for detection or to be edited.
     */
    fun init(context: Context, resultCode: Int, data: Intent, scenario: Scenario) {
        if (screenDetector.isScreenRecording.value) {
            Log.w(TAG, "The model is already initialized")
            return
        }

        screenDetector.startScreenRecord(context, resultCode, data)
        _scenario.value = scenario
    }

    /**
     * Take a screenshot of a given area on the screen.
     * The model must be initialized.
     *
     * @param area the part of the screen that will be in the resulting bitmap.
     * @param callback the callback notified upon screenshot completion.
     */
    fun captureScreenArea(area: Rect, callback: (Bitmap) -> Unit) {
        if (!screenDetector.isScreenRecording.value) {
            Log.w(TAG, "The model is not initialized")
            return
        }

        screenDetector.captureArea(area) { bitmap ->
            callback.invoke(bitmap)
        }
    }

    /**
     * Set the gesture executor for the [ScreenDetector].
     * @param listener the gesture executor.
     */
    fun setOnGestureDetectedListener(listener: (GestureDescription) -> Unit) {
        screenDetector.setOnGestureDetectedListener(listener)
    }

    /**
     * Start the detection of the events on the screen.
     * The model must be initialized.
     */
    fun startDetection() {
        if (!screenDetector.isScreenRecording.value || detecting.value) {
            Log.w(TAG, "Can't start detection, the model is not initialized or already started.")
            return
        }

        screenDetector.startDetection(scenarioEvents.value)
    }

    /** Stop a previously started detection. */
    fun stopDetection() {
        if (!detecting.value) {
            Log.w(TAG, "Can't stop detection, the model not detecting.")
            return
        }

        screenDetector.stopDetection()
    }

    /** Stop this model, releasing the resources for screen recording and the detection, if started. */
    fun stop() {
        if (!screenDetector.isScreenRecording.value) {
            Log.w(TAG, "Can't stop, the model not initialized.")
            return
        }

        if (detecting.value) {
            stopDetection()
        }

        _scenario.value = null
        screenDetector.stop()
    }

    /** Clear this engine. It can't be used after this call. */
    fun clear() {
        if (screenDetector.isScreenRecording.value) {
            Log.w(TAG, "Clearing the model but it was still started.")
            screenDetector.stop()
        }

        Log.i(TAG, "clear")
        screenDetector.setOnGestureDetectedListener(null)
        coroutineScope.cancel()
        cleanInstance()
    }
}