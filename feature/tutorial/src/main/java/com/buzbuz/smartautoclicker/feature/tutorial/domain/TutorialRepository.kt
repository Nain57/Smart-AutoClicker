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
import android.util.Log

import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialDataSource
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialEngine
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.Tutorial
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialOverlayState
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.game.TutorialGameTargetType
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.toDomain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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

    private val tutorialEngine: TutorialEngine = TutorialEngine(context)

    val tutorials: List<Tutorial> = dataSource.tutorials.map { it.toDomain() }

    val activeTutorial: Flow<Tutorial?> = tutorialEngine.tutorial.map { it?.toDomain() }

    val tutorialOverlayState: Flow<TutorialOverlayState?> = tutorialEngine.currentStep
        .map { step ->
            Log.d(TAG, "Update overlay state for step $step")
            step?.toDomain()
        }

    fun startTutorial(index: Int) {
        if (index < 0 || index >= tutorials.size) return
        tutorialEngine.startTutorial(dataSource.tutorials[index])
    }

    fun nextTutorialStep() {
        tutorialEngine.nextStep()
    }

    fun skipAllTutorialSteps() {
        tutorialEngine.skipAllSteps()
    }

    fun startGame(scope: CoroutineScope, area: Rect, targetSize: Int) {
        tutorialEngine.startGame(scope, area, targetSize)
    }

    fun onGameTargetHit(targetType: TutorialGameTargetType) {
        tutorialEngine.onGameTargetHit(targetType)
    }

    fun stopGame() {
        tutorialEngine.stopGame()
    }
    fun stopTutorial() {
        tutorialEngine.stopTutorial()
    }
}

private const val TAG = "TutorialRepository"