/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.data.processor.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent

import java.util.concurrent.ConcurrentHashMap

interface IBroadcastsState {
    fun isBroadcastReceived(condition: TriggerCondition.OnBroadcastReceived): Boolean
}

internal class BroadcastsState(
    triggerEvents: List<TriggerEvent>,
): IBroadcastsState {

    private val broadcastsState: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap<String, Boolean>().apply {
        triggerEvents.forEach { triggerEvent ->
            triggerEvent.conditions.forEach { triggerCondition ->
                if (triggerCondition is TriggerCondition.OnBroadcastReceived) {
                    put(triggerCondition.intentAction, false)
                }
            }
        }
    }

    private val broadcastFilter: IntentFilter = IntentFilter().apply {
        broadcastsState.keys.forEach(::addAction)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { intentAction ->
                broadcastsState[intentAction] = true
            }
        }
    }

    private var context: Context? = null

    fun onProcessingStarted(context: Context) {
        if (broadcastsState.isEmpty()) return
        this.context = context
        context.registerReceiver(broadcastReceiver, broadcastFilter)
    }

    fun onProcessingStopped() {
        if (broadcastsState.isEmpty()) return
        context?.unregisterReceiver(broadcastReceiver)
        context = null
    }

    override fun isBroadcastReceived(condition: TriggerCondition.OnBroadcastReceived): Boolean =
        broadcastsState[condition.intentAction] ?: false

    fun clearReceivedBroadcast() {
        broadcastsState.keys.forEach { key ->
            broadcastsState[key] = false
        }
    }
}