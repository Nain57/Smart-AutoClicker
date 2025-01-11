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