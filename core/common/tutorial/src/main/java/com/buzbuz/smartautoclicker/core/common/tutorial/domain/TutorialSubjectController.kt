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

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameTargetType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state.TutorialSubjectState
import kotlinx.coroutines.flow.StateFlow


/**
 * Runtime controller for the active tutorial subject, returned by the engine when a tutorial starts.
 *
 * The feature layer uses this handle to drive the subject-specific logic (e.g. starting a game,
 * reporting hits) and to observe its live state. The sealed hierarchy mirrors [TutorialSubject]:
 * each subject type has a matching controller sub-interface.
 */
sealed interface TutorialSubjectController {

    /** Live game state: score, time left, target positions, and finish/win flags. */
    val state: StateFlow<TutorialSubjectState>

    /** Starts the subject. */
    fun start()

    /** Stops the subject and releases any ongoing work (coroutines, timers). */
    fun stop()


    /** Controller for a [TutorialSubject.QuickClickGame] subject. */
    interface QuickClickGame : TutorialSubjectController {

        /**
         * Notifies the controller that the user tapped a valid target.
         *
         * @param target the type of target that was hit.
         */
        fun onGameTargetHit(target: QuickClickGameTargetType)
    }

    /** Controller for a [TutorialSubject.TimingGame] subject. */
    interface TimingGame : TutorialSubjectController {

        /**
         * Notifies the controller that the user tapped the timing button
         */
        fun onGameTimingButtonHit()
    }
}