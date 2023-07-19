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
package com.buzbuz.smartautoclicker.feature.tutorial.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

import com.buzbuz.smartautoclicker.feature.tutorial.R


class TutorialActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_STARTING_PAGE = "extra_starting_page"

        fun getStartIntent(context: Context, startingPage: TutorialStartingPage): Intent =
            Intent(context, TutorialActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_STARTING_PAGE, startingPage.name)

        private fun Intent.getStartingPage(): TutorialStartingPage? {
            val startingPageString = getStringExtra(EXTRA_STARTING_PAGE) ?: return null

            return try {
                TutorialStartingPage.valueOf(startingPageString)
            } catch (ex: IllegalArgumentException) {
                null
            }
        }
    }

    private val viewModel: TutorialViewModel by viewModels()

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        setupActionBar()
        setupNavGraph(intent.getStartingPage())
    }

    override fun onStart() {
        super.onStart()
        viewModel.startTutorialMode()
    }

    override fun onStop() {
        viewModel.stopTutorialMode()
        super.onStop()
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.topAppBar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupNavGraph(startingPage: TutorialStartingPage?) {
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.let { fragment ->
            val navHostFragment = (fragment as NavHostFragment)

            navController = navHostFragment.navController
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)

            when (startingPage) {
                TutorialStartingPage.INTRO -> navGraph.setStartDestination(R.id.fragment_intro)
                TutorialStartingPage.LIST -> navGraph.setStartDestination(R.id.fragment_tutorial_list)
                null -> finish()
            }

            navController.graph = navGraph
        } ?: finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (!navController.navigateUp()) finish()
        return true
    }
}

enum class TutorialStartingPage {
    INTRO,
    LIST,
}