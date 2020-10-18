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
package com.buzbuz.smartautoclicker.model

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

import com.buzbuz.smartautoclicker.database.ClickCondition
import com.buzbuz.smartautoclicker.database.ClickInfo
import com.buzbuz.smartautoclicker.database.ClickRepository
import com.buzbuz.smartautoclicker.database.ClickScenario
import com.buzbuz.smartautoclicker.detection.ScreenDetector
import com.buzbuz.smartautoclicker.extensions.displaySize

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

import java.lang.IllegalStateException

/**
 * Model for the detection.
 *
 * This class must be [attach] and [detach] to the Android component hosting all Ui views (such as an Activity,
 * a Service...) in their respective lifecycle methods. The Ui can then retrieve the model via [get] as long as the
 * Android component is created.
 *
 * To initialize the model with a scenario, use [init]. This will starts the screen record and allows the
 * click creation/edition/deletion as well as screen capture and click detection.
 */
class DetectorModel private constructor(context: Context) {

    companion object {
        /** Tag for logs */
        private const val TAG = "DetectorModel"

        /** Singleton preventing multiple instances of the bitmap manager at the same time. */
        @Volatile
        private var INSTANCE: DetectorModel? = null

        /**
         * Instantiates the detector model for the provided context.
         *
         * @param context the Android context.
         *
         * @return the detector model singleton.
         */
        fun attach(context: Context): DetectorModel {
            INSTANCE?.let { throw IllegalStateException("Detector model is already attached to a context.") }
            return synchronized(this) {
                val instance = DetectorModel(context)
                INSTANCE = instance
                instance
            }
        }

        /** Release the detector model instance. */
        fun detach() {
            INSTANCE?.let { model ->
                synchronized(this) {
                    model.clear()
                    INSTANCE = null
                }
            } ?: throw IllegalStateException("Detector model is not attached to a context.")
        }

        /**
         * Get the model singleton.
         **
         * @return the detector model singleton.
         */
        fun get(): DetectorModel {
            return INSTANCE ?: throw IllegalStateException("Detector model is not attached to a context.")
        }
    }

    /** The scope for all coroutines executed by this model. */
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.IO)
    /** Repository providing access to the click database. */
    private val clickRepository = ClickRepository.getRepository(context)
    /** Provides the bitmaps for the click conditions and manages their files on the device. */
    private val bitmapManager = BitmapManager.getInstance(context)
    /** Object recording the screen of the display to detect on and trying to match the clicks from the scenario on it. */
    private val screenDetector = ScreenDetector(context.getSystemService(WindowManager::class.java).displaySize) { path, width, height ->
        // We can run blocking here, we are on the screen detector thread
        runBlocking(Dispatchers.IO) {
            bitmapManager.loadBitmap(path, width, height)
        }
    }
    /** Backing property for [scenario]. */
    private val _scenario = MutableLiveData<ClickScenario?>()
    /** Listener upon conditions without clicks. */
    private val clicklessConditionObserver = object : Observer<List<ClickCondition>> {
        override fun onChanged(conditions: List<ClickCondition>?) {
            if (conditions.isNullOrEmpty()) {
                return
            }

            coroutineScope.launch {
                bitmapManager.deleteBitmaps(conditions.map { it.path })
                clickRepository.deleteClicklessConditions()
            }
        }
    }

    /**
     * True if this model is initialized, false if not.
     * When initialized, the [scenario] and the [scenarioClicks] are defined, scenario edition is allowed, capture and
     * detection as well. Bound to [init] and [stop].
     */
    val initialized = screenDetector.isScreenRecording
    /**
     * True if this model is detecting, false if not.
     * When detecting, scenario edition is not allowed. The callback provided in [startDetection] will be called
     * every time a click is detected on the screen. Is set back to false upon [stopDetection] call.
     */
    val detecting = screenDetector.isDetecting
    /** The current scenario. Set via [init]. */
    val scenario: LiveData<ClickScenario?> get() = _scenario
    /** The list of clicks for the [scenario]. */
    val scenarioClicks: LiveData<List<ClickInfo>> = Transformations.switchMap(_scenario) { scenario ->
        scenario?.let {
            clickRepository.getClicks(it.id)
        }
    }

    /**
     * Initialize the detector model.
     *
     * @param context the Android context.
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param scenario the scenario of clicks to be used for detection or to be edited.
     */
    fun init(context: Context, resultCode: Int, data: Intent, scenario: ClickScenario) {
        if (initialized.value!!) {
            Log.w(TAG, "The model is already initialized")
            return
        }

        screenDetector.startScreenRecord(context, resultCode, data)
        _scenario.value = scenario
        clickRepository.clicklessConditions.observeForever(clicklessConditionObserver)
    }

    /**
     * Add a click to the current scenario.
     * The model must be initialized but not detecting.
     *
     * @param click the click to add to the scenario. Its id and priority must be 0.
     */
    fun addClick(click: ClickInfo) {
        ensureNotDetecting()
        if (scenario.value == null) {
            Log.e(TAG, "Can't add click with scenario id $click.scenarioId, invalid model scenario $scenario")
            return
        }

        click.scenarioId = scenario.value!!.id
        coroutineScope.launch {
            saveNewConditions(click)
            clickRepository.addClick(click)
        }
    }

    /**
     * Update a click from the current scenario.
     * The model must be initialized but not detecting.
     *
     * @param click the click to update. Must be in the current scenario and have a defined id.
     */
    fun updateClick(click: ClickInfo) {
        ensureNotDetecting()
        if (scenario.value == null || scenario.value!!.id != click.scenarioId) {
            Log.e(TAG, "Can't update click with scenario id $click.scenarioId, invalid model scenario $scenario")
            return
        }

        coroutineScope.launch {
            saveNewConditions(click)
            clickRepository.updateClick(click)
        }
    }

    /**
     * Update the priority of the clicks in the scenario.
     * The model must be initialized but not detecting.
     *
     * @param clicks the clicks, ordered by their new priorities. They must be in the current scenario and have a
     *               defined id.
     */
    fun updateClicksPriority(clicks: List<ClickInfo>) {
        ensureNotDetecting()
        if (scenario.value == null || clicks.isEmpty() || scenario.value!!.id != clicks[0].scenarioId) {
            Log.e(TAG, "Can't update click priorities, scenario is not matching.")
            return
        }

        coroutineScope.launch { clickRepository.updateClicksPriority(clicks) }
    }

    /**
     * Delete a click from the scenario.
     * The model must be initialized but not detecting.
     *
     * @param click the click to delete. Must be in the current scenario and have a defined id.
     */
    fun deleteClick(click: ClickInfo) {
        ensureNotDetecting()
        if (scenario.value == null || scenario.value!!.id != click.scenarioId) {
            Log.e(TAG, "Can't delete click with scenario id $click.scenarioId, invalid model scenario $scenario")
            return
        }

        coroutineScope.launch { clickRepository.deleteClick(click) }
    }

    /**
     * Get the bitmap for a click condition from the internal memory.
     *
     * @param condition the condition to get the bitmap of.
     * @param completionCallback the callback notified once the bitmap is available.
     */
    fun getClickConditionBitmap(condition: ClickCondition, completionCallback: (Bitmap?) -> Unit): Job {
        return coroutineScope.launch {
            val bitmap = bitmapManager.loadBitmap(
                condition.path, condition.area.width(), condition.area.height())
            withContext(Dispatchers.Main) { completionCallback.invoke(bitmap) }
        }
    }

    /**
     * Take a screenshot of a given area on the screen.
     * The model must be initialized.
     *
     * @param area the part of the screen that will be in the resulting bitmap.
     * @param callback the callback notified upon screenshot completion.
     */
    fun captureScreenArea(area: Rect, callback: (Bitmap) -> Unit) {
        if (!initialized.value!!) {
            Log.w(TAG, "The model is not initialized")
            return
        }

        screenDetector.captureArea(area) { bitmap ->
            callback.invoke(bitmap)
        }
    }

    /**
     * Start the detection of the clicks on the screen.
     * The model must be initialized.
     *
     * @param callback notified every time a click from the current scenario is detected on the screen.
     */
    fun startDetection(callback: (ClickInfo) -> Unit) {
        if (!initialized.value!! || detecting.value!!) {
            Log.w(TAG, "Can't start detection, the model is not initialized or already started.")
            return
        }

        coroutineScope.launch {
            val clicks = clickRepository.getClickList(scenario.value!!.id)
            screenDetector.startDetection(clicks, callback)
        }
    }

    /** Stop a previously started detection. */
    fun stopDetection() {
        if (!detecting.value!!) {
            Log.w(TAG, "Can't stop detection, the model not detecting.")
            return
        }

        screenDetector.stopDetection()
    }

    /** Stop this model, releasing the resources for screen recording and the detection, if started. */
    fun stop() {
        if (!initialized.value!!) {
            Log.w(TAG, "Can't stop, the model not initialized.")
            return
        }

        if (detecting.value!!) {
            stopDetection()
        }
        clickRepository.clicklessConditions.removeObserver(clicklessConditionObserver)
        _scenario.value = null
        screenDetector.stop()
        bitmapManager.releaseCache()
    }

    /**
     * Save the bitmaps of all unsaved click conditions.
     *
     * @param click the click containing the conditions to be saved.
     */
    private suspend fun saveNewConditions(click: ClickInfo) {
        click.conditionList.forEach { condition ->
            condition.bitmap?.let { conditionBitmap ->
                condition.path = bitmapManager.saveBitmap(conditionBitmap)
                condition.bitmap = null
            }
        }
    }

    /** Ensure this model is not detecting or throw an exception. */
    private fun ensureNotDetecting() {
        if (detecting.value!!) throw IllegalStateException("Can't edit scenario while detecting.")
    }

    /**
     * Clear this model and cancel the coroutine scope.
     * After a call to this method, you must re create a new model.
     */
    private fun clear() {
        if (initialized.value!!) {
            Log.w(TAG, "Clearing the model but it was still started.")
            screenDetector.stop()
        }

        coroutineScope.cancel()
    }
}