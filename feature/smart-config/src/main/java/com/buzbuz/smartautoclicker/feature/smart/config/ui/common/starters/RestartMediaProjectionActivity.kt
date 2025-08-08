
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.starters

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.display.recorder.showMediaProjectionWarning
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository
import com.buzbuz.smartautoclicker.feature.smart.config.R

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RestartMediaProjectionActivity : AppCompatActivity() {

    companion object {

        fun getStartIntent(context: Context): Intent =
            Intent(context, RestartMediaProjectionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    private val viewModel: RestartMediaProjectionViewModel by viewModels()

    @Inject lateinit var overlayManager: OverlayManager

    /** The result launcher for the projection permission dialog. */
    private lateinit var projectionActivityResult: ActivityResultLauncher<Intent>
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transparent)

        projectionActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val data = result.data
                if (data == null || result.resultCode != RESULT_OK) {
                    Log.i(TAG, "Media projection permission rejected")
                    finishActivity()
                    return@registerForActivityResult
                }

                Log.i(TAG, "Media projection permission granted, restart recording")
                viewModel.restartScreenRecord(result.resultCode, data)

                finishActivity()
            }

        projectionActivityResult.showMediaProjectionWarning(
            context = this,
            forceEntireScreen = viewModel.isEntireScreenCaptureForced(),
            onError = { finishActivity() },
        )
    }

    private fun finishActivity() {
        dialog?.dismiss()
        dialog = null

        overlayManager.navigateUp(this)
        finish()
    }
}

private const val TAG = "RequestMediaProjectionActivity"