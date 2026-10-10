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
package com.buzbuz.smartautoclicker.core.common.overlays.manager

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager

import com.buzbuz.smartautoclicker.core.base.SafeBroadcastReceiver
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Monitor the state of the keyguard on the device.
 * It will always assume the keyguard as displayed when the screen is off.
 */
@Singleton
internal class KeyguardStateDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    private val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val keyguardManager: KeyguardManager = context.getSystemService(KeyguardManager::class.java)
    private val powerManager: PowerManager = context.getSystemService(PowerManager::class.java)

    val isKeyguardVisibleFlow: Flow<Boolean> = context
        .keyguardBroadcastsFlow()
        .map { intent -> keyguardManager.toKeyguardVisibleState(intent) }
        .stateIn(
            scope = ioScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = !powerManager.isInteractive || keyguardManager.isKeyguardLocked,
        )


    fun isKeyguardVisible(): Boolean =
        !powerManager.isInteractive || keyguardManager.isKeyguardLocked

    private fun KeyguardManager.toKeyguardVisibleState(intent: Intent?): Boolean =
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> true
            Intent.ACTION_USER_PRESENT -> false
            Intent.ACTION_SCREEN_ON -> isKeyguardLocked
            else -> isKeyguardVisible()
        }

    private fun Context.keyguardBroadcastsFlow(): Flow<Intent?> =
        callbackFlow {
            val receiver = object : SafeBroadcastReceiver(KEYGUARD_INTENT_FILTER) {
                override fun onReceive(context: Context?, intent: Intent?) {
                    trySend(intent)
                }
            }

            receiver.register(this@keyguardBroadcastsFlow, exported = false)
            trySend(null)
            awaitClose { receiver.unregister() }
        }
}

private val KEYGUARD_INTENT_FILTER = IntentFilter().apply {
    addAction(Intent.ACTION_SCREEN_OFF)     // Received when screen is off
    addAction(Intent.ACTION_SCREEN_ON)      // Received when screen is on
    addAction(Intent.ACTION_USER_PRESENT)   // Received when keyguard is unlocked by user
}
