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

import android.graphics.PointF
import android.graphics.Rect

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal data class TutorialGameData(
    @StringRes val instructionsResId: Int,
    val gameRules: TutorialGameRules,
) : TutorialGameRules by gameRules

internal interface TutorialGameRules {
    val highScore: Int
    val gameState: Flow<TutorialGameStateData>
    val targets: StateFlow<Map<TutorialGameTargetType, PointF>>

    fun start(coroutineScope: CoroutineScope, area: Rect, targetSize: Int, onResult: (Boolean) -> Unit)
    fun stop()
    fun onTargetHit(type: TutorialGameTargetType)
}

internal data class TutorialGameStateData(
    val isStarted: Boolean = false,
    val isWon: Boolean? = null,
    val timeLeft: Int = 0,
    val score: Int = 0,
)