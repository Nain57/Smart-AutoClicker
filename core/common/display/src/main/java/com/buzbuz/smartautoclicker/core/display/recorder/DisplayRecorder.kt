/*
 * Copyright (C) 2023 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.core.display.recorder

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Record the screen and provide [Image] from it.
 *
 * Uses the [MediaProjection] API to create a [VirtualDisplay] not shown to the user and containing a copy of the
 * user device screen content. An [ImageReader] is attached to this display in order to monitor every new frame
 * displayed on the screen, received in the form of an [Image]. Then, process those Image with ScenarioProcessor
 * according to the current mode (capture/detection). All Image processing code is executed on a background thread
 * (methods annotated with [WorkerThread]), and all results callbacks are executed on the main thread (the thread that
 * has instantiated this class).
 *
 * To start recording, call [startProjection] (see method documentation for permission management). This must be done
 * before any other action on this object. Once the recording isn't necessary anymore, you must stop it by calling
 * [stopProjection] in order to release all resources associated with this object.
 */
@MainThread
@Singleton
class DisplayRecorder @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaProjectionProxy: MediaProjectionProxy,
    private val imageReaderProxy: ImageReaderProxy,
) {

    /** Synchronization mutex. */
    private val mutex = Mutex()

    /** Virtual display capturing the content of the screen. */
    private var virtualDisplay: VirtualDisplay? = null
    /** Listener to notify upon projection ends. */
    private var stopListener: (() -> Unit)? = null

    /**
     * Start the media projection.
     *
     * Initialize all values required for screen recording and start the thread managing the processing. This method
     * should be called before anything else in this class. Once you are done with the screen recording, you should
     * call [stopProjection] in order to release all resources.
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
     * @param stoppedListener listener called when the projection have been stopped unexpectedly.
     */
    suspend fun startProjection(resultCode: Int, data: Intent, stoppedListener: () -> Unit) = mutex.withLock {
        if (mediaProjectionProxy.isMediaProjectionStarted()) {
            Log.w(TAG, "Attempting to start media projection while already started.")
            return@withLock
        }

        Log.d(TAG, "Start media projection")

        if (!mediaProjectionProxy.startMediaProjection(context, resultCode, data, stoppedListener)) {
            Log.e(TAG, "Failed to start media projection")
            stoppedListener()
        }
    }

    /**
     * Start the screen record.
     * This method should not be called from the main thread, but the processing thread.
     *
     * @param displaySize the size of the display, in pixels.
     */
    suspend fun startScreenRecord(displaySize: Point): Unit = mutex.withLock {
        if (!mediaProjectionProxy.isMediaProjectionStarted() || virtualDisplay != null) {
            Log.w(TAG, "Attempting to start screen record while already started.")
            return
        }

        Log.i(TAG, "Start screen record with display size $displaySize")

        imageReaderProxy.resize(displaySize)
        virtualDisplay = mediaProjectionProxy.createVirtualDisplay(
            displaySize = displaySize,
            densityDpi = context.resources.configuration.densityDpi,
            surface = imageReaderProxy.surface,
        )
    }

    suspend fun resizeDisplay(displaySize: Point): Unit = mutex.withLock {
        val vDisplay = virtualDisplay ?: return

        Log.i(TAG, "Resizing virtual display to $displaySize")

        imageReaderProxy.resize(displaySize)
        vDisplay.surface = imageReaderProxy.surface
        vDisplay.resize(
            displaySize.x,
            displaySize.y,
            context.resources.configuration.densityDpi,
        )
    }

    /** @return the last image of the screen, or null if they have been processed. */
    suspend fun acquireLatestBitmap(): Bitmap? = mutex.withLock {
        imageReaderProxy.getLastFrame()
    }

    suspend fun takeScreenshot(completion: suspend (Bitmap) -> Unit) {
        var finished = false
        do {
            imageReaderProxy.getLastFrame()?.let { screenFrame ->
                completion(screenFrame)
                finished = true
            }

        } while (!finished)
    }

    /**
     * Stop the screen recording.
     * This method should not be called from the main thread, but the processing thread.
     */
    suspend fun stopScreenRecord() = mutex.withLock {
        Log.d(TAG, "Stop screen record")

        virtualDisplay?.apply {
            release()
            virtualDisplay = null
        }
        imageReaderProxy.close()
    }

    /**
     * Stop the media projection previously started with [startProjection].
     *
     * This method will free/close any resources related to screen recording. If a detection was started, it will be
     * stopped. If the screen record wasn't started, this method will have no effect.
     */
    suspend fun stopProjection() {
        Log.d(TAG, "Stop media projection")

        stopScreenRecord()

        mutex.withLock {
            mediaProjectionProxy.stopMediaProjection()
        }
    }
}

/** Tag for logs. */
private const val TAG = "DisplayRecorder"