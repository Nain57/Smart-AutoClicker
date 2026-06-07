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
package com.buzbuz.smartautoclicker.core.database.serialization.compat

import com.buzbuz.smartautoclicker.core.base.extensions.getEnum
import com.buzbuz.smartautoclicker.core.base.extensions.getInt
import com.buzbuz.smartautoclicker.core.base.extensions.getLong
import com.buzbuz.smartautoclicker.core.base.extensions.getString
import com.buzbuz.smartautoclicker.core.common.actions.text.appendCounterReference
import com.buzbuz.smartautoclicker.core.common.actions.text.findCounterReferences
import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionType
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.CountersEntity
import com.buzbuz.smartautoclicker.core.database.entity.NotificationMessageType
import kotlinx.serialization.json.JsonObject


/** Deserializer for all Json object version below 20. */
internal open class CompatV20Deserializer : CompatDeserializer() {

    override fun deserializeCompleteScenario(jsonCompleteScenario: JsonObject): CompleteScenario =
        super.deserializeCompleteScenario(jsonCompleteScenario)
            .migrateToCounterTable()

    override fun deserializeCounterConditionValue(jsonCounterCondition: JsonObject): Double =
        jsonCounterCondition.getInt("counterValue")?.toDouble() ?: 0.0

    override fun deserializeCounterActionValue(jsonCounterCondition: JsonObject): Double =
        jsonCounterCondition.getInt("counterOperationValue")?.toDouble() ?: 0.0

    override fun deserializeActionNotification(jsonNotification: JsonObject): ActionEntity? {
        val id = jsonNotification.getLong("id", true) ?: return null
        val eventId = jsonNotification.getLong("eventId", true) ?: return null
        val channelImportance = jsonNotification.getInt("notificationImportance") ?: return null

        val notificationMessageType = jsonNotification
            .getEnum<NotificationMessageType>("notificationMessageType") ?: return null
        val notificationMessageText =
            if (notificationMessageType == NotificationMessageType.COUNTER_VALUE) {
                jsonNotification.getString("notificationMessageCounterName")?.let { counterName ->
                    "$counterName = ".appendCounterReference(counterName)
                }
            } else {
                jsonNotification.getString("notificationMessageText")
            }

        return ActionEntity(
            id = id,
            eventId = eventId,
            name = jsonNotification.getString("name") ?: "",
            priority = jsonNotification.getInt("priority")?.coerceAtLeast(0) ?: 0,
            type = ActionType.NOTIFICATION,
            notificationImportance = channelImportance,
            notificationMessageText = notificationMessageText ?: "",
        )
    }


    private fun CompleteScenario.migrateToCounterTable(): CompleteScenario =
        copy(
            counters = buildMap {
                events.forEach { (event, actions, conditions) ->
                    actions.forEach { actionItem ->
                        putIfValidCounter(event.scenarioId, actionItem.action.counterName)
                        putIfValidCounter(event.scenarioId, actionItem.action.counterOperationCounterName)

                        // We just migrated the format, so it can only contain one counter
                        val counterRefs = actionItem.action.notificationMessageText?.findCounterReferences()
                        if (!counterRefs.isNullOrEmpty()) putIfValidCounter(
                            event.scenarioId,
                            counterRefs[0],
                        )
                    }

                    conditions.forEach { conditionItem ->
                        putIfValidCounter(event.scenarioId, conditionItem.counterName)
                        putIfValidCounter(event.scenarioId, conditionItem.counterOperationCounterName)
                        putIfValidCounter(event.scenarioId, conditionItem.numberCounterOperationCounterName)
                    }
                }
            }.values.toList()
        )

    private fun MutableMap<String, CountersEntity>.putIfValidCounter(scenarioId: Long, name: String?) {
        if (name != null) put(name, CountersEntity(name = name, scenarioId = scenarioId, startingValue = 0.0))
    }
}