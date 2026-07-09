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
package com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring

enum class MonitoredOverlayType {

    // Menus
    MAIN_MENU,
    CAPTURE_MENU,
    COLOR_CAPTURE_MENU,
    CONDITION_AREA_SELECTOR_MENU,
    SMART_ACTIONS_BRIEF_MENU,
    SCREEN_CONDITIONS_BRIEF_MENU,

    // Scenario & event top-level dialogs
    SCENARIO,
    EVENT,

    // Action config dialogs
    CLICK,
    CLICK_POSITION,
    CLICK_OFFSET,
    SWIPE,
    SWIPE_POSITION,
    PAUSE,
    INTENT,
    TOGGLE_EVENT,
    EVENT_TOGGLES,
    CHANGE_COUNTER,
    NOTIFICATION,
    SYSTEM_ACTION,
    SET_TEXT,
    ACTION_TYPE_SELECTION,
    SMART_ACTIONS_LEGACY,

    // Screen condition config dialogs
    IMAGE_CONDITION,
    COLOR_CONDITION,
    NUMBER_CONDITION,
    TEXT_CONDITION,
    SCREEN_CONDITION_TYPE_SELECTION,
    SCREEN_CONDITION_SELECTION,

    // Trigger condition config dialogs
    BROADCAST_RECEIVED_CONDITION,
    COUNTER_REACHED_CONDITION,
    TIMER_REACHED_CONDITION,
    TRIGGER_CONDITION_LIST,
    TRIGGER_CONDITION_TYPE_SELECTION,

    // Counter dialogs
    COUNTERS_CONFIG,
    COUNTER_CREATION,
    COUNTER_REFERENCE,
    COUNTER_SELECTION,

    // Copy dialogs
    ACTION_COPY,
    CONDITION_COPY,
    EVENT_COPY,
    FIX_EVENTS_COPY,
    FIX_EVENT_CHILDREN_COPY,

    // Intent sub-dialogs
    INTENT_ACTIONS_SELECTION,
    ACTIVITY_SELECTION,
    COMPONENT_SELECTION,
    FLAGS_SELECTION,
    EXTRA_CONFIG,

    // Alphabet / OCR dialogs
    ALPHABET_SELECTION,
    REQUIRED_ALPHABET,
}