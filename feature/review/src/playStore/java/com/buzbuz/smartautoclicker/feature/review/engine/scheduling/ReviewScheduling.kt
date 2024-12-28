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
package com.buzbuz.smartautoclicker.feature.review.engine.scheduling

import android.content.Context


internal fun isLastReviewRequestOldEnough(context: Context, lastRequestTs: Long?): Boolean =
    if (lastRequestTs == null)
        (context.appInstallTimestamp + DELAY_BEFORE_FIRST_REVIEW_REQUEST_MS) < System.currentTimeMillis()
    else
        (lastRequestTs + DELAY_BETWEEN_REVIEW_REQUEST_MS) < System.currentTimeMillis()


private val Context.appInstallTimestamp: Long
    get() = packageManager.getPackageInfo(packageName, 0).firstInstallTime