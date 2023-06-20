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

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.OneMovingTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.OneStillTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoMovingTargetsPressInOrderRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoStillTargetsPressWhenBothVisibleRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoStillTargetsPressWhenOneVisibleRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.feature.tutorial.domain.game.TutorialGameRules

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class TutorialListViewModel : ViewModel() {

    private val tutorialRepository: TutorialRepository = TutorialRepository.getTutorialRepository()

    val items: Flow<List<TutorialItem>> = flowOf(
        buildList {
            add(TutorialItem.Intro)
            tutorialRepository.games.forEachIndexed { index, tutorialGame ->
                tutorialGame.gameRules.toItem(index)?.let { add(it) }
            }
        }
    )

    private fun TutorialGameRules.toItem(index: Int): TutorialItem? =
        when (this) {
            is OneStillTargetRules ->
                TutorialItem.Game(R.string.item_title_game_1, R.string.item_desc_game_1, index)
            is OneMovingTargetRules ->
                TutorialItem.Game(R.string.item_title_game_2, R.string.item_desc_game_2, index)
            is TwoStillTargetsPressWhenBothVisibleRules ->
                TutorialItem.Game(R.string.item_title_game_3, R.string.item_desc_game_3, index)
            is TwoStillTargetsPressWhenOneVisibleRules ->
                TutorialItem.Game(R.string.item_title_game_4, R.string.item_desc_game_4, index)
            is TwoMovingTargetsPressInOrderRules ->
                TutorialItem.Game(R.string.item_title_game_5, R.string.item_desc_game_5, index)
            else -> null
        }
}

sealed class TutorialItem {

    abstract val nameResId: Int
    abstract val descResId: Int

    object Intro : TutorialItem() {
        override val nameResId: Int = R.string.item_title_tutorial_intro
        override val descResId: Int = R.string.item_desc_tutorial_intro
    }

    data class Game(
        override val nameResId: Int,
        override val descResId: Int,
        val index: Int,
    ) : TutorialItem()
}