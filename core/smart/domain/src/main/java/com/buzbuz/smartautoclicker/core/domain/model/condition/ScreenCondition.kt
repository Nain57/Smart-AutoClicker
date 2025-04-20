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

import com.buzbuz.smartautoclicker.core.domain.model.DetectionType
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Prioritizable
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnBroadcastReceived
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnCounterCountReached
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition.OnTimerReached
import com.buzbuz.smartautoclicker.core.smart.training.model.TrainedTextLanguage


sealed class ScreenCondition : Condition(), Prioritizable {

    /** The accepted difference between the conditions and the screen content, in percent (0-100%). */
    abstract val threshold: Int
    /** Tells if the condition should be detected to be fulfilled. */
    abstract val shouldBeDetected: Boolean

    /** The type of detection for this condition. Must be one of [DetectionType]. */
    @DetectionType abstract val detectionType: Int
    /** The area to detect the condition in if [detectionType] is IN_AREA. */
    abstract val detectionArea: Rect?

    /** Tells if this condition is complete and valid to be saved. */
    override fun isComplete(): Boolean =
        super.isComplete() && (detectionType == IN_AREA && detectionArea != null || detectionType != IN_AREA)

    override fun hashCodeNoIds(): Int =
        name.hashCode() + threshold.hashCode() + detectionType.hashCode() + shouldBeDetected.hashCode() +
                detectionArea.hashCode() + priority.hashCode()

    fun copyBase(
        evtId: Identifier = this.eventId,
        name: String = this.name,
        priority: Int = this.priority,
        threshold: Int = this.threshold,
        detectionType: Int = this.detectionType,
        detectionArea: Rect? = this.detectionArea,
    ): ScreenCondition = when (this) {
        is ImageCondition -> copy(eventId = evtId, name = name, priority = priority, threshold = threshold,
            detectionType = detectionType, detectionArea = detectionArea)
        is TextCondition -> copy(eventId = evtId, name = name, priority = priority, threshold = threshold,
            detectionType = detectionType, detectionArea = detectionArea)
    }
}


/**
 * Image condition for a Event.
 *
 * @param path the path to the bitmap that should be matched for detection.
 * @param captureArea the area of the condition within the screen at capture time.
 */
data class ImageCondition(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String,
    override var priority: Int,
    override val threshold: Int,
    override val shouldBeDetected: Boolean,
    override val detectionType: Int,
    override val detectionArea: Rect? = null,
    val path: String,
    val captureArea: Rect,
): ScreenCondition() {

    override fun hashCodeNoIds(): Int =
        super.hashCode() + path.hashCode() + captureArea.hashCode()
}


/**
 * Text condition for a Event.
 *
 * @param textToDetect the text to be detected.
 * @param textLanguage the language of the text.
 */
data class TextCondition(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String,
    override var priority: Int,
    override val threshold: Int,
    override val shouldBeDetected: Boolean,
    override val detectionType: Int,
    override val detectionArea: Rect? = null,
    val textToDetect: String,
    val textLanguage: TrainedTextLanguage,
) : ScreenCondition() {

    override fun isComplete(): Boolean =
        super.isComplete() && textToDetect.isNotBlank()

    override fun hashCodeNoIds(): Int =
        super.hashCode() + textToDetect.hashCode() + textLanguage.hashCode()
}