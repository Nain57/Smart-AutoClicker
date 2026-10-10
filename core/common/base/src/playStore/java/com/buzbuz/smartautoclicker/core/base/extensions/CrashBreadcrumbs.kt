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
package com.buzbuz.smartautoclicker.core.base.extensions

import com.google.firebase.crashlytics.FirebaseCrashlytics


fun logCrashBreadcrumb(message: String) {
    getCrashlyticsOrNull()?.log(message)
}

fun setCrashKey(key: String, value: String) {
    getCrashlyticsOrNull()?.setCustomKey(key, value)
}

/** Breadcrumbs must never crash the caller, e.g. in unit tests where Firebase is not initialized. */
private fun getCrashlyticsOrNull(): FirebaseCrashlytics? =
    try {
        FirebaseCrashlytics.getInstance()
    } catch (_: IllegalStateException) {
        null
    }
