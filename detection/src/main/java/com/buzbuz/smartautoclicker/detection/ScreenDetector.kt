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
import android.graphics.Point
import android.graphics.Rect
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log

import androidx.annotation.AnyThread
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread

import com.buzbuz.smartautoclicker.database.Repository
import com.buzbuz.smartautoclicker.database.domain.Event
import com.buzbuz.smartautoclicker.extensions.ScreenMetrics

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Detects [Event] conditions on a display and execute its actions.
 *
 * In order to detect, you must start recording the screen to get images to detect on, this can be done by calling
 * [startScreenRecord]. Then, to take a screenshot of the screen, you can use [captureArea]. Or, you can start the
 * detection of a list of [Event] by using [startDetection].
 * The states of the recording and the detection are available in [isScreenRecording] and [isDetecting] respectively.
 * Once you no longer needs to capture or detect, call [stopDetection] or [stop] to release all processing resources.
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
    /** Handler on the main thread. Used to post processing results callbacks. */
    private val mainHandler = Handler(Looper.getMainLooper())
    /** Record the screen and provide images of it regularly via [onNewImage]. */
    private val screenRecorder = ScreenRecorder(::onNewImage)
    /** The cache for image processing optimization. */
    private val cache = Cache { path, width, height ->
        // We can run blocking here, we are on the screen detector thread
        runBlocking(Dispatchers.IO) {
            repository.getBitmap(path, width, height)
        }
    }
    /** Process the events conditions to detect them on the screen. */
    private val conditionDetector = ConditionDetector(cache)
    /** Execute the detected event actions. */
    private val actionExecutor = ActionExecutor()

    /** The current size of the display, in pixels. */
    @GuardedBy("mainHandler")
    private var displaySize: Point = Point()
        get() = synchronized(mainHandler) { field }
        set(value) = synchronized(mainHandler) { field = value }

    /**
     * Information about the current screen capture.
     * The Rect is the area on the screen to be capture and the lambda is the callback to be called once the capture is
     * complete.
     */
    @GuardedBy("mainHandler")
    private var captureInfo: Pair<Rect, (Bitmap) -> Unit>? = null
        get() = synchronized(mainHandler) { field }
        set(value) = synchronized(mainHandler) { field = value }

    /**
     * Information about the current detection session.
     * The list contains the clicks to be detected and the lambda is the callback to be called once a detection occurs.
     */
    @GuardedBy("mainHandler")
    private var detectionInfo: List<Event>? = null
        get() = synchronized(mainHandler) { field }
        set(value) = synchronized(mainHandler) { field = value }

    /** Background thread executing the Image processing code. */
    @VisibleForTesting
    var processingThread: HandlerThread? = null
    /** Handler on the [processingThread]. */
    private var processingThreadHandler: Handler? = null
    // TODO: Get rid of android concurrency to use only coroutines
    /** Coroutine scope for the image processing. */
    private var processingCoroutineScope = CoroutineScope(Job())
    /** Coroutine dispatcher for the image processing. */
    private var processingCoroutineDispatcher: CoroutineDispatcher? = null

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
     * Once started, you can use [captureArea] or [startDetection]. Once your are done, call [stop].
     *
     * @param context the Android context.
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     */
    @MainThread
    fun startScreenRecord(context: Context, resultCode: Int, data: Intent) {
        if (_isScreenRecording.value) {
            Log.w(TAG, "startScreenRecord: Screen record is already started")
            return
        }

        displaySize = screenMetrics.getScreenSize()
        screenMetrics.registerOrientationListener(orientationListener)

        processingThread = HandlerThread(PROCESSING_THREAD_NAME).apply {
            start()
            processingThreadHandler = Handler(looper)
            processingCoroutineDispatcher = processingThreadHandler!!.asCoroutineDispatcher()
        }

        screenRecorder.apply {
            startProjection(context, resultCode, data) {
                stop()
            }

            processingCoroutineScope.launch(processingCoroutineDispatcher!!) {
                startScreenRecord(context, displaySize, processingThreadHandler!!)
                _isScreenRecording.emit(true)
            }
        }
    }

    /**
     * Capture the provided area on the next [Image] of the screen.
     *
     * After calling this method, the next [Image] processed by the [onNewImage] will be cropped to the provided area
     * and a bitmap will be generated from it, then notified through the provided callback.
     * [isScreenRecording] should be true to capture. Calling [stop] will drop any capture info provided here.
     *
     * @param area the area of the screen to be captured.
     * @param callback the object to notify upon capture completion.
     */
    @MainThread
    fun captureArea(area: Rect, callback: (Bitmap) -> Unit) {
        if (!_isScreenRecording.value) {
            Log.w(TAG, "captureArea: Screen record is not started.")
            return
        }

        captureInfo = area to callback
        processingThreadHandler?.post(::onCaptureImage)
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
     * [isScreenRecording] should be true to capture. Detection can be stopped with [stopDetection] or [stop].
     *
     * @param events the list of events to be detected on the screen.
     */
    @AnyThread
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

        processingCoroutineScope.launch {
            _isDetecting.emit(true)
        }
        detectionInfo = events
    }

    /**
     * Stop the screen detection started with [startDetection].
     *
     * After a call to this method, the events provided in the start method will no longer be checked on the current
     * image. Note that this will not stop the screen recording, you should still call [stop] to completely
     * release the [ScreenDetector] resources.
     */
    @AnyThread
    fun stopDetection() {
        processingCoroutineScope.launch {
            _isDetecting.emit(false)
        }
        detectionInfo = null
    }

    /**
     * Stop the screen recording and the detection, if any.
     *
     * First, calls [stopDetection] if the detection was active. Then, stop the screen recording and release any related
     * resources.
     */
    @MainThread
    fun stop() {
        if (!_isScreenRecording.value) {
            Log.w(TAG, "stop: Screen record is already stopped.")
            return
        } else if (_isDetecting.value) {
            stopDetection()
        }

        screenMetrics.unregisterOrientationListener()
        processingCoroutineDispatcher?.let {
            processingCoroutineScope.launch(it) {
                screenRecorder.stopProjection()
                cache.release()
                _isScreenRecording.emit(false)
                processingThread?.quitSafely()
                processingThreadHandler = null
                processingThread = null
            }
        }
    }

    /**
     * Called when the orientation of the screen changes.
     *
     * As we now have different screen metrics, we need to stop and start the virtual display with the correct one.
     */
    private fun onOrientationChanged() {
        processingCoroutineDispatcher?.let {
            processingCoroutineScope.launch(it) {
                screenRecorder.stopScreenRecord()
                displaySize = screenMetrics.getScreenSize()
                screenRecorder.startScreenRecord(context, displaySize, processingThreadHandler!!)
            }
        }
    }

    /**
     * Called when a screenshot is required by [captureArea].
     * Process the currently displayed image and creates a bitmap of it.
     */
    @WorkerThread
    private fun onCaptureImage() {
        if (cache.currentImage == null) return

        // Refresh cached values on the image, we are going to use it.
        cache.refreshProcessedImage(displaySize)

        // A screen capture is requested, process it and notify the resulting bitmap.
        captureInfo?.let { capture ->
            notifyCapture(
                Bitmap.createBitmap(
                    cache.screenBitmap!!, capture.first.left, capture.first.top,
                    capture.first.width(), capture.first.height()
                )
            )
        }
    }

    /**
     * Called when a new Image of the screen is available.
     *
     * Used as listener for [ScreenRecorder] and executed on the thread handled by [processingThread], this method will
     * either make a screen capture or tries to detect an event depending on the current values for [captureInfo] and
     * [detectionInfo]. If none of those values are not null, the Image will be ignored.
     *
     * @param imageReader the image reader providing [Image] to process. Should be the same reader as [imageReader].
     */
    @WorkerThread
    private fun onNewImage(imageReader: ImageReader) {
        cache.currentImage?.close()

        cache.currentImage = imageReader.acquireLatestImage()

        // An area has been found and we are waiting for its actions to be executed or no event list to detect ?
        // We have nothing to do.
        if (detectionInfo == null || actionExecutor.state != ActionExecutor.State.IDLE) {
            return
        }

        // Check if an event has reached its max execution count.
        executedEvents.forEach { (event, executedCount) ->
            event.stopAfter?.let { stopAfter ->
                if (stopAfter <= executedCount) {
                    stopDetection()
                }
            }
        }

        // Refresh cached values on the image, we are going to use it.
        cache.refreshProcessedImage(displaySize)

        // A detection is ongoing, process the scenario to detect an event that fulfils its conditions.
        detectionInfo?.let { detectionInfo ->
            conditionDetector.detect(detectionInfo)?.let { event ->
                executedEvents[event] = executedEvents[event]?.plus(1)
                    ?: throw IllegalStateException("Can' find the event in the executed events map.")

                event.actions?.let { actions ->
                    actionExecutor.executeActions(actions)
                }
            }
        }
    }

    /**
     * Notify the [captureInfo] callback for capture completion.
     *
     * Executed on the thread handled by [processingThread], this method will clear the [captureInfo] values and
     * notifies the capture listener contained in it with the provided capture bitmap value. The callback will be
     * executed on the main thread.
     *
     * @param capture the bitmap of the capture request.
     */
    @WorkerThread
    private fun notifyCapture(capture: Bitmap) {
        val captureCallback = captureInfo!!.second
        captureInfo = null
        mainHandler.post {
            captureCallback.invoke(capture)
        }
    }
}

/** Tag for logs. */
private const val TAG = "ScreenDetector"
/** Name of the image processing thread. */
private const val PROCESSING_THREAD_NAME = "SmartAutoClicker.Processing"