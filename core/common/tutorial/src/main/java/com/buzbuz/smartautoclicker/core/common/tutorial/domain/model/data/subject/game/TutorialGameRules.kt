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
package com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.game

import android.graphics.PointF
import android.graphics.Rect


/**
 * Strategy interface for a [TutorialSubject.Game]'s gameplay logic.
 *
 * The engine calls these methods at well-defined moments in the game loop. Implementations
 * declare a tutorial in the feature module and return updated target positions so the UI can
 * redraw them without knowing the game rules.
 *
 * All returned maps use [TutorialGameTargetType] as the key and a screen-coordinate [PointF]
 * (center of the target) as the value. An empty map means no targets should be displayed.
 */
interface TutorialGameRules {

    /**
     * Called once when the game starts.
     *
     * @param area the screen region available for placing targets.
     * @return initial target center positions to display.
     */
    fun onStart(area: Rect): Map<TutorialGameTargetType, PointF>

    /**
     * Called each time the user successfully hits a valid target.
     *
     * Implementations should update internal state (e.g. increment score, pick a new position)
     * and return the refreshed target map.
     *
     * @param current the current targets displayed.
     * @param type the target type that was hit.
     * @return updated target positions after the hit.
     */
    fun onValidTargetHit(
        current: Map<TutorialGameTargetType, PointF>,
        type: TutorialGameTargetType,
    ): Map<TutorialGameTargetType, PointF>

    /**
     * Called on every timer tick (once per second) while the game is running.
     *
     * @param current the current targets displayed.
     * @param timeLeft remaining game duration in milliseconds at the time of the tick.
     * @return updated target positions for this tick.
     */
    fun onTimerTick(current: Map<TutorialGameTargetType, PointF>, timeLeft: Long): Map<TutorialGameTargetType, PointF>

    /** Returns the player's current score. Called by the engine after each tick and hit. */
    fun getScore(): Int
}