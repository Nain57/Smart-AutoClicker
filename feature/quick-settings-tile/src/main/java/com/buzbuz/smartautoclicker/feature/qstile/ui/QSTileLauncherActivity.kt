
package com.buzbuz.smartautoclicker.feature.qstile.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.core.display.recorder.showMediaProjectionWarning
import com.buzbuz.smartautoclicker.feature.qstile.R

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QSTileLauncherActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_SCENARIO_ID =
            "com.buzbuz.smartautoclicker.feature.qstile.ui.EXTRA_SCENARIO_ID"
        private const val EXTRA_IS_SMART_SCENARIO =
            "com.buzbuz.smartautoclicker.feature.qstile.ui.EXTRA_IS_SMART_SCENARIO"

        fun getStartIntent(context: Context, scenarioId: Long, isSmartScenario: Boolean): Intent =
            Intent(context, QSTileLauncherActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_SCENARIO_ID, scenarioId)
                .putExtra(EXTRA_IS_SMART_SCENARIO, isSmartScenario)
    }

    private val viewModel: QSTileLauncherViewModel by viewModels()
    /** The result launcher for the projection permission dialog. */
    private lateinit var projectionActivityResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qstile_launcher)

        val scenarioId = intent?.getLongExtra(EXTRA_SCENARIO_ID, -1)
        val isSmartScenario = intent?.getBooleanExtra(EXTRA_IS_SMART_SCENARIO, false)
        if (scenarioId == null || scenarioId == -1L || isSmartScenario == null) {
            Log.e(TAG, "Invalid start parameter, finish activity")
            finish()
            return
        }

        Log.i(TAG, "Start scenario from tile...")
        if (isSmartScenario) onCreateSmartScenarioLauncher(scenarioId)
        else onCreateDumbScenarioLauncher(scenarioId)
    }

    private fun onCreateDumbScenarioLauncher(scenarioId: Long) {
        viewModel.startPermissionFlowIfNeeded(
            activity = this,
            onMandatoryDenied = ::finish,
            onAllGranted = {
                Log.i(TAG, "All permissions are granted, start scenario")
                viewModel.startDumbScenario(scenarioId)
                finish()
            }
        )
    }

    private fun onCreateSmartScenarioLauncher(scenarioId: Long) {
        projectionActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                finish()
                return@registerForActivityResult
            }

            Log.i(TAG, "Media projection us running, start scenario")

            viewModel.startSmartScenario(result.resultCode, result.data!!, scenarioId)
            finish()
        }

        viewModel.startPermissionFlowIfNeeded(
            activity = this,
            onMandatoryDenied = ::finish,
            onAllGranted = ::showMediaProjectionWarning
        )
    }

    /** Show the media projection start warning. */
    private fun showMediaProjectionWarning() {
        Log.i(TAG, "All permissions are granted, request media projection")
        projectionActivityResult.showMediaProjectionWarning(this, viewModel.isEntireScreenCaptureForced()) {
            finish()
        }
    }
}

private const val TAG = "QSTileLauncherActivity"