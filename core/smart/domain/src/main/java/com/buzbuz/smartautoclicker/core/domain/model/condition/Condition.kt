/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.model.condition

import android.graphics.Rect
import androidx.annotation.CallSuper

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable

sealed class Condition : Identifiable, Completable {

    /** The identifier of the event for this condition. */
    abstract val eventId: Identifier
    /** The name of the condition. */
    abstract val name: String

    @CallSuper
    override fun isComplete(): Boolean =
        name.isNotEmpty()

    abstract fun hashCodeNoIds(): Int

    fun withNewEventId(evtId: Identifier): Condition = when (this) {
        is ScreenCondition -> copyBase(evtId = evtId)
        is TriggerCondition -> copyBase(evtId = evtId)
    }
}