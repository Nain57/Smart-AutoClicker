
package com.buzbuz.smartautoclicker.core.domain.model.condition

import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.domain.model.DetectionType
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Prioritizable

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
data class ImageCondition(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String,
    override var priority: Int,
    val path: String,
    val area: Rect,
    val threshold: Int,
    @DetectionType val detectionType: Int,
    val shouldBeDetected: Boolean,
    val detectionArea: Rect? = null,
): Condition(), Prioritizable {

    /** @return creates a deep copy of this condition. */
    fun deepCopy(): ImageCondition = copy(
        path = "" + path,
        area = Rect(area),
    )

    /** Tells if this condition is complete and valid to be saved. */
    override fun isComplete(): Boolean =
        super.isComplete() && (detectionType == IN_AREA && detectionArea != null || detectionType != IN_AREA)

    override fun hashCodeNoIds(): Int =
        name.hashCode() + path.hashCode() + area.hashCode() + threshold.hashCode() + detectionType.hashCode() +
                shouldBeDetected.hashCode() + detectionArea.hashCode() + priority.hashCode()
}
