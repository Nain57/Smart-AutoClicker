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
package com.buzbuz.smartautoclicker.core.database.entity

// /!\
// Enums used in the database. Do not rename without a proper migration, as values names are used in Db.
// /!\


/** Type of [EventEntity]. */
enum class EventType {
    /** The conditions of the event are images. */
    IMAGE_EVENT,
    /** The conditions of the event are triggers. */
    TRIGGER_EVENT,
}

/**
 * Type of [ActionEntity].
 * For each type there is a set of values that will be available in the database, all others will always be null. Refers
 * to the [ActionEntity] documentation for values/type association.
 */
enum class ActionType {
    /** A single tap on the screen. */
    CLICK,
    /** A swipe on the screen. */
    SWIPE,
    /** A pause, waiting before the next action. */
    PAUSE,
    /** An Android Intent, allowing to interact with other applications. */
    INTENT,
    /** Toggle the enabled state of an event. */
    TOGGLE_EVENT,
    /** Change the value of a counter. */
    CHANGE_COUNTER,
    /** Send a notification. */
    NOTIFICATION,
}


/**
 * Type of click position for a [ActionType.CLICK].
 * Indicates how to click on the screen for the action.
 */
enum class ClickPositionType {
    /** The user must manually select a position to be clicked. */
    USER_SELECTED,
    /**
     * Click on the detected condition.
     * When the condition operator is AND, click on the condition specified by the user.
     * When the condition operator is OR, click on the condition detected condition.
     */
    ON_DETECTED_CONDITION,
}


/** Types of notification message of a [ActionType.NOTIFICATION]. */
enum class NotificationMessageType {
    /** Display the text defined by [ActionEntity.notificationMessageText]. */
    TEXT,
    /** Display the value of the counter defined by [ActionEntity.notificationMessageCounterName]. */
    COUNTER_VALUE;
}


/**
 * Type of [ConditionEntity].
 * For each type there is a set of values that will be available in the database, all others will always be null. Refers
 * to the [ConditionEntity] documentation for values/type association.
 */
enum class ConditionType {
    /** Condition fulfilled upon broadcast reception. */
    ON_BROADCAST_RECEIVED,
    /** Condition fulfilled upon counter value. */
    ON_COUNTER_REACHED,
    /** Condition fulfilled upon image detected. */
    ON_IMAGE_DETECTED,
    /** Condition fulfilled upon text detected. */
    ON_TEXT_DETECTED,
    /** Toggle the enabled state of an event. */
    ON_TIMER_REACHED,
}


/** Types of counter change of a [ActionType.CHANGE_COUNTER].  */
enum class ChangeCounterOperationType {
    /** Add to the current counter value. */
    ADD,
    /** Remove from the current counter value. */
    MINUS,
    /** Set the counter to a specific value. */
    SET;
}


/**
 * Type of counter comparison for [ConditionEntity] of type [ConditionType.ON_COUNTER_REACHED].
 * For each type there is a set of values that will be available in the database, all others will always be null. Refers
 * to the [CounterComparisonOperation] documentation for values/type association.
 */
enum class CounterComparisonOperation {
    /** The counter value is strictly equals to the value. */
    EQUALS,
    /** The counter value is strictly lower than the value. */
    LOWER,
    /** The counter value is lower or equals to the value */
    LOWER_OR_EQUALS,
    /** The counter value is strictly greater than the value. */
    GREATER,
    /** The counter value is greater or equals to the value. */
    GREATER_OR_EQUALS,
}

/**
 * Type of counter value to apply to the [CounterComparisonOperation] or [ChangeCounterOperationType].
 * This allow to perform either an operation or a comparison with a numeral value or the value contained in another counter.
 */
enum class CounterOperationValueType {
    /** The operand is a numeral value. */
    NUMBER,
    /** The operand is a value reference by another counter. */
    COUNTER,
}


/** The type of manipulation to apply to an event with a [EventToggleEntity]. */
enum class EventToggleType {
    /** Enable the event. Has no effect if the event is already enabled. */
    ENABLE,
    /** Disable the event. Has no effect if the event is already disabled. */
    DISABLE,
    /** Enable the event if it is disabled, disable it if it is enabled. */
    TOGGLE,
}


/** The list of supported types for the value of an [IntentExtraEntity]. */
enum class IntentExtraType {
    BOOLEAN,
    BYTE,
    CHAR,
    DOUBLE,
    INTEGER,
    FLOAT,
    SHORT,
    STRING
}