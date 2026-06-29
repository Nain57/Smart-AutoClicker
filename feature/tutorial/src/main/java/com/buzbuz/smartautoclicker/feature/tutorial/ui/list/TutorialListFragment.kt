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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController

import com.buzbuz.smartautoclicker.core.display.recorder.MediaProjectionRequest
import com.buzbuz.smartautoclicker.core.ui.errors.createNoMediaProjectionDialog
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.FragmentTutorialListBinding

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class TutorialListFragment : Fragment() {

    /** ViewModel providing the state of the UI. */
    private val viewModel: TutorialListViewModel by viewModels()
    /** ViewBinding containing the views for this fragment. */
    private lateinit var viewBinding: FragmentTutorialListBinding
    /** Adapter for the list of tutorials. */
    private lateinit var adapter: TutorialListAdapter

    /** The result launcher for the projection permission dialog. */
    private val mediaProjectionRequest: MediaProjectionRequest = MediaProjectionRequest()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentTutorialListBinding.inflate(inflater, container, false)

        adapter = TutorialListAdapter(onItemClicked = ::onItemClicked)
        mediaProjectionRequest.registerForActivityResult(this)

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.list.adapter = adapter

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect(adapter::submitList)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.stopTutorial()
    }

    private fun onItemClicked(tutorialId: String) {
        val tutorialActivity : AppCompatActivity = activity as? AppCompatActivity ?: return
        viewModel.startPermissionFlowIfNeeded(
            activity = tutorialActivity,
            onAllGranted = { showMediaProjectionWarning(tutorialId) },
        )
    }

    private fun showMediaProjectionWarning(tutorialId: String) {
        mediaProjectionRequest.showMediaProjectionWarning(
            context = requireContext(),
            forceEntireScreen = viewModel.isEntireScreenCaptureForced(),
            onSuccess = { resultCode, data -> startTutorial(tutorialId, resultCode, data) },
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

    private fun startTutorial(tutorialId: String, resultCode: Int, data: Intent) {
        viewModel.startTutorial(tutorialId, resultCode, data)
        findNavController().navigate(TutorialListFragmentDirections.tutorialListToGame())
    }
}