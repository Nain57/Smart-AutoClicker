/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.ui.activity

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.setCustomTitle
import com.buzbuz.smartautoclicker.database.ClickScenario

import kotlinx.android.synthetic.main.dialog_edit.edit_name
import kotlinx.android.synthetic.main.fragment_scenarios.add
import kotlinx.android.synthetic.main.merge_loadable_list.empty
import kotlinx.android.synthetic.main.merge_loadable_list.list
import kotlinx.android.synthetic.main.merge_loadable_list.loading

/**
 * Fragment displaying the list of click scenario and the creation dialog.
 * If the list is empty, it will hide the list and displays the empty list view.
 */
class ScenarioListFragment : Fragment() {

    private companion object {
        /** Tag for logs. */
        private const val TAG = "ScenarioListFragment"
    }
    /**
     * Listener interface upon user clicks on a scenario displayed by this fragment.
     * Must be implemented by the [androidx.fragment.app.FragmentActivity] attached to this fragment.
     */
    interface OnScenarioClickedListener {

        /**
         * The user has clicked on a scenario.
         * @param scenario the clicked scenario.
         */
        fun onClicked(scenario: ClickScenario)
    }

    /** ViewModel providing the click scenarios data to the UI. */
    private val scenarioViewModel: ScenarioViewModel by activityViewModels()
    /** Adapter displaying the click scenarios as a list. */
    private lateinit var scenariosAdapter: ScenarioAdapter

    /** The current dialog being displayed. Null if not displayed. */
    private var dialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_scenarios, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scenariosAdapter = ScenarioAdapter().apply {
            startScenarioListener = (activity as OnScenarioClickedListener)::onClicked
            deleteScenarioListener = ::onDeleteClicked
            editClickListener = ::onRenameClicked
        }
        scenarioViewModel.clickScenario.observe(this, scenariosObserver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        list.adapter = scenariosAdapter
        empty.setText(R.string.no_scenarios)
        add.setOnClickListener { onCreateClicked() }
    }

    override fun onDestroy() {
        super.onDestroy()
        scenarioViewModel.clickScenario.removeObservers(this)
    }

    /**
     * Show an AlertDialog from this fragment.
     * This method will ensure that only one dialog is shown at the same time.
     *
     * @param newDialog the new dialog to be shown.
     */
    private fun showDialog(newDialog: AlertDialog) {
        dialog?.let {
            Log.w(TAG, "Requesting show dialog while another one is one screen.")
            it.dismiss()
            dialog = null
        }

        dialog = newDialog
        newDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        newDialog.setOnDismissListener { dialog = null }
        newDialog.show()
    }

    /**
     * Called when the user clicks on the add scenario button.
     * Create and show the [dialog]. Upon Ok press, creates the scenario.
     */
    private fun onCreateClicked() {
        showDialog(AlertDialog.Builder(requireContext())
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_add_scenario_title)
            .setView(R.layout.dialog_edit)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                dialog?.let {
                    scenarioViewModel.createScenario(it.edit_name.text.toString())
                }
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
    private fun onDeleteClicked(scenario: ClickScenario) {
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
    private fun onRenameClicked(scenario: ClickScenario) {
        val dialog = AlertDialog.Builder(requireContext())
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_rename_scenario_title)
            .setView(R.layout.dialog_edit)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                dialog?.let {
                    scenarioViewModel.renameScenario(scenario, it.edit_name.text.toString())
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.edit_name.apply {
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
    private val scenariosObserver: Observer<List<ClickScenario>> = Observer { scenarios ->
        loading.visibility = View.GONE
        if (scenarios.isNullOrEmpty()) {
            list.visibility = View.GONE
            empty.visibility = View.VISIBLE
        } else {
            list.visibility = View.VISIBLE
            empty.visibility = View.GONE
        }

        scenariosAdapter.scenarios = scenarios
    }
}