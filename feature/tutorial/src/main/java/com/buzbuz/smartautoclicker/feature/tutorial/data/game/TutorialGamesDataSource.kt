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
package com.buzbuz.smartautoclicker.feature.tutorial.data.game

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.OneMovingTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.OneStillTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoMovingTargetsPressInOrderRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoStillTargetsPressWhenBothVisibleRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoStillTargetsPressWhenOneVisibleRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.game.TutorialGame

object TutorialGamesDataSource {

    val tutorialGames: List<TutorialGame> = listOf(
        TutorialGame(R.string.message_tutorial_instructions_1, 10, OneStillTargetRules()),
        TutorialGame(R.string.message_tutorial_instructions_1, 10, OneMovingTargetRules()),
        TutorialGame(R.string.message_tutorial_instructions_1, 10, TwoStillTargetsPressWhenBothVisibleRules()),
        TutorialGame(R.string.message_tutorial_instructions_1, 10, TwoStillTargetsPressWhenOneVisibleRules()),
        TutorialGame(R.string.message_tutorial_instructions_1, 10, TwoMovingTargetsPressInOrderRules()),
    )

}
