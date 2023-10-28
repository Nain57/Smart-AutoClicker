/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.database.utils

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.entity.ToggleEventType


internal data class V10Click(
    val id: Long,
    val evtId: Long,
    val name: String,
    val priority: Int,
    val x: Int?,
    val y: Int?,
    val clickOnCondition: Boolean,
    val pressDuration: Long,
)

internal data class ExpectedV11Click(
    val id: Long,
    val evtId: Long,
    val name: String,
    val priority: Int,
    val x: Int?,
    val y: Int?,
    val clickPositionType: ClickPositionType,
    val clickOnConditionId: Long?,
    val pressDuration: Long,
)

internal fun V10Click.toExpectedV11Click(clickPositionType: ClickPositionType, clickOnConditionId: Long?): ExpectedV11Click =
    ExpectedV11Click(
        id = id,
        evtId = evtId,
        name = name,
        priority = priority,
        x = x,
        y = y,
        pressDuration = pressDuration,
        clickPositionType = clickPositionType,
        clickOnConditionId = clickOnConditionId,
    )

internal fun SupportSQLiteDatabase.insertV10Click(v10Click: V10Click) {
    execSQL(getInsertV10Click(
        id = v10Click.id,
        eventId = v10Click.evtId,
        name = v10Click.name,
        x = v10Click.x,
        y = v10Click.y,
        clickOnCondition = v10Click.clickOnCondition,
        pressDuration = v10Click.pressDuration,
        priority = v10Click.priority,
    ))
}

internal fun Cursor.assertRowIsV11Click(expectedClick: ExpectedV11Click) {
    assertColumnEquals(expectedClick.id, "id")
    assertColumnEquals(expectedClick.evtId, "eventId")
    assertColumnEquals(expectedClick.name, "name")
    assertColumnEquals(expectedClick.priority, "priority")
    assertColumnEquals(expectedClick.pressDuration, "pressDuration")
    assertColumnEquals(expectedClick.x, "x")
    assertColumnEquals(expectedClick.y, "y")
    assertColumnEquals(expectedClick.clickPositionType, "clickPositionType")
    assertColumnEquals(expectedClick.clickOnConditionId, "clickOnConditionId")
}

internal data class ExpectedV10ToV11Swipe(
    val id: Long,
    val evtId: Long,
    val name: String,
    val priority: Int,
    val fromX: Int,
    val fromY: Int,
    val toX: Int,
    val toY: Int,
    val swipeDuration: Long,
)

internal fun SupportSQLiteDatabase.insertV10Swipe(expectedSwipe: ExpectedV10ToV11Swipe) {
    execSQL(getInsertV10Swipe(
        id = expectedSwipe.id,
        eventId = expectedSwipe.evtId,
        name = expectedSwipe.name,
        fromX = expectedSwipe.fromX,
        fromY = expectedSwipe.fromY,
        toX = expectedSwipe.toX,
        toY = expectedSwipe.toY,
        swipeDuration = expectedSwipe.swipeDuration,
        priority = expectedSwipe.priority,
    ))
}

internal fun Cursor.assertRowIsSwipe(expectedSwipe: ExpectedV10ToV11Swipe) {
    assertColumnEquals(expectedSwipe.id, "id")
    assertColumnEquals(expectedSwipe.evtId, "eventId")
    assertColumnEquals(expectedSwipe.name, "name")
    assertColumnEquals(expectedSwipe.priority, "priority")
    assertColumnEquals(expectedSwipe.fromX, "fromX")
    assertColumnEquals(expectedSwipe.fromY, "fromY")
    assertColumnEquals(expectedSwipe.toX, "toX")
    assertColumnEquals(expectedSwipe.toY, "toY")
    assertColumnEquals(expectedSwipe.swipeDuration, "swipeDuration")
}


internal class ExpectedV10ToV11Pause(
    val id: Long,
    val evtId: Long,
    val name: String,
    val priority: Int,
    val pauseDuration: Long,
)

internal fun SupportSQLiteDatabase.insertV10Pause(expectedPause: ExpectedV10ToV11Pause) {
    execSQL(getInsertV10Pause(
        id = expectedPause.id,
        eventId = expectedPause.evtId,
        name = expectedPause.name,
        pauseDuration = expectedPause.pauseDuration,
        priority = expectedPause.priority,
    ))
}

internal fun Cursor.assertRowIsPause(expectedPause: ExpectedV10ToV11Pause) {
    assertColumnEquals(expectedPause.id, "id")
    assertColumnEquals(expectedPause.evtId, "eventId")
    assertColumnEquals(expectedPause.name, "name")
    assertColumnEquals(expectedPause.priority, "priority")
    assertColumnEquals(expectedPause.pauseDuration, "pauseDuration")
}


internal class ExpectedV10ToV11Intent(
    val id: Long,
    val evtId: Long,
    val name: String,
    val priority: Int,
    val isAdvanced: Boolean,
    val isBroadcast: Boolean,
    val action: String,
    val componentName: String,
    val flags: Int,
)

internal fun SupportSQLiteDatabase.insertV10Intent(expectedIntent: ExpectedV10ToV11Intent) {
    execSQL(getInsertV10Intent(
        id = expectedIntent.id,
        eventId = expectedIntent.evtId,
        name = expectedIntent.name,
        advanced = expectedIntent.isAdvanced,
        broadcast = expectedIntent.isBroadcast,
        action = expectedIntent.action,
        comp = expectedIntent.componentName,
        flags = expectedIntent.flags,
        priority = expectedIntent.priority,
    ))
}

internal fun Cursor.assertRowIsIntent(expectedIntent: ExpectedV10ToV11Intent) {
    assertColumnEquals(expectedIntent.id, "id")
    assertColumnEquals(expectedIntent.evtId, "eventId")
    assertColumnEquals(expectedIntent.name, "name")
    assertColumnEquals(expectedIntent.priority, "priority")
    assertColumnEquals(expectedIntent.isAdvanced, "isAdvanced")
    assertColumnEquals(expectedIntent.isBroadcast, "isBroadcast")
    assertColumnEquals(expectedIntent.action, "intent_action")
    assertColumnEquals(expectedIntent.componentName, "component_name")
    assertColumnEquals(expectedIntent.flags, "flags")
}


internal class ExpectedV10ToV11ToggleEvent(
    val id: Long,
    val evtId: Long,
    val name: String,
    val priority: Int,
    val toggleEventId: Long,
    val toggleType: ToggleEventType,
)

internal fun SupportSQLiteDatabase.insertV10ToggleEvent(expectedToggleEvent: ExpectedV10ToV11ToggleEvent) {
    execSQL(getInsertV10ToggleEvent(
        id = expectedToggleEvent.id,
        eventId = expectedToggleEvent.evtId,
        name = expectedToggleEvent.name,
        toggleEventId = expectedToggleEvent.toggleEventId,
        toggleType = expectedToggleEvent.toggleType,
        priority = expectedToggleEvent.priority,
    ))
}

internal fun Cursor.assertRowIsToggleEvent(expectedToggleEvent: ExpectedV10ToV11ToggleEvent) {
    assertColumnEquals(expectedToggleEvent.id, "id")
    assertColumnEquals(expectedToggleEvent.evtId, "eventId")
    assertColumnEquals(expectedToggleEvent.name, "name")
    assertColumnEquals(expectedToggleEvent.priority, "priority")
    assertColumnEquals(expectedToggleEvent.toggleEventId, "toggle_event_id")
    assertColumnEquals(expectedToggleEvent.toggleType, "toggle_type")
}
