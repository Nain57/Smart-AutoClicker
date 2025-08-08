
package com.buzbuz.smartautoclicker.core.domain.model.condition

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.database.entity.CounterComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue

sealed class TriggerCondition: Condition(), Identifiable, Completable {

    @Suppress("USELESS_CAST") // Yet it is required by Android Studio
    fun copy(evtId: Identifier) = when (this) {
        is OnBroadcastReceived -> (this as OnBroadcastReceived).copy(eventId = evtId)
        is OnCounterCountReached -> (this as OnCounterCountReached).copy(eventId = evtId)
        is OnTimerReached -> (this as OnTimerReached).copy(eventId = evtId)
    }

    override fun hashCodeNoIds(): Int =
        name.hashCode()

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