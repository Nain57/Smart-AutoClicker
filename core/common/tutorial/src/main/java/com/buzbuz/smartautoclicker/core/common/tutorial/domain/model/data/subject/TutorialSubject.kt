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
package com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.quickclickgame.QuickClickGameRules

/**
 * Describes the interactive subject that runs alongside a tutorial's step sequence.
 *
 * The engine creates a matching [TutorialSubjectController] from the concrete subtype and exposes
 * it to the feature layer so UI components can drive and observe the subject.
 */
sealed interface TutorialSubject {

    /**
     * A timed mini-game subject where the user must reach a target score before time runs out.
     *
     * @property instructionsResId string resource explaining how to play, shown before the game starts.
     * @property scoreToReach minimum score (exclusive) the user must exceed to win.
     * @property durationSeconds total game duration in seconds.
     * @property rules strategy object that controls target placement, movement, and scoring.
     */
    data class QuickClickGame(
        @field:StringRes val instructionsResId: Int,
        val scoreToReach: Int,
        val durationSeconds: Long,
        val rules: QuickClickGameRules,
    ) : TutorialSubject

    /**
     *
     */
    data class TimingGame(
        @field:StringRes val instructionsResId: Int,
        val clickCount: Int,
        val frequencyMs: Int,
        val targetTotalDiffMs: Long,
    ) : TutorialSubject
}