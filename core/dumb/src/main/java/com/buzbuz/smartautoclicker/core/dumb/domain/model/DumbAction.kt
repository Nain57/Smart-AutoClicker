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
package com.buzbuz.smartautoclicker.core.dumb.domain.model

import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

sealed class DumbAction {

    /** The unique identifier for the action. */
    abstract val id: Identifier
    /** The identifier of the dumb scenario for this dumb action. */
    abstract val scenarioId: Identifier
    /** The name of the dumb action. */
    abstract val name: String?

    abstract fun isValid(): Boolean

    data class DumbClick(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        override val repeatCount: Int,
        override val isRepeatInfinite: Boolean,
        override val repeatDelayMs: Long,
        val position: Point,
        val pressDurationMs: Long,
    ) : DumbAction(), RepeatableWithDelay {

        override fun isValid(): Boolean =
            name.isNotEmpty() && pressDurationMs > 0 && isRepeatCountValid() && isRepeatDelayValid()
    }

    data class DumbSwipe(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        override val repeatCount: Int,
        override val isRepeatInfinite: Boolean,
        override val repeatDelayMs: Long,
        val fromPosition: Point,
        val toPosition: Point,
        val swipeDurationMs: Long,
    ) : DumbAction(), RepeatableWithDelay {
        override fun isValid(): Boolean =
            name.isNotEmpty() && swipeDurationMs > 0 && isRepeatCountValid() && isRepeatDelayValid()
    }

    data class DumbPause(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        val pauseDurationMs: Long,
    ) : DumbAction() {

        override fun isValid(): Boolean = name.isNotEmpty()
    }
}
