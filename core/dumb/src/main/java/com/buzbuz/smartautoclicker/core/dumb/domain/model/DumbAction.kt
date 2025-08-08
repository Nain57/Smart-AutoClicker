
package com.buzbuz.smartautoclicker.core.dumb.domain.model

import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

sealed class DumbAction : Identifiable {

    /** The identifier of the dumb scenario for this dumb action. */
    abstract val scenarioId: Identifier
    /** The name of the dumb action. */
    abstract val name: String?

    abstract val priority: Int

    abstract fun isValid(): Boolean

    fun copyWithNewScenarioId(scenarioId: Identifier): DumbAction =
        when (this) {
            is DumbClick -> copy(scenarioId = scenarioId)
            is DumbPause -> copy(scenarioId = scenarioId)
            is DumbSwipe -> copy(scenarioId = scenarioId)
        }

    data class DumbClick(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        override val priority: Int = 0,
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
        override val priority: Int = 0,
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
        override val priority: Int = 0,
        val pauseDurationMs: Long,
    ) : DumbAction() {

        override fun isValid(): Boolean = name.isNotEmpty()
    }
}
