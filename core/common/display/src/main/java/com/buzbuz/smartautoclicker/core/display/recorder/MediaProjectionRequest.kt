/*
 * Copyright (C) 2026 Kevin Buzeau
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

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat


/**
 * Handles the request for MediaProjection.
 * Basically a wrapper over [ActivityResult] API dedicated to the Media Projection request.
 *
 * - First [registerForActivityResult] should be called in the onCreate method of your [ActivityResultCaller].
 * - Then call [showMediaProjectionWarning] to display the projection dialog and define your request results callbacks.
 */
class MediaProjectionRequest {

     private lateinit var resultLauncher: ActivityResultLauncher<Intent>

     private var successListener: ((resultCode: Int, data: Intent) -> Unit)? = null
     private var failureListener: (() -> Unit)? = null

     fun registerForActivityResult(resultCaller: ActivityResultCaller) {
          val contract = ActivityResultContracts.StartActivityForResult()
          resultLauncher = resultCaller.registerForActivityResult(contract) { result -> onResult(result) }
     }

     fun showMediaProjectionWarning(
          context: Context,
          forceEntireScreen: Boolean,
          onSuccess: (resultCode: Int, data: Intent) -> Unit,
          onFailure: () -> Unit,
          onError: () -> Unit,
     ) {
          val projectionManager = ContextCompat
               .getSystemService(context, MediaProjectionManager::class.java) ?: return

          // The component name defined in com.android.internal.R.string.config_mediaProjectionPermissionDialogComponent
          // specifying the dialog to start to request the permission is invalid on some devices (Chinese Honor6X Android 10).
          // There is nothing to do in those cases, the app can't be used.
          try {
               Log.i(TAG, "Requesting MediaProjection")
               successListener = onSuccess
               failureListener = onFailure
               resultLauncher.launch(projectionManager.createScreenCaptureIntentCompat(forceEntireScreen))
          } catch (ex: Exception) {
               Log.e(TAG, "Can't start projection permission screen", ex)
               successListener = null
               failureListener = null
               onError()
          }
     }

     private fun onResult(result: ActivityResult) {
          val projectionIntent = result.data
          if (result.resultCode == RESULT_OK && projectionIntent != null) {
               successListener?.invoke(result.resultCode, projectionIntent)
          } else {
               failureListener?.invoke()
          }

          successListener = null
          failureListener = null
     }

     private fun MediaProjectionManager.createScreenCaptureIntentCompat(forceEntireScreen: Boolean): Intent =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && forceEntireScreen)
               createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
          else createScreenCaptureIntent()

}

private const val TAG = "MediaProjectionRequest"