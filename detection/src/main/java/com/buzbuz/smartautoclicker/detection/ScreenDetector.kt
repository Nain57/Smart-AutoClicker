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
import android.media.Image
import android.media.projection.MediaProjectionManager
import android.util.Log

import androidx.annotation.GuardedBy

import com.buzbuz.smartautoclicker.database.Repository
import com.buzbuz.smartautoclicker.database.domain.Event
import com.buzbuz.smartautoclicker.extensions.ScreenMetrics
import com.buzbuz.smartautoclicker.opencv.ImageDetector

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Detects [Event] conditions on a display and execute its actions.
 *
 * In order to detect, you must start recording the screen to get images to detect on, this can be done by calling
 * [startScreenRecord]. Then, to take a screenshot of the screen, you can use [captureArea]. Or, you can start the
 * detection of a list of [Event] by using [startDetection].
 * The states of the recording and the detection are available in [isScreenRecording] and [isDetecting] respectively.
 * Once you no longer needs to capture or detect, call [stopDetection] or [stopScreenRecord] to release all processing resources.
 *
 * @param context the Android context.
 * @param repository the repository of the events to detect.
 */
class ScreenDetector(
    private val context: Context,
    private val repository: Repository,
) {

    /** Monitors the state of the screen. */
    private val screenMetrics = ScreenMetrics(context)
    /** Listener upon the screen orientation changes. */
    private val orientationListener = ::onOrientationChanged

    /** Record the screen and provide images via [ScreenRecorder.acquireLatestImage]. */
    private val screenRecorder = ScreenRecorder()
    /** Process the events conditions to detect them on the screen. */
    private val conditionDetector = ConditionDetector { path, width, height ->
        // We can run blocking here, we are on the screen detector thread
        runBlocking(Dispatchers.IO) {
            repository.getBitmap(path, width, height)
        }
    }
    /** Execute the detected event actions. */
    private val actionExecutor = ActionExecutor()
    /** The native detector for images. */
    private var imageDetector: ImageDetector? = null
    /** Coroutine scope for the image processing. */
    private var processingScope: CoroutineScope? = null
    /** Coroutine job for the image currently processed. */
    private var processingJob: Job? = null
    /** The bitmap of the currently processed image. Kept in order to avoid instantiating a new one everytime. */
    private var processedScreenBitmap: Bitmap? = null

    /**
     * Information about the current detection session.
     * The list contains the clicks to be detected and the lambda is the callback to be called once a detection occurs.
     */
    @GuardedBy("screenRecorder")
    private var detectionInfo: List<Event>? = null
        get() = synchronized(screenRecorder) { field }
        set(value) = synchronized(screenRecorder) { field = value }

    /** Number of execution count for each events since the detection start. */
    private val executedEvents = HashMap<Event, Int>()

    /** Backing property for [isScreenRecording]. */
    private val _isScreenRecording = MutableStateFlow(false)
    /** True if we are currently screen recording, false if not. */
    val isScreenRecording: StateFlow<Boolean> = _isScreenRecording

    /** Backing property for [isDetecting]. */
    private val _isDetecting = MutableStateFlow(false)
    /** True if we are currently detecting clicks, false if not. */
    val isDetecting: StateFlow<Boolean> = _isDetecting

    /**
     * Start the screen detection.
     *
     * This requires the media projection permission code and its data intent, they both can be retrieved using the
     * results of the activity intent provided by [MediaProjectionManager.createScreenCaptureIntent] (this Intent shows
     * the dialog warning about screen recording privacy). Any attempt to call this method without the correct screen
     * capture intent result will leads to a crash.
     *
     * Once started, you can use [captureArea] or [startDetection]. Once your are done, call [stopScreenRecord].
     *
     * @param context the Android context.
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun startScreenRecord(context: Context, resultCode: Int, data: Intent) {
        if (_isScreenRecording.value) {
            Log.w(TAG, "startScreenRecord: Screen record is already started")
            return
        }

        screenMetrics.registerOrientationListener(orientationListener)

        processingScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))

        screenRecorder.apply {
            startProjection(context, resultCode, data) {
                this@ScreenDetector.stopScreenRecord()
            }

            processingScope?.launch {
                startScreenRecord(context, screenMetrics.screenSize)
                _isScreenRecording.emit(true)
            }
        }
    }

    /**
     * Capture the provided area on the next [Image] of the screen.
     *
     * After calling this method, the next [Image] processed by the [processLatestImage] will be cropped to the provided area
     * and a bitmap will be generated from it, then notified through the provided callback.
     * [isScreenRecording] should be true to capture. Calling [stopScreenRecord] will drop any capture info provided here.
     *
     * @param area the area of the screen to be captured.
     * @param callback the object to notify upon capture completion.
     */
    fun captureArea(area: Rect, callback: (Bitmap) -> Unit) {
        if (!_isScreenRecording.value) {
            Log.w(TAG, "captureArea: Screen record is not started.")
            return
        }

        processingScope?.launch {
            screenRecorder.acquireLatestImage()?.use { image ->
                val bitmap = Bitmap.createBitmap(
                    image.toBitmap(),
                    area.left,
                    area.top,
                    area.width(),
                    area.height()
                )

                withContext(Dispatchers.Main) {
                    callback(bitmap)
                }
            }
        }
    }

    /**
     * Set the gesture executor for the [ActionExecutor].
     * @param listener the gesture executor.
     */
    fun setOnGestureDetectedListener(listener: ((GestureDescription) -> Unit)?) {
        actionExecutor.onGestureExecutionListener = listener
    }

    /**
     * Start the screen detection.
     *
     * After calling this method, all [Image] displayed on the screen will be checked for the provided clicks conditions
     * fulfillment. For each image, the first event in the list that is detected will be notified through the provided
     * callback.
     * [isScreenRecording] should be true to capture. Detection can be stopped with [stopDetection] or [stopScreenRecord].
     *
     * @param events the list of events to be detected on the screen.
     */
    fun startDetection(events: List<Event>) {
        if (!_isScreenRecording.value) {
            Log.w(TAG, "captureArea: Screen record is not started.")
            return
        } else if (_isDetecting.value) {
            Log.w(TAG, "captureArea: detection is already started.")
            return
        }

        executedEvents.clear()
        events.forEach { executedEvents[it] = 0 }

        _isDetecting.value = true
        detectionInfo = events

        processingJob = processingScope?.launch {
            imageDetector = ImageDetector()
            processLatestImage()
        }
    }

    /**
     * Stop the screen detection started with [startDetection].
     *
     * After a call to this method, the events provided in the start method will no longer be checked on the current
     * image. Note that this will not stop the screen recording, you should still call [stopScreenRecord] to completely
     * release the [ScreenDetector] resources.
     */
    fun stopDetection() {
        processingScope?.launch {
            processingJob?.cancelAndJoin()
            processingJob = null

            imageDetector?.close()
            imageDetector = null

            _isDetecting.value = false
            detectionInfo = null
        }
    }

    /**
     * Stop the screen recording and the detection, if any.
     *
     * First, calls [stopDetection] if the detection was active. Then, stop the screen recording and release any related
     * resources.
     */
    fun stopScreenRecord() {
        if (!_isScreenRecording.value) {
            Log.w(TAG, "stop: Screen record is already stopped.")
            return
        } else if (_isDetecting.value) {
            stopDetection()
        }

        screenMetrics.unregisterOrientationListener()
        processingScope?.launch {
            screenRecorder.stopProjection()
            _isScreenRecording.emit(false)

            processingScope?.cancel()
            processingScope = null
        }
    }

    /**
     * Called when the orientation of the screen changes.
     *
     * As we now have different screen metrics, we need to stop and start the virtual display with the correct one.
     */
    private fun onOrientationChanged() {
        processingScope?.launch {
            processingJob?.cancelAndJoin()

            screenRecorder.stopScreenRecord()
            screenRecorder.startScreenRecord(context, screenMetrics.screenSize)

            processingJob = processingScope?.launch {
                processLatestImage()
            }
        }
    }

    /** Process the latest image provided by the [ScreenRecorder]. */
    private suspend fun processLatestImage() {
        screenRecorder.acquireLatestImage()?.use { image ->

            // An area has been found and we are waiting for its actions to be executed or no event list to detect ?
            // We have nothing to do.
            if (detectionInfo == null || actionExecutor.state != ActionExecutor.State.IDLE) {
                return
            }

            yield()

            // Check if an event has reached its max execution count.
            executedEvents.forEach { (event, executedCount) ->
                event.stopAfter?.let { stopAfter ->
                    if (stopAfter <= executedCount) {
                        stopDetection()
                        return
                    }
                }
            }

            yield()

            // A detection is ongoing, process the scenario to detect an event that fulfils its conditions.
            detectionInfo?.let { detectionInfo ->
                imageDetector?.let { detector ->
                    detector.setScreenImage(image.toBitmap(processedScreenBitmap))
                    conditionDetector.detect(detector, detectionInfo)?.let { event ->
                        executedEvents[event] = executedEvents[event]?.plus(1)
                            ?: throw IllegalStateException("Can' find the event in the executed events map.")

                        event.actions?.let { actions ->
                            actionExecutor.executeActions(actions)
                        }
                    }
                }
            }
        }

        processingJob = processingScope?.launch {
            processLatestImage()
        }
    }
}

/**
 * Transform an Image into a bitmap.
 *
 * @param resultBitmap a bitmap to use as a cache in order to avoid instantiating an new one. If null, a new one is
 *                     created.
 * @return the bitmap corresponding to the image. If [resultBitmap] was provided, it will be the same object.
 */
private fun Image.toBitmap(resultBitmap: Bitmap? = null): Bitmap {
    var bitmap = resultBitmap
    if (bitmap == null || bitmap.width != width || bitmap.height != height) {
        val pixelStride = planes[0].pixelStride
        val rowPadding = planes[0].rowStride - pixelStride * width

        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        } else {
            bitmap.reconfigure(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        }
    }

    bitmap?.copyPixelsFromBuffer(planes[0].buffer)
    return bitmap!!
}

/** Tag for logs. */
private const val TAG = "ScreenDetector"