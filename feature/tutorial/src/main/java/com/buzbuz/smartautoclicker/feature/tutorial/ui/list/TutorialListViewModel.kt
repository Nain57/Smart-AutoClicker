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

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.Tutorial
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class TutorialListViewModel @Inject constructor(
    private val tutorialRepository: TutorialRepository
) : ViewModel() {

    val items: Flow<List<TutorialItem>> = tutorialRepository.tutorials
        .map { tutorials ->
            tutorials
                .filter { it.isUnlocked }
                .mapIndexed { index, tutorial -> tutorial.toItem(index) }
        }

    private fun Tutorial.toItem(index: Int): TutorialItem =
        TutorialItem(
            nameResId = nameResId,
            descResId = descResId,
            index = index,
        )
}

data class TutorialItem(
    val nameResId: Int,
    val descResId: Int,
    val index: Int,
)
