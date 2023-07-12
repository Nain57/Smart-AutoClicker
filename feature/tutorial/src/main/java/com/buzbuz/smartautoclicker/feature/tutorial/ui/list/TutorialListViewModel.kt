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

import android.app.Application
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.OneMovingTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.OneStillTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoMovingTargetsPressInOrderRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoStillTargetsPressWhenBothVisibleRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoStillTargetsPressWhenOneVisibleRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.TutorialGameRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.Tutorial

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class TutorialListViewModel(application: Application) : AndroidViewModel(application) {

    private val tutorialRepository: TutorialRepository = TutorialRepository.getTutorialRepository(application)
    private val overlayManager: OverlayManager = OverlayManager.getInstance(application)

    val items: Flow<List<TutorialItem>> = flowOf(
        buildList {
            add(TutorialItem.Intro)
            tutorialRepository.tutorials.forEachIndexed { index, tutorial ->
                add(tutorial.toItem(index))
            }
        }
    )

    fun setOverlayVisibility(visible: Boolean) {
        if (visible) overlayManager.restoreAll()
        else overlayManager.hideAll()
    }

    private fun Tutorial.toItem(index: Int): TutorialItem =
        TutorialItem.Game(
            nameResId = nameResId,
            descResId = descResId,
            index = index,
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