
package com.buzbuz.smartautoclicker.core.processing.data.processor.state

import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import com.buzbuz.smartautoclicker.core.base.SafeBroadcastReceiver
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

    private val broadcastReceiver = object : SafeBroadcastReceiver(broadcastFilter) {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { intentAction ->
                broadcastsState[intentAction] = true
            }
        }
    }

    fun onProcessingStarted(context: Context) {
        if (broadcastsState.isEmpty()) return
        broadcastReceiver.register(context)
    }

    fun onProcessingStopped() {
        if (broadcastsState.isEmpty()) return
        broadcastReceiver.unregister()
    }

    override fun isBroadcastReceived(condition: TriggerCondition.OnBroadcastReceived): Boolean =
        broadcastsState[condition.intentAction] ?: false

    fun clearReceivedBroadcast() {
        broadcastsState.keys.forEach { key ->
            broadcastsState[key] = false
        }
    }
}