
package com.buzbuz.smartautoclicker.core.display.recorder

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat


fun ActivityResultLauncher<Intent>.showMediaProjectionWarning(context: Context, forceEntireScreen: Boolean, onError: () -> Unit) {
    ContextCompat.getSystemService(context, MediaProjectionManager::class.java)
        ?.let { projectionManager ->
            // The component name defined in com.android.internal.R.string.config_mediaProjectionPermissionDialogComponent
            // specifying the dialog to start to request the permission is invalid on some devices (Chinese Honor6X Android 10).
            // There is nothing to do in those cases, the app can't be used.
            try {
                Log.i(TAG, "Requesting MediaProjection")
                launch(projectionManager.createScreenCaptureIntentCompat(forceEntireScreen))
            } catch (ex: Exception) {
                Log.e(TAG, "Can't start projection permission screen")
                onError()
            }
        }
}


private fun MediaProjectionManager.createScreenCaptureIntentCompat(forceEntireScreen: Boolean): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && forceEntireScreen)
        createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
    else createScreenCaptureIntent()


private const val TAG = "ProjectionRequest"