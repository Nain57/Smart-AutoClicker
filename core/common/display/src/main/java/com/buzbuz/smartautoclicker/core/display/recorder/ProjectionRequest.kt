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
import android.media.projection.MediaProjectionManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat


fun ActivityResultLauncher<Intent>.showMediaProjectionWarning(context: Context, onError: () -> Unit) {
    ContextCompat.getSystemService(context, MediaProjectionManager::class.java)
        ?.let { projectionManager ->
            // The component name defined in com.android.internal.R.string.config_mediaProjectionPermissionDialogComponent
            // specifying the dialog to start to request the permission is invalid on some devices (Chinese Honor6X Android 10).
            // There is nothing to do in those cases, the app can't be used.
            try {
                Log.i(TAG, "Requesting MediaProjection")
                launch(projectionManager.createScreenCaptureIntent())
            } catch (ex: Exception) {
                Log.e(TAG, "Can't start projection permission screen")
                onError()
            }
        }
}

private const val TAG = "ProjectionRequest"