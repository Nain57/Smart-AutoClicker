
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Prioritizable

/** Base for for all possible actions for an Event. */
sealed class Action : Identifiable, Completable, Prioritizable {

    /** The identifier of the event for this action. */
    abstract val eventId: Identifier
    /** The name of the action. */
    abstract val name: String?

    /** @return true if this action is complete and can be transformed into its entity. */
    override fun isComplete(): Boolean = name != null

    abstract fun hashCodeNoIds(): Int

    /** @return creates a deep copy of this action. */
    abstract fun deepCopy(): Action

    fun copyBase(
        id: Identifier = this.id,
        eventId: Identifier = this.eventId,
        name: String? = this.name,
        priority: Int = this.priority,
    ): Action =
        when (this) {
            is Click -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is ChangeCounter -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Intent -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Pause -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Swipe -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is ToggleEvent -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Notification -> copy(id = id, eventId = eventId, name = name, priority = priority)

            is LongPress -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Scroll -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Back -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Home -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Recents -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is OpenNotifications -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is OpenQuickSettings -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Screenshot -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is HideKeyboard -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is ShowKeyboard -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is TypeText -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is KeyEvent -> copy(id = id, eventId = eventId, name = name, priority = priority)
        }

    // 1.1 Helpers
    enum class Axis { UP, DOWN, LEFT, RIGHT }
    enum class HideMethod { BACK, TAP_OUTSIDE, BACK_THEN_TAP_OUTSIDE }

    // Simple Rect in px for actions that need ROI in action-level (e.g., Screenshot)
    data class RectPx(val left: Int, val top: Int, val width: Int, val height: Int)

// 1.2 New actions

    /** Long press (press & hold). Uses same target mechanics as Click. */
    data class LongPress(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        override var priority: Int,
        val holdDuration: Long? = null,
        val positionType: Click.PositionType,            // reuse Click semantics
        val position: Point? = null,                     // when USER_SELECTED
        val onConditionId: Identifier? = null,           // when ON_DETECTED_CONDITION
        val offset: Point? = null,                       // offset from detected center
    ) : Action() {
        override fun isComplete(): Boolean =
            super.isComplete() && holdDuration != null &&
                    ((positionType == Click.PositionType.USER_SELECTED && position != null) ||
                            positionType == Click.PositionType.ON_DETECTED_CONDITION)

        override fun hashCodeNoIds(): Int =
            name.hashCode() + holdDuration.hashCode() + positionType.hashCode() +
                    position.hashCode() + onConditionId.hashCode() + offset.hashCode()

        override fun deepCopy(): LongPress = copy(name = "" + name)
    }

    /** Scroll as a higher-level swipe (engine will compute from/to from axis+distance). */
    data class Scroll(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        override var priority: Int,
        val axis: Axis? = null,
        val distancePercent: Float? = null,              // 0..1 of screen on that axis
        val duration: Long? = null,
        val stutter: Boolean = true,
    ) : Action() {
        override fun isComplete(): Boolean =
            super.isComplete() && axis != null && distancePercent != null && duration != null

        override fun hashCodeNoIds(): Int =
            name.hashCode() + axis.hashCode() + distancePercent.hashCode() + duration.hashCode() + stutter.hashCode()

        override fun deepCopy(): Scroll = copy(name = "" + name)
    }

    /** System/global actions — no extra fields. */
    data class Back(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        override var priority: Int,
    ) : Action() {
        override fun hashCodeNoIds(): Int = name.hashCode()
        override fun deepCopy(): Back = copy(name = "" + name)
    }
    data class Home(override val id: Identifier, override val eventId: Identifier, override val name: String? = null, override var priority: Int) : Action() {
        override fun hashCodeNoIds(): Int = name.hashCode(); override fun deepCopy(): Home = copy(name = "" + name)
    }
    data class Recents(override val id: Identifier, override val eventId: Identifier, override val name: String? = null, override var priority: Int) : Action() {
        override fun hashCodeNoIds(): Int = name.hashCode(); override fun deepCopy(): Recents = copy(name = "" + name)
    }
    data class OpenNotifications(override val id: Identifier, override val eventId: Identifier, override val name: String? = null, override var priority: Int) : Action() {
        override fun hashCodeNoIds(): Int = name.hashCode(); override fun deepCopy(): OpenNotifications = copy(name = "" + name)
    }
    data class OpenQuickSettings(override val id: Identifier, override val eventId: Identifier, override val name: String? = null, override var priority: Int) : Action() {
        override fun hashCodeNoIds(): Int = name.hashCode(); override fun deepCopy(): OpenQuickSettings = copy(name = "" + name)
    }

    /** Screenshot with optional ROI and path */
    data class Screenshot(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        override var priority: Int,
        val roi: RectPx? = null,
        val savePath: String? = null,                    // optional; null => log-only or temp
    ) : Action() {
        override fun hashCodeNoIds(): Int = name.hashCode() + roi.hashCode() + savePath.hashCode()
        override fun deepCopy(): Screenshot = copy(name = "" + name)
    }

    /** Hide keyboard (strategy) and Show keyboard (tap a focus target). */
    data class HideKeyboard(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        override var priority: Int,
        val method: HideMethod = HideMethod.BACK_THEN_TAP_OUTSIDE,
    ) : Action() {
        override fun hashCodeNoIds(): Int = name.hashCode() + method.hashCode()
        override fun deepCopy(): HideKeyboard = copy(name = "" + name)
    }
    data class ShowKeyboard(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        override var priority: Int,
        val positionType: Click.PositionType,
        val position: Point? = null,
        val onConditionId: Identifier? = null,
        val offset: Point? = null,
    ) : Action() {
        override fun isComplete(): Boolean =
            super.isComplete() &&
                    ((positionType == Click.PositionType.USER_SELECTED && position != null) ||
                            positionType == Click.PositionType.ON_DETECTED_CONDITION)

        override fun hashCodeNoIds(): Int =
            name.hashCode() + positionType.hashCode() + position.hashCode() + onConditionId.hashCode() + offset.hashCode()

        override fun deepCopy(): ShowKeyboard = copy(name = "" + name)
    }

    /** Text typing and raw key events */
    data class TypeText(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        override var priority: Int,
        val text: String? = null,
    ) : Action() {
        override fun isComplete(): Boolean = super.isComplete() && text != null
        override fun hashCodeNoIds(): Int = name.hashCode() + text.hashCode()
        override fun deepCopy(): TypeText = copy(name = "" + name)
    }

    data class KeyEvent(
        override val id: Identifier,
        override val eventId: Identifier,
        override val name: String? = null,
        override var priority: Int,
        val codes: List<Int>? = null,                    // Android keycodes
        val intervalMs: Long? = 50,
    ) : Action() {
        override fun isComplete(): Boolean = super.isComplete() && codes != null && intervalMs != null
        override fun hashCodeNoIds(): Int = name.hashCode() + codes.hashCode() + intervalMs.hashCode()
        override fun deepCopy(): KeyEvent = copy(name = "" + name)
    }
}
