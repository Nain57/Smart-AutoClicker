/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController

import com.buzbuz.smartautoclicker.core.display.recorder.MediaProjectionRequest
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.core.ui.errors.createNoMediaProjectionDialog
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategoryUiItems
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategoryUiState

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class TutorialListFragment : Fragment() {

    /** ViewModel providing the state of the UI. */
    private val viewModel: TutorialListViewModel by viewModels()
    /** ViewBinding containing the views for this fragment. */
    private lateinit var viewBinding: IncludeLoadableListBinding
    /** Adapter for the list of tutorials. */
    private lateinit var adapter: TutorialListAdapter

    /** The result launcher for the projection permission dialog. */
    private val mediaProjectionRequest: MediaProjectionRequest = MediaProjectionRequest()
    /** Handles back navigation: browse up a category, or let the activity handle it when at the root. */
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!viewModel.browseParent()) {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = IncludeLoadableListBinding.inflate(inflater, container, false)

        adapter = TutorialListAdapter(onItemClicked = ::onItemClicked)
        mediaProjectionRequest.registerForActivityResult(this)

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.list.adapter = adapter

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, backPressedCallback)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUi) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.stopTutorial()
    }

    private fun updateUi(uiState: TutorialCategoryUiState) {
        when (uiState) {
            TutorialCategoryUiState.Loading -> viewBinding.updateState(null)
            is TutorialCategoryUiState.Loaded -> {
                viewBinding.updateState(uiState.items)
                adapter.submitList(uiState.items)
            }
        }
    }

    private fun onItemClicked(item: TutorialCategoryUiItems.Item) {
        when (item) {
            is TutorialCategoryUiItems.Item.Category -> {
                viewModel.browseCategory(item)
            }

            is TutorialCategoryUiItems.Item.Tutorial -> {
                val tutorialActivity : AppCompatActivity = activity as? AppCompatActivity ?: return
                viewModel.startPermissionFlowIfNeeded(
                    activity = tutorialActivity,
                    onAllGranted = { showMediaProjectionWarning(item) },
                )
            }
        }
    }

    private fun showMediaProjectionWarning(item: TutorialCategoryUiItems.Item.Tutorial) {
        mediaProjectionRequest.showMediaProjectionWarning(
            context = requireContext(),
            forceEntireScreen = true,
            onSuccess = { resultCode, data -> startTutorial(item, resultCode, data) },
            onFailure = { showProjectionDeniedToast() },
            onError = { showUnsupportedDeviceDialog() },
        )
    }

    private fun showProjectionDeniedToast() {
        Toast.makeText(activity, R.string.toast_denied_screen_sharing_permission, Toast.LENGTH_SHORT).show()
    }

    private fun showUnsupportedDeviceDialog() {
        requireContext().createNoMediaProjectionDialog { activity?.finish() }.show()
    }

    private fun startTutorial(item: TutorialCategoryUiItems.Item.Tutorial, resultCode: Int, data: Intent) {
        viewModel.startTutorial(item, resultCode, data)
        findNavController().navigate(TutorialListFragmentDirections.tutorialListToGame())
    }
}