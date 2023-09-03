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
package com.buzbuz.smartautoclicker.activity

import android.content.DialogInterface
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.activity.PermissionsDialogFragment.Companion.FRAGMENT_TAG_PERMISSION_DIALOG
import com.buzbuz.smartautoclicker.feature.backup.ui.BackupDialogFragment.Companion.FRAGMENT_TAG_BACKUP_DIALOG
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.databinding.DialogEditBinding
import com.buzbuz.smartautoclicker.databinding.FragmentScenariosBinding
import com.buzbuz.smartautoclicker.feature.backup.ui.BackupDialogFragment
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_DISABLED_ITEM
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_ENABLED_ITEM

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable

import kotlinx.coroutines.launch

/**
 * Fragment displaying the list of click scenario and the creation dialog.
 * If the list is empty, it will hide the list and displays the empty list view.
 */
class ScenarioListFragment : Fragment(), PermissionsDialogFragment.PermissionDialogListener {

    /** ViewModel providing the click scenarios data to the UI. */
    private val scenarioViewModel: ScenarioViewModel by activityViewModels()

    /** ViewBinding containing the views for this fragment. */
    private lateinit var viewBinding: FragmentScenariosBinding
    /** Adapter displaying the click scenarios as a list. */
    private lateinit var scenariosAdapter: ScenarioAdapter
    /** The result launcher for the projection permission dialog. */
    private lateinit var projectionActivityResult: ActivityResultLauncher<Intent>

    /** The current dialog being displayed. Null if not displayed. */
    private var dialog: AlertDialog? = null
    /** Scenario clicked by the user. */
    private var requestedScenario: Scenario? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentScenariosBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scenariosAdapter = ScenarioAdapter(
            bitmapProvider = scenarioViewModel::getConditionBitmap,
            startScenarioListener = ::onStartClicked,
            deleteScenarioListener = ::onDeleteClicked,
            exportClickListener = ::onExportClicked,
        )

        projectionActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != AppCompatActivity.RESULT_OK) {
                Toast.makeText(requireContext(), R.string.toast_denied_screen_sharing_permission, Toast.LENGTH_SHORT).show()
            } else {
                requestedScenario?.let { scenario ->
                    val started = scenarioViewModel.loadScenario(requireContext(), result.resultCode, result.data!!, scenario)
                    if (started) activity?.finish()
                    else Toast.makeText(requireContext(), R.string.toast_denied_foreground_permission, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.apply {
            list.adapter = scenariosAdapter

            emptyCreateButton.setOnClickListener { onCreateClicked() }

            appBarLayout.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(context)
            topAppBar.apply {
                setOnMenuItemClickListener { onMenuItemSelected(it) }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { scenarioViewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    override fun onPermissionsGranted() {
        showMediaProjectionWarning()
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        val uiState = scenarioViewModel.uiState.value ?: return false

        when (item.itemId) {
            R.id.action_export -> when {
                !uiState.isProModePurchased -> scenarioViewModel.onExportClickedWithoutProMode(requireContext())
                uiState.type == ScenarioListFragmentUiState.Type.EXPORT -> showBackupDialog(
                    isImport = false,
                    scenariosToBackup = scenarioViewModel.getScenariosSelectedForBackup(),
                )
                else -> scenarioViewModel.setUiState(ScenarioListFragmentUiState.Type.EXPORT)
            }

            R.id.action_import -> when {
                !uiState.isProModePurchased -> scenarioViewModel.onImportClickedWithoutProMode(requireContext())
                else -> showBackupDialog(true)
            }

            R.id.action_cancel -> scenarioViewModel.setUiState(ScenarioListFragmentUiState.Type.SELECTION)
            R.id.action_search -> scenarioViewModel.setUiState(ScenarioListFragmentUiState.Type.SEARCH)
            R.id.action_select_all -> scenarioViewModel.toggleAllScenarioSelectionForBackup()
            else -> return false
        }

        return true
    }

    private fun updateUiState(uiState: ScenarioListFragmentUiState?) {
        uiState ?: return

        updateMenu(uiState.menuUiState)
        updateScenarioList(uiState.listContent)
        updateScenarioLimitationVisibility(uiState.isScenarioLimitReached)
    }

    /**
     * Update the display of the action menu.
     * @param menuState the new ui state for the menu.
     */
    private fun updateMenu(menuState: ScenarioListFragmentUiState.Menu) {
        viewBinding.topAppBar.menu.apply {
            findItem(R.id.action_select_all)?.bind(menuState.selectAllItemState)
            findItem(R.id.action_cancel)?.bind(menuState.cancelItemState)
            findItem(R.id.action_import)?.bind(menuState.importItemState)
            findItem(R.id.action_export)?.bind(menuState.exportItemState)
            findItem(R.id.action_search)?.apply {
                bind(menuState.searchItemState)
                actionView?.let { actionView ->
                    (actionView as SearchView).apply {
                        setIconifiedByDefault(true)
                        setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String?) = false
                            override fun onQueryTextChange(newText: String?): Boolean {
                                scenarioViewModel.updateSearchQuery(newText)
                                return true
                            }
                        })
                        addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                            override fun onViewDetachedFromWindow(arg0: View) {
                                scenarioViewModel.updateSearchQuery(null)
                                scenarioViewModel.setUiState(ScenarioListFragmentUiState.Type.SELECTION)
                            }

                            override fun onViewAttachedToWindow(arg0: View) {}
                        })
                    }
                }
            }
        }
    }

    /**
     * Observer upon the list of click scenarios.
     * Will update the list/empty view according to the current click scenarios
     */
    private fun updateScenarioList(scenarios: List<ScenarioListFragmentUiState.ScenarioListItem>) {
        viewBinding.apply {
            if (scenarios.isEmpty()) {
                list.visibility = View.GONE
                add.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            } else {
                list.visibility = View.VISIBLE
                add.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE
            }
        }

        scenariosAdapter.submitList(scenarios)
    }

    private fun updateScenarioLimitationVisibility(isVisible: Boolean) {
        viewBinding.add.apply {
            if (isVisible){
                alpha = ALPHA_DISABLED_ITEM
                setOnClickListener { scenarioViewModel.onScenarioCountReachedAddCopyClicked(requireContext()) }
            } else {
                alpha = ALPHA_ENABLED_ITEM
                setOnClickListener { onCreateClicked() }
            }
        }
    }

    /**
     * Show an AlertDialog from this fragment.
     * This method will ensure that only one dialog is shown at the same time.
     *
     * @param newDialog the new dialog to be shown.
     */
    private fun showDialog(newDialog: AlertDialog) {
        dialog.let {
            Log.w(TAG, "Requesting show dialog while another one is one screen.")
            it?.dismiss()
        }

        dialog = newDialog
        newDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        newDialog.setOnDismissListener { dialog = null }
        newDialog.show()
    }

    /**
     * Called when the user clicks on a scenario.
     * @param scenario the scenario clicked.
     */
    private fun onStartClicked(scenario: Scenario) {
        requestedScenario = scenario

        if (!scenarioViewModel.arePermissionsGranted()) {
            activity?.let {
                PermissionsDialogFragment.newInstance().show(it.supportFragmentManager, FRAGMENT_TAG_PERMISSION_DIALOG)
            }
            return
        }

        showMediaProjectionWarning()
    }

    /** Show the media projection start warning. */
    private fun showMediaProjectionWarning() {
        getSystemService(requireContext(), MediaProjectionManager::class.java)?.let { projectionManager ->
            // The component name defined in com.android.internal.R.string.config_mediaProjectionPermissionDialogComponent
            // specifying the dialog to start to request the permission is invalid on some devices (Chinese Honor6X Android 10).
            // There is nothing to do in those cases, the app can't be used.
            try {
                projectionActivityResult.launch(projectionManager.createScreenCaptureIntent())
            } catch (npe: NullPointerException) {
                showUnsupportedDeviceDialog()
            }
        }
    }

    /**
     * Called when the user clicks on the export button of a scenario.
     *
     * @param scenario the scenario clicked.
     */
    private fun onExportClicked(scenario: Scenario) {
        scenarioViewModel.toggleScenarioSelectionForBackup(scenario)
    }

    /**
     * Called when the user clicks on the add scenario button.
     * Create and show the [dialog]. Upon Ok press, creates the scenario.
     */
    private fun onCreateClicked() {
        val dialogViewBinding = DialogEditBinding.inflate(LayoutInflater.from(context))
        showDialog(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_add_scenario)
            .setView(dialogViewBinding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                scenarioViewModel.createScenario(requireContext(), dialogViewBinding.textField.text.toString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create())
    }

    /**
     * Called when the user clicks on the delete button of a scenario.
     * Create and show the [dialog]. Upon Ok press, delete the scenario.
     *
     * @param scenario the scenario to delete.
     */
    private fun onDeleteClicked(scenario: Scenario) {
        showDialog(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_delete_scenario)
            .setMessage(resources.getString(R.string.message_delete_scenario, scenario.name))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                scenarioViewModel.deleteScenario(scenario)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create())
    }

    /**
     * Shows the backup dialog fragment.
     *
     * @param isImport true to display in import mode, false for export.
     * @param scenariosToBackup the list of identifiers for the scenarios to export. Null if isImport = true.
     */
    private fun showBackupDialog(isImport: Boolean, scenariosToBackup: Collection<Long>? = null) {
        activity?.let {
            BackupDialogFragment
                .newInstance(isImport, scenariosToBackup)
                .show(it.supportFragmentManager, FRAGMENT_TAG_BACKUP_DIALOG)
        }
        scenarioViewModel.setUiState(ScenarioListFragmentUiState.Type.SELECTION)
    }

    /**
     * Some devices messes up too much with Android.
     * Display a dialog in those cases and stop the application.
     */
    private fun showUnsupportedDeviceDialog() {
        showDialog(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.message_error_screen_capture_permission_dialog_not_found)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                activity?.finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create())
    }
}

private fun MenuItem.bind(state: ScenarioListFragmentUiState.Menu.Item) {
    isVisible = state.visible
    isEnabled = state.enabled
    icon = icon?.mutate()?.apply {
        alpha = state.iconAlpha
    }
}

/** Tag for scenario list fragment. */
const val FRAGMENT_TAG_SCENARIO_LIST = "ScenarioList"
/** Tag for logs. */
private const val TAG = "ScenarioListFragment"