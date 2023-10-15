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

import android.graphics.Point
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.MultiChoiceDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.ClickSwipeSelectorMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.CoordinatesSelector
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.utils.AnimatedStatesImageButtonController
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.OverlayDumbMainMenuBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.DumbActionTypeChoice
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.allDumbActionChoices
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.click.DumbClickDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.pause.DumbPauseDialog
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.swipe.DumbSwipeDialog
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
        OverlayManager.getInstance(context).navigateTo(
            context = context,
            newOverlay = MultiChoiceDialog(
                theme = R.style.AppTheme,
                dialogTitleText = R.string.dialog_overlay_title_dumb_action_type,
                choices = allDumbActionChoices(),
                onChoiceSelected = { choice ->
                    when (choice) {
                        DumbActionTypeChoice.Click -> onDumbClickCreationSelected()
                        DumbActionTypeChoice.Swipe -> onDumbSwipeCreationSelected()
                        DumbActionTypeChoice.Pause -> onDumbPauseCreationSelected()
                    }
                }
            )
        )
    }

    private fun onDumbClickCreationSelected() {
        OverlayManager.getInstance(context).navigateTo(
            context = context,
            newOverlay = ClickSwipeSelectorMenu(
                selector = CoordinatesSelector.One(),
                onCoordinatesSelected = { selector ->
                    onDumbClickPositionSelected((selector as CoordinatesSelector.One).coordinates)
                }
            ),
            hideCurrent = true,
        )
    }

    private fun onDumbClickPositionSelected(position: Point?) {
        position ?: return

        viewModel.startEdition(dumbScenarioId) {
            OverlayManager.getInstance(context).navigateTo(
                context = context,
                newOverlay = DumbClickDialog(
                    dumbClick = viewModel.createNewDumbClick(position),
                    onConfirmClicked = { viewModel.addNewDumbAction(it, true) },
                    onDeleteClicked = { viewModel.deleteDumbAction(it, true) },
                    onDismissClicked = viewModel::stopEdition,
                ),
                hideCurrent = true,
            )
        }
    }

    private fun onDumbSwipeCreationSelected() {
        OverlayManager.getInstance(context).navigateTo(
            context = context,
            newOverlay = ClickSwipeSelectorMenu(
                selector = CoordinatesSelector.Two(),
                onCoordinatesSelected = { selector ->
                    (selector as CoordinatesSelector.Two).let { two ->
                        onDumbSwipePositionSelected(two.coordinates1, two.coordinates2)
                    }
                }
            ),
            hideCurrent = true,
        )
    }

    private fun onDumbSwipePositionSelected(from: Point?, to: Point?) {
        if (from == null || to == null) return

        viewModel.startEdition(dumbScenarioId) {
            OverlayManager.getInstance(context).navigateTo(
                context = context,
                newOverlay = DumbSwipeDialog(
                    dumbSwipe = viewModel.createNewDumbSwipe(from, to),
                    onConfirmClicked = { viewModel.addNewDumbAction(it, true) },
                    onDeleteClicked = { viewModel.deleteDumbAction(it, true) },
                    onDismissClicked = viewModel::stopEdition,
                ),
                hideCurrent = true,
            )
        }
    }

    private fun onDumbPauseCreationSelected() {
        viewModel.startEdition(dumbScenarioId) {
            OverlayManager.getInstance(context).navigateTo(
                context = context,
                newOverlay = DumbPauseDialog(
                    dumbPause = viewModel.createNewDumbPause(),
                    onConfirmClicked = { viewModel.addNewDumbAction(it, true) },
                    onDeleteClicked = { viewModel.deleteDumbAction(it, true) },
                    onDismissClicked = viewModel::stopEdition,
                ),
                hideCurrent = true,
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