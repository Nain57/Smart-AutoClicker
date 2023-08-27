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

import android.os.Bundle
import android.view.WindowManager

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.tutorial.R

import kotlinx.coroutines.launch


class TutorialActivity : AppCompatActivity() {

    private val viewModel: TutorialViewModel by viewModels()
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupActionBar()

        navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
            .navController

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.shouldBeStopped.collect { shouldBeStopped ->
                        if (shouldBeStopped) finish()
                    }
                }

                launch {
                    viewModel.onFloatingUiVisibilityStep.collect { newVisibility ->
                        setFloatingUiVisibility(newVisibility)
                        viewModel.validateFloatingUiVisibilityStep()
                    }
                }
            }
        }

        viewModel.startTutorialMode()
    }

    override fun onDestroy() {
        viewModel.stopTutorialMode()
        super.onDestroy()
        setFloatingUiVisibility(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (!navController.navigateUp()) finish()
        return true
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.topAppBar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setFloatingUiVisibility(isVisible: Boolean) {
        OverlayManager.getInstance(this).apply {
            if (isVisible) restoreVisibility()
            else hideAll()
        }
    }
}
