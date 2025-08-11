
package com.buzbuz.smartautoclicker.scenarios

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge

import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.scenarios.list.ScenarioListFragment
import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioListUiState
import com.buzbuz.smartautoclicker.core.display.recorder.showMediaProjectionWarning
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.scenarios.viewmodel.ScenarioViewModel

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point activity for the application.
 * Shown when the user clicks on the launcher icon for the application, this activity will displays the list of
 * available scenarios, if any.
 */
@AndroidEntryPoint
class ScenarioActivity : AppCompatActivity(), ScenarioListFragment.Listener {

    /** ViewModel providing the click scenarios data to the UI. */
    private val scenarioViewModel: ScenarioViewModel by viewModels()

    /** The result launcher for the projection permission dialog. */
    private lateinit var projectionActivityResult: ActivityResultLauncher<Intent>

    /** Scenario clicked by the user. */
    private var requestedItem: ScenarioListUiState.Item.ScenarioItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scenario)

        scenarioViewModel.stopScenario()

        projectionActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                Toast.makeText(this, R.string.toast_denied_screen_sharing_permission, Toast.LENGTH_SHORT).show()
            } else {
                (requestedItem?.scenario as? Scenario)?.let { scenario ->
                    startSmartScenario(result, scenario)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun startScenario(item: ScenarioListUiState.Item.ScenarioItem) {
        requestedItem = item

        scenarioViewModel.startPermissionFlowIfNeeded(
            activity = this,
            onAllGranted = ::onMandatoryPermissionsGranted,
        )
    }

    private fun onMandatoryPermissionsGranted() {
        scenarioViewModel.startTroubleshootingFlowIfNeeded(this) {
            when (val scenario = requestedItem?.scenario) {
                is Scenario -> projectionActivityResult.showMediaProjectionWarning(
                    context = this,
                    forceEntireScreen = scenarioViewModel.isEntireScreenCaptureForced(),
                    onError = { showUnsupportedDeviceDialog() },
                )
            }
        }
    }

    /**
     * Some devices messes up too much with Android.
     * Display a dialog in those cases and stop the application.
     */
    private fun showUnsupportedDeviceDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.message_error_screen_capture_permission_dialog_not_found)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    private fun startSmartScenario(result: ActivityResult, scenario: Scenario) {
        handleScenarioStartResult(scenarioViewModel.loadSmartScenario(
            context = this,
            resultCode = result.resultCode,
            data = result.data!!,
            scenario = scenario,
        ))
    }

    private fun handleScenarioStartResult(result: Boolean) {
        if (result) finish()
        else Toast.makeText(this, R.string.toast_denied_foreground_permission, Toast.LENGTH_SHORT).show()
    }
}
