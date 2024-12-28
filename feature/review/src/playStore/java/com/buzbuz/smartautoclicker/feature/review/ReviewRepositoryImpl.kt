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
package com.buzbuz.smartautoclicker.feature.review

import android.content.Context
import android.content.Intent

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout
import com.buzbuz.smartautoclicker.feature.review.data.ReviewDataSource
import com.buzbuz.smartautoclicker.feature.review.engine.engagement.isUserEngaged
import com.buzbuz.smartautoclicker.feature.review.engine.scheduling.isLastReviewRequestOldEnough
import com.buzbuz.smartautoclicker.feature.review.engine.session.UserSessionsRecorder
import com.buzbuz.smartautoclicker.feature.review.ui.ReviewActivity

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.PrintWriter

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ReviewRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val reviewDataSource: ReviewDataSource,
    private val userSessionsRecorder: UserSessionsRecorder,
) : ReviewRepository, Dumpable {

    private val coroutineScope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val isLastReviewRequestOldEnough: Flow<Boolean> =
        reviewDataSource.lastReviewRequestTimestamp().map { lastReviewTs ->
            isLastReviewRequestOldEnough(context, lastReviewTs)
        }

    private val isUserEngaged: Flow<Boolean> =
        reviewDataSource.lastUserSessions().map { lastSessions ->
            lastSessions.isUserEngaged()
        }

    private val isUserCandidateForReview: StateFlow<Boolean> =
        combine(isLastReviewRequestOldEnough, isUserEngaged) { isOldEnough, isEngaged ->
            isOldEnough && isEngaged
        }.stateIn(coroutineScope, SharingStarted.Eagerly, false)


    override fun isUserCandidateForReview(): Boolean =
        isUserCandidateForReview.value

    override fun getReviewActivityIntent(context: Context): Intent =
        Intent(context, ReviewActivity::class.java)

    override fun onUserSessionStarted(): Unit =
        userSessionsRecorder.startSession()

    override fun onUserSessionStopped(): Unit =
        userSessionsRecorder.stopSession()

    internal fun updateLastReviewRequestTimestamp() {
        coroutineScope.launch {
            reviewDataSource.setLastReviewRequestTimestamp(System.currentTimeMillis())
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        super.dump(writer, prefix)
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.append(contentPrefix)
            .append("- isLastReviewRequestOldEnough=${isLastReviewRequestOldEnough.dumpWithTimeout()}; ")
            .append("isUserEngaged=${isUserEngaged.dumpWithTimeout()}; ")
            .println()
    }
}


