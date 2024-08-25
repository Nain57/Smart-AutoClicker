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
package com.buzbuz.smartautoclicker.core.display

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Get the media projection and retries if the foreground service isn't started yet.
 * @throws SecurityException after [RETRY_MAX_COUNT] retries without success.
 */
internal suspend fun Context.getMediaProjection(resultCode: Int, data: Intent): MediaProjection {
    Log.d(TAG, "getMediaProjection")

    return (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
        .getMediaProjectionWithCurrentDelay(resultCode, data)
}


internal suspend fun MediaProjectionManager.getMediaProjectionWithCurrentDelay(
    resultCode: Int,
    data: Intent,
    retryCount: Int = 0,
): MediaProjection {
    if (retryCount > 0) delay(RETRY_DELAY)

    try {
        return getMediaProjection(resultCode, data)
    } catch (sEx: SecurityException) {
        if (retryCount >= RETRY_MAX_COUNT) throw sEx

        Log.w(TAG, "Foreground service is not started yet, retrying...")
        return getMediaProjectionWithCurrentDelay(resultCode, data, retryCount + 1)
    }
}

private const val RETRY_DELAY = 500L
private const val RETRY_MAX_COUNT = 10

private const val TAG = "ProjectionManagerExt"