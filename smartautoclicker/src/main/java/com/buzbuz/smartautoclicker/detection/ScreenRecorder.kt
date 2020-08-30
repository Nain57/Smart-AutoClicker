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
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import androidx.annotation.GuardedBy
import androidx.annotation.WorkerThread

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
 * @param display the display to be recorded.
 */
class ScreenRecorder(display: Display)  {

    private companion object {
        /** Tag for logs. */
        private const val TAG = "ScreenRecorder"
        /** Name of the virtual display generating [Image]. */
        private const val VIRTUAL_DISPLAY_NAME = "SmartAutoClicker"
        /** Name of the image processing thread. */
        private const val PROCESSING_THREAD_NAME = "SmartAutoClicker.Processing"
    }

    /** Size of the device. We keep a 1:1 ratio with the virtual display so it's also the size of the [Image] */
    private val displaySize = Point()
    /** Screen DPI density. */
    private val displayDensityDpi: Int
    /** Handler on the main thread. Used to post processing results callbacks. */
    private val mainHandler = Handler()

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

    /** Initialize the display metrics. */
    init {
        display.getSize(displaySize)

        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        displayDensityDpi = displayMetrics.densityDpi
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
     * @param context the Android context.
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     */
    fun startScreenRecord(context: Context, resultCode: Int, data: Intent) {
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

        processingThread?.let {
            it.quitSafely()
        }
        processingThread = null
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

    }

    // TODO: do something
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
        }
    }
}