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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.game.bindings

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.databinding.IncludeTutorialGameHeaderBinding


internal fun IncludeTutorialGameHeaderBinding.setHeaderInfo(@StringRes instructionsResId: Int, highScore: Int) {
    textInstructions.setText(instructionsResId)
    textHighScore.text = root.context.getString(R.string.message_high_score, highScore)
}

internal fun IncludeTutorialGameHeaderBinding.setScore(score: Int) {
    textScore.text = root.context.getString(R.string.message_score, score)
}
