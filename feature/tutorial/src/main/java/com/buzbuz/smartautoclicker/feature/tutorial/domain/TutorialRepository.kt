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
package com.buzbuz.smartautoclicker.feature.tutorial.domain

import android.content.Context
import android.graphics.Rect

import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialDataSource
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialPlayer
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.Tutorial
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialOverlayState
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.toDomain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TutorialRepository private constructor(
    context: Context,
    private val dataSource: TutorialDataSource,
) {

    companion object {

        /** Singleton preventing multiple instances of the TutorialRepository at the same time. */
        @Volatile
        private var INSTANCE: TutorialRepository? = null

        /**
         * Get the TutorialRepository singleton, or instantiates it if it wasn't yet.
         *
         * @return the TutorialRepository singleton.
         */
        fun getTutorialRepository(context: Context): TutorialRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TutorialRepository(context, TutorialDataSource)
                INSTANCE = instance
                instance
            }
        }
    }

    private val tutorialPlayer: TutorialPlayer = TutorialPlayer(context)

    val tutorials: List<Tutorial> = dataSource.tutorials.map { it.toDomain() }

    val activeTutorial: Flow<Tutorial?> = tutorialPlayer.tutorial.map { it?.toDomain() }

    val tutorialOverlayState: Flow<TutorialOverlayState?> =
        combine(tutorialPlayer.currentStep, tutorialPlayer.stepMonitoredViewPosition) { step, position ->
            step?.toDomain(position)
        }

    fun startTutorial(index: Int) {
        if (index < 0 || index >= tutorials.size) return
        tutorialPlayer.startTutorial(dataSource.tutorials[index])
    }

    fun nextTutorialStep() {
        tutorialPlayer.nextStep()
    }

    fun skipAllTutorialSteps() {
        tutorialPlayer.skipAllSteps()
    }

    fun startGame(scope: CoroutineScope, area: Rect, targetSize: Int) {
        tutorialPlayer.startGame(scope, area, targetSize)
    }

    fun onGameTargetHit(targetType: TutorialGameTargetType) {
        tutorialPlayer.onGameTargetHit(targetType)
    }

    fun stopGame() {
        tutorialPlayer.stopGame()
    }
    fun stopTutorial() {
        tutorialPlayer.stopTutorial()
    }
}