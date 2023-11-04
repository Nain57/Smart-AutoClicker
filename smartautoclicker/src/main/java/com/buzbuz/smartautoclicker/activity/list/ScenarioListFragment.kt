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
package com.buzbuz.smartautoclicker.activity.list

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.WindowManager

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.activity.creation.ScenarioCreationDialog
import com.buzbuz.smartautoclicker.feature.backup.ui.BackupDialogFragment.Companion.FRAGMENT_TAG_BACKUP_DIALOG
import com.buzbuz.smartautoclicker.databinding.FragmentScenariosBinding
import com.buzbuz.smartautoclicker.feature.backup.ui.BackupDialogFragment

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable

import kotlinx.coroutines.launch

/**
 * Fragment displaying the list of click scenario and the creation dialog.
 * If the list is empty, it will hide the list and displays the empty list view.
 */
class ScenarioListFragment : Fragment() {

    interface Listener {
        fun startScenario(item: ScenarioListUiState.Item)
    }

    /** ViewModel providing the scenarios data to the UI. */
    private val scenarioListViewModel: ScenarioListViewModel by viewModels()

    /** ViewBinding containing the views for this fragment. */
    private lateinit var viewBinding: FragmentScenariosBinding
    /** Adapter displaying the click scenarios as a list. */
    private lateinit var scenariosAdapter: ScenarioAdapter


    /** The current dialog being displayed. Null if not displayed. */
    private var dialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentScenariosBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scenariosAdapter = ScenarioAdapter(
            bitmapProvider = scenarioListViewModel::getConditionBitmap,
            startScenarioListener = ::onStartClicked,
            deleteScenarioListener = ::onDeleteClicked,
            exportClickListener = ::onExportClicked,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.apply {
            list.adapter = scenariosAdapter

            emptyCreateButton.setOnClickListener { onCreateClicked() }
            add.setOnClickListener { onCreateClicked() }

            appBarLayout.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(context)
            topAppBar.apply {
                setOnMenuItemClickListener { onMenuItemSelected(it) }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { scenarioListViewModel.uiState.collect(::updateUiState) }
            }
        }
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        val uiState = scenarioListViewModel.uiState.value ?: return false

        when (item.itemId) {
            R.id.action_export -> when {
                !uiState.isProModePurchased -> scenarioListViewModel.onExportClickedWithoutProMode(requireContext())
                uiState.type == ScenarioListUiState.Type.EXPORT -> showBackupDialog(
                    isImport = false,
                    smartScenariosToBackup = scenarioListViewModel.getSmartScenariosSelectedForBackup(),
                    dumbScenariosToBackup = scenarioListViewModel.getDumbScenariosSelectedForBackup(),
                )
                else -> scenarioListViewModel.setUiState(ScenarioListUiState.Type.EXPORT)
            }

            R.id.action_import -> when {
                !uiState.isProModePurchased -> scenarioListViewModel.onImportClickedWithoutProMode(requireContext())
                else -> showBackupDialog(true)
            }

            R.id.action_cancel -> scenarioListViewModel.setUiState(ScenarioListUiState.Type.SELECTION)
            R.id.action_search -> scenarioListViewModel.setUiState(ScenarioListUiState.Type.SEARCH)
            R.id.action_select_all -> scenarioListViewModel.toggleAllScenarioSelectionForBackup()
            else -> return false
        }

        return true
    }

    private fun updateUiState(uiState: ScenarioListUiState?) {
        uiState ?: return

        updateMenu(uiState.menuUiState)
        updateScenarioList(uiState.listContent)
    }

    /**
     * Update the display of the action menu.
     * @param menuState the new ui state for the menu.
     */
    private fun updateMenu(menuState: ScenarioListUiState.Menu) {
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
                                scenarioListViewModel.updateSearchQuery(newText)
                                return true
                            }
                        })
                        addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                            override fun onViewDetachedFromWindow(arg0: View) {
                                scenarioListViewModel.updateSearchQuery(null)
                                scenarioListViewModel.setUiState(ScenarioListUiState.Type.SELECTION)
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
    private fun updateScenarioList(scenarios: List<ScenarioListUiState.Item>) {
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
    private fun onStartClicked(scenario: ScenarioListUiState.Item) {
        (requireActivity() as? Listener)?.startScenario(scenario)
    }

    /**
     * Called when the user clicks on the export button of a scenario.
     *
     * @param item the scenario clicked.
     */
    private fun onExportClicked(item: ScenarioListUiState.Item) {
        scenarioListViewModel.toggleScenarioSelectionForBackup(item)
    }

    /**
     * Called when the user clicks on the add scenario button.
     * Create and show the [dialog]. Upon Ok press, creates the scenario.
     */
    private fun onCreateClicked() {
        ScenarioCreationDialog()
            .show(requireActivity().supportFragmentManager, ScenarioCreationDialog.FRAGMENT_TAG)
    }

    /**
     * Called when the user clicks on the delete button of a scenario.
     * Create and show the [dialog]. Upon Ok press, delete the scenario.
     *
     * @param item the scenario to delete.
     */
    private fun onDeleteClicked(item: ScenarioListUiState.Item) {
        showDialog(MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_delete_scenario)
            .setMessage(resources.getString(R.string.message_delete_scenario, item.displayName))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                scenarioListViewModel.deleteScenario(item)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create())
    }

    /**
     * Shows the backup dialog fragment.
     *
     * @param isImport true to display in import mode, false for export.
     * @param smartScenariosToBackup the list of identifiers for the smart scenarios to export. Null if isImport = true.
     * @param dumbScenariosToBackup the list of identifiers for the dumb scenarios to export. Null if isImport = true.
     *
     */
    private fun showBackupDialog(
        isImport: Boolean,
        smartScenariosToBackup: Collection<Long>? = null,
        dumbScenariosToBackup: Collection<Long>? = null,
    ) {
        activity?.let {
            BackupDialogFragment
                .newInstance(isImport, smartScenariosToBackup, dumbScenariosToBackup)
                .show(it.supportFragmentManager, FRAGMENT_TAG_BACKUP_DIALOG)
        }
        scenarioListViewModel.setUiState(ScenarioListUiState.Type.SELECTION)
    }
}

private fun MenuItem.bind(state: ScenarioListUiState.Menu.Item) {
    isVisible = state.visible
    isEnabled = state.enabled
    icon = icon?.mutate()?.apply {
        alpha = state.iconAlpha
    }
}

/** Tag for logs. */
private const val TAG = "ScenarioListFragment"