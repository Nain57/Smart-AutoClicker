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
package com.buzbuz.smartautoclicker.core.domain.model.triggercondition

import androidx.annotation.CallSuper

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation

sealed class TriggerCondition: Identifiable, Completable {

    abstract val triggerEventId: Identifier
    abstract val name: String

    @CallSuper
    override fun isComplete(): Boolean =
        name.isNotEmpty()

    @Suppress("USELESS_CAST") // Yet it is required by Android Studio
    fun copy(eventId: Identifier) = when (this) {
        is OnScenarioStart -> (this as OnScenarioStart).copy(triggerEventId = eventId)
        is OnScenarioEnd -> (this as OnScenarioEnd).copy(triggerEventId = eventId)
        is OnBroadcastReceived -> (this as OnBroadcastReceived).copy(triggerEventId = eventId)
        is OnCounterCountReached -> (this as OnCounterCountReached).copy(triggerEventId = eventId)
        is OnTimerReached -> (this as OnTimerReached).copy(triggerEventId = eventId)
    }

    data class OnScenarioStart(
        override val id: Identifier,
        override val triggerEventId: Identifier,
        override val name: String,
    ) : TriggerCondition()

    data class OnScenarioEnd(
        override val id: Identifier,
        override val triggerEventId: Identifier,
        override val name: String,
    ) : TriggerCondition()

    data class OnBroadcastReceived(
        override val id: Identifier,
        override val triggerEventId: Identifier,
        override val name: String,
        val intentAction: String,
    ) : TriggerCondition() {

        override fun isComplete(): Boolean =
            super.isComplete() && intentAction.isNotEmpty()
    }

    data class OnCounterCountReached(
        override val id: Identifier,
        override val triggerEventId: Identifier,
        override val name: String,
        val counterName: String,
        val comparisonOperation: ComparisonOperation,
        val counterValue: Int,
    ) : TriggerCondition() {

        /**
         * Type of counter comparison.
         * /!\ DO NOT RENAME: ComparisonOperation enum name is used in the database.
         */
        enum class ComparisonOperation {
            /** The counter value is strictly equals to the value. */
            EQUALS,
            /** The counter value is strictly lower than the value. */
            LOWER,
            /** The counter value is lower or equals to the value */
            LOWER_OR_EQUALS,
            /** The counter value is strictly greater than the value. */
            GREATER,
            /** The counter value is greater or equals to the value. */
            GREATER_OR_EQUALS;

            fun toEntity(): CounterComparisonOperation = CounterComparisonOperation.valueOf(name)
        }

        override fun isComplete(): Boolean =
            super.isComplete() && counterName.isNotEmpty()
    }

    data class OnTimerReached(
        override val id: Identifier,
        override val triggerEventId: Identifier,
        override val name: String,
        val durationMs: Long,
    ) : TriggerCondition() {

        override fun isComplete(): Boolean =
            super.isComplete() && durationMs > 0
    }
}