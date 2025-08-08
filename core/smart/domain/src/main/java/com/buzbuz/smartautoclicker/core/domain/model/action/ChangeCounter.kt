
package com.buzbuz.smartautoclicker.core.domain.model.action

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.ChangeCounterOperationType
import com.buzbuz.smartautoclicker.core.domain.model.CounterOperationValue

data class ChangeCounter(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val counterName: String,
    val operation: OperationType,
    val operationValue: CounterOperationValue,
): Action() {

    /**
     * Types of counter change of a [ChangeCounter].
     * Keep the same names as the db ones.
     */
    enum class OperationType {
        /** Add to the current counter value. */
        ADD,
        /** Remove from the current counter value. */
        MINUS,
        /** Set the counter to a specific value. */
        SET;

        fun toEntity(): ChangeCounterOperationType = ChangeCounterOperationType.valueOf(name)
    }

    override fun isComplete(): Boolean =
        super.isComplete() && counterName.isNotEmpty() && operationValue.isComplete()

    override fun hashCodeNoIds(): Int =
        name.hashCode() + counterName.hashCode() + operation.hashCode() + operationValue.hashCode()

    override fun deepCopy(): ChangeCounter = copy(
        name = "" + name,
        counterName = "" + counterName,
    )
}