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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager

import com.buzbuz.smartautoclicker.feature.tutorial.databinding.FragmentTutorialListBinding

import kotlinx.coroutines.launch

class TutorialListFragment : Fragment() {

    /** ViewModel providing the state of the UI. */
    private val viewModel: TutorialListViewModel by viewModels()
    /** ViewBinding containing the views for this fragment. */
    private lateinit var viewBinding: FragmentTutorialListBinding
    /** Adapter for the list of tutorials. */
    private lateinit var adapter: TutorialListAdapter

    private var isOpeningTutorial: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentTutorialListBinding.inflate(inflater, container, false)

        adapter = TutorialListAdapter(
            onGameClicked = ::onGameClicked,
        )

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

    override fun onStart() {
        super.onStart()
        OverlayManager.getInstance(requireContext()).hideAll()
    }

    private fun onGameClicked(gameIndex: Int) {
        isOpeningTutorial = true
        findNavController().navigate(TutorialListFragmentDirections.tutorialListToGame(gameIndex))
    }
}