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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.annotation.GuardedBy
import androidx.annotation.WorkerThread
import com.buzbuz.smartautoclicker.clicks.ClickCondition

import com.buzbuz.smartautoclicker.clicks.ClickInfo
import com.buzbuz.smartautoclicker.extensions.displaySize

/**
 * Record the screen and provide screen capture and click detection on it.
 *
 * Uses the [MediaProjection] API to create a [VirtualDisplay] not shown to the user and containing a copy of the
 * user device screen content. An [ImageReader] is attached to this display in order to monitor every new frame
 * displayed on the screen, received in the form of an [Image]. Then, process those Image with [ImageProcessor]
 * according to the current mode (capture/detection). All Image processing code is executed on a background thread
 * (methods annotated with [WorkerThread]), and all results callbacks are executed on the main thread (the thread that
 * has instantiated this class).
 *
 * To start recording, call [startScreenRecord] (see method documentation for permission management). This must be done
 * before any other action on this object. Once the recording isn't necessary anymore, you must stop it by calling
 * [stopScreenRecord] in order to release all resources associated with this object.
 *
 * @param context the Android context.
 * @param stoppedListener notified when the screen record has been stopped by the user.
 */
class ScreenRecorder(private val context: Context, private val stoppedListener: () -> Unit)  {

    private companion object {
        /** Tag for logs. */
        private const val TAG = "ScreenRecorder"
        /** Name of the virtual display generating [Image]. */
        private const val VIRTUAL_DISPLAY_NAME = "SmartAutoClicker"
        /** Name of the image processing thread. */
        private const val PROCESSING_THREAD_NAME = "SmartAutoClicker.Processing"
    }

    /** Size of the device. We keep a 1:1 ratio with the virtual display so it's also the size of the [Image] */
    private val displaySize : Point
    /** Screen DPI density. */
    private val displayDensityDpi: Int
    /** Handler on the main thread. Used to post processing results callbacks. */
    private val mainHandler = Handler(Looper.getMainLooper())
    /**
     * Process the [Image] from the virtual display to capture screenshots/detect clicks.
     * Should only be accessed through [processingThread] or methods annotated [WorkerThread].
     */
    private val imageProcessor : ImageProcessor

    /**
     * The token granting applications the ability to capture screen contents by creating a [VirtualDisplay].
     * Can only be not null if the user have granted the permission displayed by
     * [MediaProjectionManager.createScreenCaptureIntent].
     */
    private var projection: MediaProjection? = null
    /** Background thread executing the Image processing code. */
    private var processingThread: HandlerThread? = null
    /** Allow access to [Image] rendered into the surface view of the [VirtualDisplay] */
    private var imageReader: ImageReader? = null
    /** Virtual display capturing the content of the screen. */
    private var virtualDisplay: VirtualDisplay? = null
    /**
     * The image currently processed.
     * Used as a cache to avoid allocating a new one at each frame. Should only be accessed from [processingThread].
     */
    private var currentImage: Image? = null

    /**
     * Information about the current screen capture.
     * The Rect is the area on the screen to be capture and the lambda is the callback to be called once the capture is
     * complete.
     */
    @GuardedBy("mainHandler")
    private var captureInfo: Pair<Rect, (ClickCondition) -> Unit>? = null
        get() = synchronized(mainHandler) { field }
        set(value) = synchronized(mainHandler) { field = value }
    /**
     * Information about the current detection session.
     * The list contains the clicks to be detected and the lambda is the callback to be called once a detection occurs.
     */
    @GuardedBy("mainHandler")
    private var detectionInfo: Pair<List<ClickInfo>, (ClickInfo) -> Unit>? = null
        get() = synchronized(mainHandler) { field }
        set(value) = synchronized(mainHandler) { field = value }

    /** True if a click is detected and we are waiting its delay before next processing. */
    @GuardedBy("mainHandler")
    private var isAreaFound = false
        get() = synchronized(mainHandler) { field }
        set(value) = synchronized(mainHandler) { field = value }

    /** True if we are currently detecting clicks, false if not. */
    val isDetecting: Boolean
        get() { return synchronized(mainHandler) { detectionInfo != null } }

    /** Initialize the display metrics and the image processor. */
    init {
        displaySize = context.getSystemService(WindowManager::class.java).displaySize
        displayDensityDpi = context.resources.configuration.densityDpi
        imageProcessor = ImageProcessor(context, displaySize)
    }

    /**
     * Start recording the screen.
     *
     * Initialize all values required for screen recording and start the thread managing the processing. This method
     * should be called before anything else in this class. Once you are done with the screen recording, you should
     * call [stopScreenRecord] in order to release all resources.
     *
     * Recording the screen requires the media projection permission code and its data intent, they both can be
     * retrieved using the results of the activity intent provided by [MediaProjectionManager.createScreenCaptureIntent]
     * (this Intent shows the dialog warning about screen recording privacy). Any attempt to call this method without
     * the correct screen capture intent result will leads to a crash.
     *
     * If the screen record was already started, this method will have no effect.
     *
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     */
    fun startScreenRecord(resultCode: Int, data: Intent) {
        if (processingThread != null) {
            Log.w(TAG, "Attempting to start screen record while already started.")
            return
        }

        processingThread = HandlerThread(PROCESSING_THREAD_NAME).apply {
            start()
        }

        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        projection = projectionManager.getMediaProjection(resultCode, data).apply {
            registerCallback(projectionCallback, null)
        }
        @SuppressLint("WrongConstant")
        imageReader = ImageReader.newInstance(displaySize.x, displaySize.y, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener(::onImageAvailable, Handler(processingThread!!.looper))
        }
        virtualDisplay = projection!!.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME, displaySize.x, displaySize.y,
            displayDensityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader!!.surface, null,
            null)
    }

    /**
     * Stop the screen recording previously started with [startScreenRecord].
     *
     * This method will free/close any resources related to screen recording. If a detection was started, it will be
     * stopped. If the screen record wasn't started, this method will have no effect.
     */
    fun stopScreenRecord() {
        virtualDisplay?.apply {
            release()
            virtualDisplay = null
        }
        imageReader?.apply {
            setOnImageAvailableListener(null, null)
            close()
            imageReader = null
        }
        projection?.apply {
            unregisterCallback(projectionCallback)
            projection = null
        }

        detectionInfo = null

        processingThread?.let {
            Handler(it.looper).post { imageProcessor.releaseCache() }
            it.quitSafely()
        }
        processingThread = null
    }

    /**
     * Capture the provided area on the next [Image] of the screen.
     *
     * After calling this method, the next [Image] processed by the [imageReader] will be cropped to the provided area
     * and a bitmap will be generated from it, then notified through the provided callback.
     * Calling [stopScreenRecord] will drop any capture info provided here.
     *
     * @param area the area of the screen to be captured.
     * @param callback the object to notify upon capture completion.
     */
    fun captureArea(area: Rect, callback: (ClickCondition) -> Unit) {
        captureInfo = area to callback
    }

    /**
     * Start the screen detection.
     *
     * After calling this method, all [Image] displayed on the screen will be checked for the provided clicks conditions
     * fulfillment. For each image, the first click in the list that is detected will be notified through the provided
     * callback.
     * Detection can be stopped with [stopDetection] or [stopScreenRecord].
     *
     * @param clicks the list of clicks to be detected on the screen.
     * @param callback the object to notify upon click detection.
     */
    fun startDetection(clicks: List<ClickInfo>, callback: (ClickInfo) -> Unit) {
        detectionInfo = clicks to callback
    }

    /**
     * Stop the screen detection started with [startDetection].
     *
     * After a call to this method, the clicks provided in the start method will no longer be checked on the current
     * image. Note that this will not stop the screen recording, you should still call [stopScreenRecord] to completely
     * release the [ScreenRecorder] resources.
     */
    fun stopDetection() {
        detectionInfo = null
    }

    /**
     * Called when a new Image of the screen is available.
     *
     * Used as listener for [ImageReader.OnImageAvailableListener] and executed on the thread handled by
     * [processingThread], this method will start the screen capture or tries to detect a click depending on the current
     * values for [captureInfo] and [detectionInfo]. If none of those values are not null, or if we just detected a
     * click and we are waiting for its delay, the Image will be ignored.
     *
     * @param reader the image reader providing [Image] to process. Should be the same reader as [imageReader].
     */
    @WorkerThread
    private fun onImageAvailable(reader: ImageReader) {
        currentImage = reader.acquireLatestImage()

        currentImage?.use { image ->
            // An area has been found and we are waiting its clicks delay before detecting something else or
            // no capture to do and no click list to detect ? We have nothing to do.
            if (isAreaFound || captureInfo == null && detectionInfo == null) {
                return
            }

            captureInfo?.let { capture ->
                notifyCapture(imageProcessor.captureArea(image, capture.first))
                return
            }

            detectionInfo?.let { detectionInfo ->
                imageProcessor.detect(image, detectionInfo.first)?.let {
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
     * @param clickCondition the clickCondition captured to be propagated through the callback.
     */
    @WorkerThread
    private fun notifyCapture(clickCondition: ClickCondition) {
        val captureCallback = captureInfo!!.second
        captureInfo = null
        mainHandler.post {
            captureCallback.invoke(clickCondition)
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

    /** Called when the user have stopped the projection by clicking on the 'Cast' icon in the status bar. */
    private val projectionCallback = object : MediaProjection.Callback() {

        override fun onStop() {
            Log.i(TAG, "Projection stopped by the user")
            // We only notify, we let the detector take care of calling stopScreenRecord
            stoppedListener.invoke()
        }
    }
}