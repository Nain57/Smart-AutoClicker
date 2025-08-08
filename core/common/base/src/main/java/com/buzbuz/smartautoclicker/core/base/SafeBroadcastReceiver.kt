
package com.buzbuz.smartautoclicker.core.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat


abstract class SafeBroadcastReceiver(private val intentFilter: IntentFilter) : BroadcastReceiver() {

    private var registrationContext: Context? = null
    protected val isRegistered: Boolean
        get() = registrationContext != null

    fun register(context: Context, exported: Boolean = true) {
        registrationContext = context
        ContextCompat.registerReceiver(
            context,
            this,
            intentFilter,
            if (exported) ContextCompat.RECEIVER_EXPORTED else ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        onRegistered(context)
    }

    fun unregister() {
        try {
            registrationContext?.unregisterReceiver(this)
        } catch (iaEx: IllegalArgumentException) {
            return
        }

        registrationContext = null
        onUnregistered()
    }

    protected open fun onRegistered(context: Context): Unit = Unit
    protected open fun onUnregistered(): Unit = Unit
}