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

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.clicks.ScenarioViewModel
import com.buzbuz.smartautoclicker.clicks.database.ScenarioEntity

import kotlinx.android.synthetic.main.dialog_add_scenario.edit_name
import kotlinx.android.synthetic.main.fragment_scenarios.add
import kotlinx.android.synthetic.main.merge_loadable_list.empty
import kotlinx.android.synthetic.main.merge_loadable_list.list
import kotlinx.android.synthetic.main.merge_loadable_list.loading

/**
 * Fragment displaying the list of click scenario and the creation dialog.
 * If the list is empty, it will hide the list and displays the empty list view.
 */
class ScenarioListFragment : Fragment() {

    /**
     * Listener interface upon user clicks on a scenario displayed by this fragment.
     * Must be implemented by the [androidx.fragment.app.FragmentActivity] attached to this fragment.
     */
    interface OnScenarioClickedListener {

        /**
         * The user has clicked on a scenario.
         * @param scenario the clicked scenario.
         */
        fun onClicked(scenario: ScenarioEntity)
    }

    /** ViewModel providing the click scenarios data to the UI. */
    private lateinit var scenarioViewModel: ScenarioViewModel
    /** Adapter displaying the click scenarios as a list. */
    private lateinit var scenariosAdapter: ScenarioAdapter

    /** The create new scenario dialog. Null if not displayed. */
    private var createDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_scenarios, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scenarioViewModel = ViewModelProvider(this).get(ScenarioViewModel::class.java)
        scenariosAdapter = ScenarioAdapter(
            (activity as OnScenarioClickedListener)::onClicked,
            scenarioViewModel::deleteScenario
        )
        scenarioViewModel.clickScenario.observe(this, scenariosObserver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        list.adapter = scenariosAdapter
        empty.setText(R.string.no_scenarios)
        add.setOnClickListener { onAddClicked() }
    }

    override fun onDestroy() {
        super.onDestroy()
        scenarioViewModel.clickScenario.removeObservers(this)
    }

    /**
     * Called when the user clicks on the add scenario button.
     * Create and show the [createDialog].
     */
    @SuppressLint("InflateParams") // Dialog views have no parent at inflation time
    private fun onAddClicked() {
        val titleView = context!!.getSystemService(LayoutInflater::class.java)!!
            .inflate(R.layout.view_dialog_title, null)
        titleView.findViewById<TextView>(R.id.title).setText(R.string.dialog_add_scenario_title)

        createDialog = AlertDialog.Builder(context!!)
            .setCustomTitle(titleView)
            .setView(R.layout.dialog_add_scenario)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int -> onCreateScenarioClicked() }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        createDialog!!.show()
    }

    /** Called when the user clicks the create button in the [createDialog]. */
    private fun onCreateScenarioClicked() {
        val name = createDialog!!.edit_name.text.toString()
        createDialog?.let {
            scenarioViewModel.createScenario(name)
        }
    }

    /**
     * Observer upon the list of click scenarios.
     * Will update the list/empty view according to the current click scenarios
     */
    private val scenariosObserver: Observer<List<ScenarioEntity>> = Observer { scenarios ->
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