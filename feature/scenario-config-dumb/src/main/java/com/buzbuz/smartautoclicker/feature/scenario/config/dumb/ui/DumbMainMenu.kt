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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.utils.AnimatedStatesImageButtonController
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.OverlayDumbMainMenuBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.DumbActionCreator
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.DumbActionUiFlowListener
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.startDumbActionCreationUiFlow
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.DumbScenarioDialog

import kotlinx.coroutines.launch

class DumbMainMenu(
    private val dumbScenarioId: Identifier,
    private val onStopClicked: () -> Unit,
) : OverlayMenu() {

    /** The view model for this menu. */
    private val viewModel: DumbMainMenuModel by viewModels()

    /** View binding for the content of the overlay. */
    private lateinit var viewBinding: OverlayDumbMainMenuBinding
    /** Controls the animations of the play/pause button. */
    private lateinit var playPauseButtonController: AnimatedStatesImageButtonController

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isPlaying.collect(::updateMenuPlayingState) }
                launch { viewModel.canPlay.collect(::updatePlayPauseButtonEnabledState) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        playPauseButtonController = AnimatedStatesImageButtonController(
            context = context,
            state1StaticRes = R.drawable.ic_play_arrow,
            state2StaticRes = R.drawable.ic_pause,
            state1to2AnimationRes = R.drawable.anim_play_pause,
            state2to1AnimationRes = R.drawable.anim_pause_play,
        )

        viewBinding = OverlayDumbMainMenuBinding.inflate(layoutInflater).apply {
            playPauseButtonController.attachView(btnPlay)
            btnAdd.setOnClickListener { debounceUserInteraction { onAddButtonClicked() } }
            btnActionList.setOnClickListener { debounceUserInteraction { onDumbScenarioConfigClicked() } }
        }

        return viewBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        playPauseButtonController.detachView()
        viewModel.stopEdition()
    }

    /** Refresh the play menu item according to the scenario state. */
    private fun updatePlayPauseButtonEnabledState(canStartDetection: Boolean) =
        setMenuItemViewEnabled(viewBinding.btnPlay, canStartDetection)

    private fun updateMenuPlayingState(isPlaying: Boolean) {
        val currentState = viewBinding.btnPlay.tag
        if (currentState == isPlaying) return

        viewBinding.btnPlay.tag = isPlaying
        if (isPlaying) {
            if (currentState == null) {
                playPauseButtonController.toState2(false)
            } else {
                animateLayoutChanges {
                    setMenuItemVisibility(viewBinding.btnStop, false)
                    setMenuItemVisibility(viewBinding.btnAdd, false)
                    setMenuItemVisibility(viewBinding.btnShowActions, false)
                    setMenuItemVisibility(viewBinding.btnActionList, false)
                    playPauseButtonController.toState2(true)
                }
            }
        } else {
            if (currentState == null) {
                playPauseButtonController.toState1(false)
            } else {
                animateLayoutChanges {
                    setMenuItemVisibility(viewBinding.btnStop, true)
                    setMenuItemVisibility(viewBinding.btnAdd, true)
                    setMenuItemVisibility(viewBinding.btnShowActions, true)
                    setMenuItemVisibility(viewBinding.btnActionList, true)
                    playPauseButtonController.toState1(true)
                }
            }
        }
    }

    override fun onMenuItemClicked(viewId: Int) {
        debounceUserInteraction {
            when (viewId) {
                R.id.btn_play -> viewModel.toggleScenarioPlay()
                R.id.btn_stop -> onStopClicked()
                R.id.btn_add -> onAddButtonClicked()
                R.id.btn_show_actions -> Unit
                R.id.btn_action_list -> onDumbScenarioConfigClicked()
            }
        }
    }

    private fun onAddButtonClicked() {
        viewModel.startEdition(dumbScenarioId) {
            OverlayManager.getInstance(context).startDumbActionCreationUiFlow(
                context = context,
                creator = DumbActionCreator(
                    createNewDumbClick = viewModel::createNewDumbClick,
                    createNewDumbSwipe = viewModel::createNewDumbSwipe,
                    createNewDumbPause = viewModel::createNewDumbPause,
                ),
                listener = DumbActionUiFlowListener(
                    onDumbActionSaved = viewModel::addNewDumbAction,
                    onDumbActionDeleted = viewModel::deleteDumbAction,
                    onDumbActionCreationCancelled = viewModel::stopEdition,
                )
            )
        }
    }

    private fun onDumbScenarioConfigClicked() {
        viewModel.startEdition(dumbScenarioId) {
            OverlayManager.getInstance(context).navigateTo(
                context = context,
                newOverlay = DumbScenarioDialog(
                    onConfigSaved = viewModel::saveEditions,
                    onConfigDiscarded = viewModel::stopEdition,
                ),
                hideCurrent = true,
            )
        }
    }
}