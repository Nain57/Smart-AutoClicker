/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.activity

import android.content.DialogInterface
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
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
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.activity.PermissionsDialogFragment.Companion.FRAGMENT_TAG_PERMISSION_DIALOG
import com.buzbuz.smartautoclicker.activity.backup.BackupDialogFragment
import com.buzbuz.smartautoclicker.activity.backup.BackupDialogFragment.Companion.FRAGMENT_TAG_BACKUP_DIALOG
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.databinding.DialogEditBinding
import com.buzbuz.smartautoclicker.databinding.FragmentScenariosBinding
import com.buzbuz.smartautoclicker.databinding.MergeLoadableListBinding

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
    /** ViewBinding containing the views for the loadable list merge layout. */
    private lateinit var listBinding: MergeLoadableListBinding
    /** Adapter displaying the click scenarios as a list. */
    private lateinit var scenariosAdapter: ScenarioAdapter
    /** The action menu for this fragment. */
    private lateinit var menu: Menu
    /** The result launcher for the projection permission dialog. */
    private lateinit var projectionActivityResult: ActivityResultLauncher<Intent>

    /** The current dialog being displayed. Null if not displayed. */
    private var dialog: AlertDialog? = null
    /** Scenario clicked by the user. */
    private var requestedScenario: Scenario? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentScenariosBinding.inflate(inflater, container, false)
        listBinding = MergeLoadableListBinding.bind(viewBinding.root)
        return viewBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        scenariosAdapter = ScenarioAdapter(
            startScenarioListener = ::onStartClicked,
            deleteScenarioListener = ::onDeleteClicked,
            editClickListener = ::onRenameClicked,
            exportClickListener = ::onExportClicked,
        )

        projectionActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != AppCompatActivity.RESULT_OK) {
                Toast.makeText(requireContext(), "User denied screen sharing permission", Toast.LENGTH_SHORT).show()
            } else {
                requestedScenario?.let { scenario ->
                    scenarioViewModel.loadScenario(result.resultCode, result.data!!, scenario)
                    activity?.finish()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu
        inflater.inflate(R.menu.menu_scenario_fragment, menu)

        (menu.findItem(R.id.action_search).actionView as SearchView).apply {
            setIconifiedByDefault(true)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    scenarioViewModel.updateSearchQuery(newText)
                    return true
                }
            })
            addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                override fun onViewDetachedFromWindow(arg0: View?) {
                    scenarioViewModel.updateSearchQuery(null)
                    scenarioViewModel.setMenuState(MenuState.SELECTION)
                }

                override fun onViewAttachedToWindow(arg0: View?) {}
            })
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                scenarioViewModel.menuUiState.collect { menuState ->
                    updateMenu(menuState)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listBinding.list.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = scenariosAdapter
        }

        listBinding.empty.setText(R.string.no_scenarios)
        viewBinding.add.setOnClickListener { onCreateClicked() }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                scenarioViewModel.scenarioList.collect {
                    onNewScenarioList(it)
                }
            }
        }
    }

    override fun onPermissionsGranted() {
        showMediaProjectionWarning()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_export ->
                if (scenarioViewModel.menuState.value == MenuState.EXPORT) {
                    showBackupDialog(false, scenarioViewModel.getScenariosSelectedForBackup())
                } else {
                    scenarioViewModel.setMenuState(MenuState.EXPORT)
                }
            R.id.action_import -> showBackupDialog(true)
            R.id.action_cancel -> scenarioViewModel.setMenuState(MenuState.SELECTION)
            R.id.action_search -> scenarioViewModel.setMenuState(MenuState.SEARCH)
            R.id.action_select_all -> scenarioViewModel.toggleAllScenarioSelectionForBackup()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    /**
     * Update the display of the action menu.
     * @param menuState the new ui state for the menu.
     */
    private fun updateMenu(menuState: MenuUiState) {
        menu.apply {
            findItem(R.id.action_search).isVisible = menuState.searchVisibility
            findItem(R.id.action_import).isVisible = menuState.importBackupVisibility
            findItem(R.id.action_cancel).isVisible = menuState.cancelVisibility
            findItem(R.id.action_select_all).isVisible = menuState.selectAllVisibility
            findItem(R.id.action_export).apply {
                isVisible = menuState.createBackupVisibility
                isEnabled = menuState.createBackupEnabled
                icon = icon?.mutate()?.apply {
                    alpha = menuState.createBackupAlpha
                }
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
            projectionActivityResult.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    /**
     * Called when the user clicks on the export button of a scenario.
     *
     * @param scenario the scenario clicked.
     */
    private fun onExportClicked(scenario: Scenario) {
        scenarioViewModel.toggleScenarioSelectionForBackup(scenario.id)
    }

    /**
     * Called when the user clicks on the add scenario button.
     * Create and show the [dialog]. Upon Ok press, creates the scenario.
     */
    private fun onCreateClicked() {
        val dialogViewBinding = DialogEditBinding.inflate(LayoutInflater.from(context))
        showDialog(AlertDialog.Builder(requireContext())
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_add_scenario_title)
            .setView(dialogViewBinding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                scenarioViewModel.createScenario(requireContext(), dialogViewBinding.editName.text.toString())
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
        showDialog(AlertDialog.Builder(requireContext())
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_delete_scenario_title)
            .setMessage(resources.getString(R.string.dialog_delete_scenario_message, scenario.name))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                scenarioViewModel.deleteScenario(scenario)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create())
    }

    /**
     * Called when the user clicks on the rename button of a scenario.
     * Create and show the [dialog]. Upon Ok press, rename the scenario.
     *
     * @param scenario the scenario to rename.
     */
    private fun onRenameClicked(scenario: Scenario) {
        val dialogViewBinding = DialogEditBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(requireContext())
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_rename_scenario_title)
            .setView(dialogViewBinding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                scenarioViewModel.renameScenario(scenario, dialogViewBinding.editName.text.toString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialogViewBinding.editName.apply {
                setText(scenario.name)
                setSelection(0, scenario.name.length)
            }
        }
        showDialog(dialog)
    }

    /**
     * Observer upon the list of click scenarios.
     * Will update the list/empty view according to the current click scenarios
     */
    private fun onNewScenarioList(scenarios: List<ScenarioItem>) {
        listBinding.apply {
            loading.visibility = View.GONE
            if (scenarios.isEmpty()) {
                list.visibility = View.GONE
                empty.visibility = View.VISIBLE
            } else {
                list.visibility = View.VISIBLE
                empty.visibility = View.GONE
            }
        }

        scenariosAdapter.submitList(scenarios)
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
    }
}

/** Tag for scenario list fragment. */
const val FRAGMENT_TAG_SCENARIO_LIST = "ScenarioList"
/** Tag for logs. */
private const val TAG = "ScenarioListFragment"