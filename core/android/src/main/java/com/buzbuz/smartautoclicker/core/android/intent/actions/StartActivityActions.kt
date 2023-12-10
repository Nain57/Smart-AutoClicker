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
package com.buzbuz.smartautoclicker.core.android.intent.actions

import android.content.Intent
import android.os.Build
import kotlin.reflect.KProperty0

internal fun getAndroidAPIStartActivityIntentActions(): List<KProperty0<String>> = buildList {
    add(Intent::ACTION_ANSWER)
    add(Intent::ACTION_ALL_APPS)
    add(Intent::ACTION_APPLICATION_PREFERENCES)
    add(Intent::ACTION_ASSIST)
    add(Intent::ACTION_BUG_REPORT)
    add(Intent::ACTION_CALL_BUTTON)
    add(Intent::ACTION_CHOOSER)
    add(Intent::ACTION_CREATE_DOCUMENT)
    add(Intent::ACTION_CREATE_SHORTCUT)
    add(Intent::ACTION_DEFAULT)
    add(Intent::ACTION_DELETE)
    add(Intent::ACTION_DIAL)
    add(Intent::ACTION_EDIT)
    add(Intent::ACTION_FACTORY_TEST)
    add(Intent::ACTION_GET_CONTENT)
    add(Intent::ACTION_INSERT)
    add(Intent::ACTION_INSERT_OR_EDIT)
    add(Intent::ACTION_MAIN)
    add(Intent::ACTION_MANAGE_NETWORK_USAGE)
    add(Intent::ACTION_OPEN_DOCUMENT)
    add(Intent::ACTION_OPEN_DOCUMENT_TREE)
    add(Intent::ACTION_PASTE)
    add(Intent::ACTION_PICK)
    add(Intent::ACTION_PICK_ACTIVITY)
    add(Intent::ACTION_POWER_USAGE_SUMMARY)
    add(Intent::ACTION_PROCESS_TEXT)
    add(Intent::ACTION_QUICK_CLOCK)
    add(Intent::ACTION_QUICK_VIEW)
    add(Intent::ACTION_RUN)
    add(Intent::ACTION_SEARCH)
    add(Intent::ACTION_SEND)
    add(Intent::ACTION_SEARCH_LONG_PRESS)
    add(Intent::ACTION_SENDTO)
    add(Intent::ACTION_SEND_MULTIPLE)
    add(Intent::ACTION_SET_WALLPAPER)
    add(Intent::ACTION_SHOW_APP_INFO)
    add(Intent::ACTION_VIEW)
    add(Intent::ACTION_VOICE_COMMAND)
    add(Intent::ACTION_WEB_SEARCH)

    // Sdk 34
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        add(Intent::ACTION_CREATE_NOTE)
    }

    // Sdk 33
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Intent::ACTION_SAFETY_CENTER)
    }

    // Sdk 31
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Intent::ACTION_MANAGE_UNUSED_APPS)
        add(Intent::ACTION_VIEW_PERMISSION_USAGE_FOR_PERIOD)
    }

    // Sdk 30
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        add(Intent::ACTION_CREATE_REMINDER)
    }

    // Sdk 29
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(Intent::ACTION_DEFINE)
        add(Intent::ACTION_VIEW_LOCUS)
        add(Intent::ACTION_VIEW_PERMISSION_USAGE)
    }

    // Sdk 27
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        add(Intent::ACTION_INSTALL_FAILURE)
    }

    // Sdk 26
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        add(Intent::ACTION_CARRIER_SETUP)
    }
}

