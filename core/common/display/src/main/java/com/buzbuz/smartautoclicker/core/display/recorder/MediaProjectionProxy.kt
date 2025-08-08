
package com.buzbuz.smartautoclicker.core.display.recorder

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.delay
import javax.inject.Inject

internal class MediaProjectionProxy @Inject constructor() {

    /**
     * The object granting applications the ability to capture screen contents by creating a [VirtualDisplay].
     * Can only be not null if the user have granted the permission displayed by
     * [MediaProjectionManager.createScreenCaptureIntent].
     */
    private var projection: MediaProjection? = null
    /**
     * The number of retries to get a media projection.
     * Samsung devices are really slow to start the foreground service and as there is no way to ensure it is
     * effectively started, we need to try a few times until it is ok.
     */
    private var getProjectionRetries: Int = 0
    /** Listener to notify upon projection ends. */
    private var onStopListener: (() -> Unit)? = null


    fun isMediaProjectionStarted(): Boolean =
        projection != null

    suspend fun startMediaProjection(context: Context, resultCode: Int, data: Intent, stopListener: () -> Unit): Boolean {
        Log.i(TAG, "Get MediaProjection")

        onStopListener = stopListener

        projection = getMediaProjectionWithRetryDelay(context.getAndroidMediaProjectionManager(), resultCode, data)
            ?.apply { registerCallback(projectionCallback, Handler(Looper.getMainLooper())) }

        if (projection == null) {
            onStopListener = null
            getProjectionRetries = 0
            return false
        }

        return true
    }

    fun createVirtualDisplay(displaySize: Point, densityDpi: Int, surface: Surface): VirtualDisplay? {
        val mediaProjection = projection ?: run {
            Log.e(TAG, "Can't create virtual display, MediaProjection is null")
            return null
        }

        return try {
            mediaProjection.createVirtualDisplay(VIRTUAL_DISPLAY_NAME, displaySize.x, displaySize.y, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null)
        } catch (sEx: SecurityException) {
            Log.e(TAG, "Can't create VirtualDisplay, screencast permission is no longer valid", sEx)
            onStopListener?.invoke()
            null
        }
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

/** Name of the virtual display generating [Image]. */
@VisibleForTesting internal const val VIRTUAL_DISPLAY_NAME = "Klickr"

/** Tag for logs. */
private const val TAG = "MediaProjectionProxy"