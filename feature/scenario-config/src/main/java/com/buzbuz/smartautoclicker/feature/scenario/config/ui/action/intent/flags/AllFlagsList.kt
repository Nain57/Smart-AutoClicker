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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.flags

import android.content.Intent
import android.os.Build
import kotlin.reflect.KProperty0

/**
 * Get the list of [Intent] actions defined by the Android SDK that can be used for a Intent
 * starting an Activity.
 *
 * @return the list of supported flags for the current Android version.
 */
internal fun getAndroidAPIStartActivityIntentFlags(withUtils: Boolean = true): List<KProperty0<Int>> =
    buildList {
        add(Intent::FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        add(Intent::FLAG_ACTIVITY_CLEAR_TASK)
        add(Intent::FLAG_ACTIVITY_CLEAR_TOP)
        add(Intent::FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        add(Intent::FLAG_ACTIVITY_FORWARD_RESULT)
        add(Intent::FLAG_ACTIVITY_LAUNCH_ADJACENT)
        add(Intent::FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        add(Intent::FLAG_ACTIVITY_MULTIPLE_TASK)
        add(Intent::FLAG_ACTIVITY_NEW_DOCUMENT)
        add(Intent::FLAG_ACTIVITY_NEW_TASK)
        add(Intent::FLAG_ACTIVITY_NO_ANIMATION)
        add(Intent::FLAG_ACTIVITY_NO_HISTORY)
        add(Intent::FLAG_ACTIVITY_NO_USER_ACTION)
        add(Intent::FLAG_ACTIVITY_PREVIOUS_IS_TOP)
        add(Intent::FLAG_ACTIVITY_SINGLE_TOP)
        add(Intent::FLAG_ACTIVITY_REORDER_TO_FRONT)
        add(Intent::FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        add(Intent::FLAG_ACTIVITY_RETAIN_IN_RECENTS)
        add(Intent::FLAG_ACTIVITY_TASK_ON_HOME)

        // Sdk 30
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(Intent::FLAG_ACTIVITY_REQUIRE_DEFAULT)
            add(Intent::FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
        }

        // Sdk 28
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(Intent::FLAG_ACTIVITY_MATCH_EXTERNAL)
        }

        if (withUtils) addAll(getAndroidAPIUtilsIntentFlags())
    }

/**
 * Get the list of [Intent] actions defined by the Android SDK that can be used for a Intent
 * sent as a broadcast.
 *
 * @return the list of supported flags for the current Android version.
 */
internal fun getAndroidAPIBroadcastIntentFlags(withUtils: Boolean = true): List<KProperty0<Int>> =
    buildList {
        add(Intent::FLAG_RECEIVER_FOREGROUND)
        add(Intent::FLAG_RECEIVER_NO_ABORT)
        add(Intent::FLAG_RECEIVER_REGISTERED_ONLY)
        add(Intent::FLAG_RECEIVER_REPLACE_PENDING)

        // Sdk 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Intent::FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS)
        }

        if (withUtils) addAll(getAndroidAPIUtilsIntentFlags())
    }

/**
 * Get the list of [Intent] actions defined by the Android SDK that can be used for all Intents
 *
 * @return the list of supported flags for the current Android version.
 */
private fun getAndroidAPIUtilsIntentFlags(): List<KProperty0<Int>> =
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