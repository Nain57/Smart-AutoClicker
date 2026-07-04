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
package com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game.TutorialGameTargetType

/**
 * Live state of the active tutorial subject, emitted on [TutorialSubjectController.Game.state].
 *
 * The sealed hierarchy mirrors [TutorialSubject]: each subject type has a matching state subtype.
 */
sealed interface TutorialSubjectState {

    val subject: TutorialSubject

    /**
     * Live state for a [TutorialSubject.Game] subject.
     *
     * @property isFinished `true` once the game timer has reached zero.
     * @property isWon `true` if the player's score exceeded [TutorialSubject.Game.scoreToReach],
     *   `false` if the game ended without reaching it, or `null` while the game is still in progress.
     * @property timeLeft remaining game duration in milliseconds; 0 before the game starts or after it ends.
     * @property score the player's current score as reported by [TutorialGameRules.getScore].
     * @property targets current target positions keyed by [TutorialGameTargetType]; empty when no
     *   targets should be displayed (before start or after game over).
     */
    data class Game(
        override val subject: TutorialSubject.Game,
        val isFinished: Boolean,
        val isWon: Boolean?,
        val timeLeft: Long,
        val score: Int,
        val targets: Map<TutorialGameTargetType, TutorialGameTargetState>,
    ) : TutorialSubjectState

}