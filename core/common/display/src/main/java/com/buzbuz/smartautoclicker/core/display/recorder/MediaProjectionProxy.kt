/*
 * Copyright (C) 2024 Kevin Buzeau
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
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.delay
import javax.inject.Inject

internal class MediaProjectionProxy @Inject constructor() {

    /**
     * The object granting applications the ability to capture screen contents by creating a [VirtualDisplay].
     * Can only be not null if the user have granted the permission displayed by
     * [MediaProjectionManager.createScreenCaptureIntent].
     */
    private var projection: MediaProjection? = null
    /** */
    private var getProjectionRetries: Int = 0
    /** Listener to notify upon projection ends. */
    private var onStopListener: (() -> Unit)? = null


    fun isMediaProjectionStarted(): Boolean =
        projection != null

    fun getMediaProjection(): MediaProjection =
        projection!!

    suspend fun startMediaProjection(context: Context, resultCode: Int, data: Intent, stopListener: () -> Unit): Boolean {
        onStopListener = stopListener

        Log.i(TAG, "Get MediaProjection")

        projection = getMediaProjectionWithRetryDelay(context.getAndroidMediaProjectionManager(), resultCode, data)
            ?.apply { registerCallback(projectionCallback, Handler(Looper.getMainLooper())) }

        if (projection == null) {
            onStopListener = null
            getProjectionRetries = 0
            return false
        }

        return true
    }

    fun stopMediaProjection() {
        Log.i(TAG, "Stop MediaProjection")

        projection?.apply {
            unregisterCallback(projectionCallback)
            stop()
        }
        projection = null
        onStopListener = null
    }

    private suspend fun getMediaProjectionWithRetryDelay(
        projectionManager: MediaProjectionManager,
        resultCode: Int,
        data: Intent,
    ): MediaProjection? {
        if (getProjectionRetries > 0) delay(GET_PROJECTION_RETRY_DELAY_MS)

        try {
            return projectionManager.getMediaProjection(resultCode, data)
        } catch (sEx: SecurityException) {
            if (getProjectionRetries >= GET_PROJECTION_RETRY_MAX_COUNT) {
                Log.e(TAG, "Failed to get MediaProjection after $getProjectionRetries retries")
                return null
            }

            Log.w(TAG, "Foreground service is not started yet, retrying...")
            getProjectionRetries += 1
            return getMediaProjectionWithRetryDelay(projectionManager, resultCode, data)
        }
    }

    /** Called when the user have stopped the projection by clicking on the 'Cast' icon in the status bar. */
    private val projectionCallback = object : MediaProjection.Callback() {

        override fun onStop() {
            Log.i(TAG, "Projection stopped by the user")
            // We only notify, we let the detector take care of calling stopScreenRecord
            onStopListener?.invoke()
        }
    }
}

private fun Context.getAndroidMediaProjectionManager(): MediaProjectionManager =
    (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)

private const val GET_PROJECTION_RETRY_DELAY_MS = 500L
private const val GET_PROJECTION_RETRY_MAX_COUNT = 10

/** Tag for logs. */
private const val TAG = "MediaProjectionManager"