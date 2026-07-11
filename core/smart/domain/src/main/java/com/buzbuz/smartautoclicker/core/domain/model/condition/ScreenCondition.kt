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
package com.buzbuz.smartautoclicker.core.domain.model.condition

import android.graphics.Rect
import androidx.annotation.ColorInt
import com.buzbuz.smartautoclicker.code.smart.detectionmodels.text.domain.OCRAlphabet

import com.buzbuz.smartautoclicker.core.domain.model.DetectionType
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Prioritizable
import com.buzbuz.smartautoclicker.core.domain.model.counter.ComparisonOperation
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue

/** Conditions verified upon the screen content. */
sealed class ScreenCondition : Condition(), Prioritizable {

    /** The accepted difference between the conditions and the screen content, in percent (0-100%). */
    abstract val threshold: Int
    /** If the condition should be fulfilled when the color is detected or when it is not detected. */
    abstract val shouldBeDetected: Boolean

    fun copyCondition(
        eventId: Identifier = this.eventId,
        priority: Int = this.priority,
        threshold: Int = this.threshold,
        shouldBeDetected: Boolean = this.shouldBeDetected,
    ) =
        when (this) {
            is Color -> copy(eventId = eventId, priority = priority, threshold = threshold, shouldBeDetected = shouldBeDetected)
            is Image -> copy(eventId = eventId, priority = priority, threshold = threshold, shouldBeDetected = shouldBeDetected)
            is Number -> copy(eventId = eventId, priority = priority, threshold = threshold)
            is Text -> copy(eventId = eventId, priority = priority, threshold = threshold, shouldBeDetected = shouldBeDetected)
        }

    /**
     * Color condition for a Event.
     *
     * @param color the color to be matched during detection.
     * @param shouldBeDetected
     * @param detectionArea the area to check the color for. If more than a pixel, it will verify the average color
     * of the detection area.
     */
    data class Color(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String,
        override val threshold: Int,
        override val shouldBeDetected: Boolean,
        override var priority: Int,
        @field:ColorInt val color: Int,
        val detectionArea: Rect,
    ) : ScreenCondition(), Prioritizable {

        override fun hashCodeNoIds(): Int =
            name.hashCode() + color.hashCode() + threshold.hashCode() + shouldBeDetected.hashCode() +
                    detectionArea.hashCode() + priority.hashCode()
    }

    /**
     * Image condition for a Event.
     *
     * @param id the unique identifier for the condition.
     * @param eventId the identifier of the event for this condition.
     * @param name the name of the condition.
     * @param path the path to the bitmap that should be matched for detection.
     * @param area the area of the screen to detect.
     * @param threshold the accepted difference between the conditions and the screen content, in percent (0-100%).
     * @param detectionType the type of detection for this condition. Must be one of [DetectionType].
     * @param detectionArea the area to detect the condition in if [detectionType] is IN_AREA.
     */
    data class Image(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String,
        override val threshold: Int,
        override val shouldBeDetected: Boolean,
        override var priority: Int,
        val path: String,
        val area: Rect,
        @param:DetectionType val detectionType: Int,
        val detectionArea: Rect? = null,
    ): ScreenCondition(), Prioritizable {

        /** Tells if this condition is complete and valid to be saved. */
        override fun isComplete(): Boolean =
            super.isComplete() && (detectionType == IN_AREA && detectionArea != null || detectionType != IN_AREA)

        override fun hashCodeNoIds(): Int =
            name.hashCode() + path.hashCode() + area.hashCode() + threshold.hashCode() + detectionType.hashCode() +
                    shouldBeDetected.hashCode() + detectionArea.hashCode() + priority.hashCode()
    }

    data class Number(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String,
        override val threshold: Int,
        override val shouldBeDetected: Boolean = true,
        override var priority: Int,
        val detectionArea: Rect,
        val comparisonOperation: ComparisonOperation,
        val counterValue: CounterOperationValue,
        val numberFormatType: NumberFormatType = NumberFormatType.AUTO,
    ): ScreenCondition(), Prioritizable {

        /** Tells if this condition is complete and valid to be saved. */
        override fun isComplete(): Boolean =
            super.isComplete() && counterValue.isComplete() && !detectionArea.isEmpty

        override fun hashCodeNoIds(): Int =
            name.hashCode() + counterValue.hashCode() + threshold.hashCode() + shouldBeDetected.hashCode() +
                    detectionArea.hashCode() + priority.hashCode() + comparisonOperation.hashCode() +
                    numberFormatType.hashCode()

    }

    data class Text(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String,
        override val threshold: Int,
        override val shouldBeDetected: Boolean,
        override var priority: Int,
        val text: String,
        val detectionArea: Rect,
        val alphabet: OCRAlphabet,
    ): ScreenCondition(), Prioritizable {

        /** Tells if this condition is complete and valid to be saved. */
        override fun isComplete(): Boolean =
            super.isComplete() && text.isNotEmpty() && !detectionArea.isEmpty

        override fun hashCodeNoIds(): Int =
            name.hashCode() + text.hashCode() + threshold.hashCode() + shouldBeDetected.hashCode() +
                    detectionArea.hashCode() + priority.hashCode()

    }
}
