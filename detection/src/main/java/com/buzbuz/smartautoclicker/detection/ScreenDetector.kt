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
package com.buzbuz.smartautoclicker.detection

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
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.buzbuz.smartautoclicker.database.ClickInfo

/**
 * Detects [ClickInfo] on a display.
 *
 * In order to detect, you must start recording the screen to get images to detect on, this can be done by calling
 * [startScreenRecord]. Then, to take a screenshot of the screen, you can use [captureArea]. Or, you can start the
 * detection of a list of [ClickInfo] by using [startDetection].
 * The states of the recording and the detection are available in [isScreenRecording] and [isDetecting] respectively.
 * Once you no longer needs to capture or detect, call [stopDetection] or [stop] to release all processing resources.
 *
 * @param displaySize the size of the display to be recorded.
 * @param bitmapSupplier provides the bitmap for the given path, width and height. This call will be made on the
 * processing thread, so you can use it directly to perform the loading from the memory, but keep in mind that the more
 * time this thread spend here, the slower the detection will be.
 */
class ScreenDetector(displaySize: Point, bitmapSupplier: (String, Int, Int) -> Bitmap?) {

    private companion object {
        /** Tag for logs. */
        private const val TAG = "ScreenDetector"
        /** Name of the image processing thread. */
        private const val PROCESSING_THREAD_NAME = "SmartAutoClicker.Processing"
    }

    /** Handler on the main thread. Used to post processing results callbacks. */
    private val mainHandler = Handler(Looper.getMainLooper())
    /** Record the screen and provide images of it regularly via [onNewImage]. */
    private val screenRecorder = ScreenRecorder(displaySize, ::onNewImage)
    /** The cache for image processing optimization. */
    private val cache = Cache(displaySize, bitmapSupplier)
    /** The click scenario processing object. */
    private val scenarioProcessor = ScenarioProcessor(cache)
    /** Backing property for [isScreenRecording]. */
    private val _isScreenRecording = MutableLiveData(false)
    /** Backing property for [isDetecting]. */
    private val _isDetecting = MutableLiveData(false)

    /**
     * Information about the current screen capture.
     * The Rect is the area on the screen to be capture and the lambda is the callback to be called once the capture is
     * complete.
     */
    @GuardedBy("mainHandler") private var captureInfo: Pair<Rect, (Bitmap) -> Unit>? = null
        get() = synchronized(mainHandler) { field }
        set(value) = synchronized(mainHandler) { field = value }
    /**
     * Information about the current detection session.
     * The list contains the clicks to be detected and the lambda is the callback to be called once a detection occurs.
     */
    @GuardedBy("mainHandler") private var detectionInfo: Pair<List<ClickInfo>, (ClickInfo) -> Unit>? = null
        get() = synchronized(mainHandler) { field }
        set(value) = synchronized(mainHandler) { field = value }
    /** True if a click is detected and we are waiting its delay before next processing. */
    @GuardedBy("mainHandler") private var isAreaFound = false
        get() = synchronized(mainHandler) { field }
        set(value) = synchronized(mainHandler) { field = value }
    /** Background thread executing the Image processing code. */
    private var processingThread: HandlerThread? = null

    /** True if we are currently detecting clicks, false if not. */
    val isDetecting: LiveData<Boolean>
        get() = _isDetecting
    /** True if we are currently screen recording, false if not. */
    val isScreenRecording: LiveData<Boolean>
        get() = _isScreenRecording

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
        if (_isScreenRecording.value!!) {
            Log.w(TAG, "startScreenRecord: Screen record is already started")
            return
        }

        processingThread = HandlerThread(PROCESSING_THREAD_NAME).apply {
            start()
            screenRecorder.startScreenRecord(context, resultCode, data, Handler(looper), this@ScreenDetector::stop)
            _isScreenRecording.value = true
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
        if (!_isScreenRecording.value!!) {
            Log.w(TAG, "captureArea: Screen record is not started.")
            return
        }

        captureInfo = area to callback
    }

    /**
     * Start the screen detection.
     *
     * After calling this method, all [Image] displayed on the screen will be checked for the provided clicks conditions
     * fulfillment. For each image, the first click in the list that is detected will be notified through the provided
     * callback.
     * [isScreenRecording] should be true to capture. Detection can be stopped with [stopDetection] or [stop].
     *
     * @param clicks the list of clicks to be detected on the screen.
     * @param callback the object to notify upon click detection.
     */
    @AnyThread
    fun startDetection(clicks: List<ClickInfo>, callback: (ClickInfo) -> Unit) {
        if (!_isScreenRecording.value!!) {
            Log.w(TAG, "captureArea: Screen record is not started.")
            return
        } else if (_isDetecting.value!!) {
            Log.w(TAG, "captureArea: detection is already started.")
            return
        }

        _isDetecting.postValue(true)
        detectionInfo = clicks to callback
    }

    /**
     * Stop the screen detection started with [startDetection].
     *
     * After a call to this method, the clicks provided in the start method will no longer be checked on the current
     * image. Note that this will not stop the screen recording, you should still call [stop] to completely
     * release the [ScreenDetector] resources.
     */
    @AnyThread
    fun stopDetection() {
        _isDetecting.postValue(false)
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
        if (!_isScreenRecording.value!!) {
            Log.w(TAG, "stop: Screen record is already stopped.")
            return
        } else if (_isDetecting.value!!) {
            stopDetection()
        }

        screenRecorder.stopScreenRecord()
        processingThread?.let {
            Handler(it.looper).post { cache.release() }
            it.quitSafely()
        }
        processingThread = null
        _isScreenRecording.value = false
    }

    /**
     * Called when a new Image of the screen is available.
     *
     * Used as listener for [ScreenRecorder] and executed on the thread handled by [processingThread], this method will
     * either make a screen capture or tries to detect a click depending on the current values for [captureInfo] and
     * [detectionInfo]. If none of those values are not null, or if we just detected a click and we are waiting for its
     * delay, the Image will be ignored.
     *
     * @param imageReader the image reader providing [Image] to process. Should be the same reader as [imageReader].
     */
    @WorkerThread
    private fun onNewImage(imageReader: ImageReader) {
        cache.currentImage = imageReader.acquireLatestImage()
        cache.currentImage?.use {
            // An area has been found and we are waiting its clicks delay before detecting something else or
            // no capture to do and no click list to detect ? We have nothing to do.
            if (isAreaFound || captureInfo == null && detectionInfo == null) {
                return
            }

            // Refresh cached values on the image, we are going to use it.
            cache.refreshProcessedImage()

            // A screen capture is requested, process it and notify the resulting bitmap.
            captureInfo?.let { capture ->
                notifyCapture(Bitmap.createBitmap(cache.screenBitmap!!, capture.first.left, capture.first.top,
                    capture.first.width(), capture.first.height()))
                return
            }

            // A detection is ongoing, process the scenario to detect a click that fulfils its conditions.
            detectionInfo?.let { detectionInfo ->
                scenarioProcessor.detect(detectionInfo.first)?.let {
                    // A click is detected, notify it
                    notifyClickDetection(it)
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

    /**
     * Notify the [detectionInfo] callback for click detection.
     *
     * Executed on the thread handled by [processingThread], this method will notifies the detection listener with the
     * provided click. The callback will be executed on the main thread.
     * Also, it will set the [isAreaFound] value to true until the [ClickInfo.delayAfterMs] is elapsed, allowing to
     * skip the [Image] processing phase to avoid detecting clicks during this waiting delay.
     *
     * @param click the click detected on the screen to be propagated through the callback.
     */
    @WorkerThread
    private fun notifyClickDetection(click: ClickInfo) {
        isAreaFound = true

        mainHandler.post {
            detectionInfo?.second?.invoke(click)
        }

        mainHandler.postDelayed({
            isAreaFound = false
        }, click.delayAfterMs)
    }
}