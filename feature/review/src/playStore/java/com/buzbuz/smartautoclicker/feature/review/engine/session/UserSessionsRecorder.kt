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
package com.buzbuz.smartautoclicker.feature.review.engine.session

import android.util.Log

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.feature.review.data.ReviewDataSource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes


@Singleton
internal class UserSessionsRecorder @Inject constructor(
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val reviewDataSource: ReviewDataSource,
) {

    private val coroutineScope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())
    private var ongoingUserSessionStartTs: Long? = null


    fun startSession() {
        if (ongoingUserSessionStartTs != null) {
            Log.w(TAG, "Previous user session was not finished!")
        }

        Log.d(TAG, "User session start")
        ongoingUserSessionStartTs = System.currentTimeMillis()
    }

    fun stopSession() {
        val sessionStartTs = ongoingUserSessionStartTs ?: let {
            Log.w(TAG, "Can't finish user session, no session was started")
            return
        }
        ongoingUserSessionStartTs = null

        val sessionDurationMs = System.currentTimeMillis() - sessionStartTs
        if (sessionDurationMs < MINIMUM_SESSION_DURATION_MS) {
            Log.d(TAG, "User session is too short, skip it")
            return
        }

        Log.d(TAG, "Registering user session")
        coroutineScope.launch {
            reviewDataSource.addUserSession(
                UserSession(sessionStartTs, sessionDurationMs),
            )
        }
    }
}

private val MINIMUM_SESSION_DURATION_MS = 1.minutes.inWholeMilliseconds
private const val TAG = "UserSessionsRecorder"