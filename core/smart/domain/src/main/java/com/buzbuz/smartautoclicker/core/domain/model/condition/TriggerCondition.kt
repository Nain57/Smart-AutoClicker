/*
 * Copyright (C) 2024 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue

sealed class TriggerCondition: Condition(), Identifiable, Completable {

    override fun hashCodeNoIds(): Int =
        name.hashCode()

    fun copyBase(
        evtId: Identifier = this.eventId,
        name: String = this.name,
    ) = when (this) {
        is OnBroadcastReceived -> copy(eventId = evtId, name = name)
        is OnCounterCountReached -> copy(eventId = evtId, name = name)
        is OnTimerReached -> copy(eventId = evtId, name = name)
    }

    data class OnBroadcastReceived(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String,
        val intentAction: String,
    ) : TriggerCondition() {

        override fun isComplete(): Boolean =
            super.isComplete() && intentAction.isNotEmpty()

        override fun hashCodeNoIds(): Int =
            super.hashCode() + intentAction.hashCode()
    }

    data class OnCounterCountReached(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String,
        val counterName: String,
        val comparisonOperation: ComparisonOperation,
        val counterValue: CounterOperationValue,
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
            super.isComplete() && counterName.isNotEmpty() && counterValue.isComplete()

        override fun hashCodeNoIds(): Int =
            super.hashCode() + counterName.hashCode() + comparisonOperation.hashCode() + counterValue.hashCode()
    }

    data class OnTimerReached(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String,
        val durationMs: Long,
        val restartWhenReached: Boolean,
    ) : TriggerCondition() {

        override fun isComplete(): Boolean =
            super.isComplete() && durationMs > 0

        override fun hashCodeNoIds(): Int =
            super.hashCode() + durationMs.hashCode()
    }
}