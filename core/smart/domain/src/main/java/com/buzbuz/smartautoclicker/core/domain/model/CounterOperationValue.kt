
package com.buzbuz.smartautoclicker.core.domain.model

import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.database.entity.CounterOperationValueType

sealed class CounterOperationValue : Completable {

    abstract val value: Any

    data class Number(override val value: Int): CounterOperationValue() {
        override fun isComplete(): Boolean = value >= 0
    }

    data class Counter(override val value: String): CounterOperationValue() {
        override fun isComplete(): Boolean = value.isNotEmpty()
    }

    internal companion object {

        fun getCounterOperationValue(
            type: CounterOperationValueType?,
            numberValue: Int?,
            counterName: String?,
        ) = when (type ?: CounterOperationValueType.NUMBER) {
            CounterOperationValueType.COUNTER -> Counter(counterName ?: "")
            CounterOperationValueType.NUMBER -> Number(numberValue ?: 0)
        }
    }
}