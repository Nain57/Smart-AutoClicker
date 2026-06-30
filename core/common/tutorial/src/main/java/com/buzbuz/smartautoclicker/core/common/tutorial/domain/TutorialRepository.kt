/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.tutorial.domain

import android.content.Intent
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tip
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialState

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


/**
 * Entry point for the tutorial engine, exposed to feature modules.
 *
 * Provides read access to the current tutorial execution state. Starting and stopping tutorials
 * is handled internally by the engine; consumers observe [tutorialState] to react to transitions.
 */
interface TutorialRepository {

    val tutorialsCompletionState: Flow<Map<String, Boolean>>

    val tutorialSubjectController: StateFlow<TutorialSubjectController?>

    /** Current state of the tutorial engine. Emits [TutorialState.Stopped] when idle. */
    val tutorialState: StateFlow<TutorialState>


    /**
     * Starts a new tutorial.
     *
     * @param tutorial the tutorial to be loaded.
     * @param mpResultCode the result code of the MediaProjection request
     * @param mpData the intent data of the MediaProjection request
     */
    fun startTutorial(tutorial: Tutorial, mpResultCode: Int, mpData: Intent)

    fun stopTutorial()

    fun nextTutorialStep()
    fun skipToLastTutorialStep()

    fun shouldShowTip(tip: Tip): Flow<Boolean>
    fun dontShowTipAgain(tip: Tip)

}