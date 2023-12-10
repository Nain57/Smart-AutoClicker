/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.android.intent.flags

import android.content.Intent
import android.os.Build
import kotlin.reflect.KProperty0

/**
 * Get the list of [Intent] actions defined by the Android SDK that can be used for all Intents
 *
 * @return the list of supported flags for the current Android version.
 */
internal fun getAndroidAPIUtilsIntentFlags(): List<KProperty0<Int>> =
    buildList {
        add(Intent::FLAG_DEBUG_LOG_RESOLUTION)
        add(Intent::FLAG_EXCLUDE_STOPPED_PACKAGES)
        add(Intent::FLAG_FROM_BACKGROUND)
        add(Intent::FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        add(Intent::FLAG_GRANT_PREFIX_URI_PERMISSION)
        add(Intent::FLAG_GRANT_READ_URI_PERMISSION)
        add(Intent::FLAG_GRANT_WRITE_URI_PERMISSION)
        add(Intent::FLAG_INCLUDE_STOPPED_PACKAGES)

        // Sdk 29
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Intent::FLAG_DIRECT_BOOT_AUTO)
        }
    }