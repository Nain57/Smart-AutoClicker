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
package com.buzbuz.smartautoclicker.core.android.intent.actions

import android.content.Intent
import kotlin.reflect.KProperty0

internal fun getAllBroadcastActions(): List<KProperty0<String>> = buildList {
    add(Intent::ACTION_AIRPLANE_MODE_CHANGED)
    add(Intent::ACTION_APPLICATION_LOCALE_CHANGED)
    add(Intent::ACTION_APPLICATION_RESTRICTIONS_CHANGED)
    add(Intent::ACTION_BOOT_COMPLETED)
    add(Intent::ACTION_BATTERY_CHANGED)
    add(Intent::ACTION_BATTERY_LOW)
    add(Intent::ACTION_BATTERY_OKAY)
    add(Intent::ACTION_CAMERA_BUTTON)
    add(Intent::ACTION_CONFIGURATION_CHANGED)
    add(Intent::ACTION_DATE_CHANGED)
    add(Intent::ACTION_DOCK_EVENT)
    add(Intent::ACTION_DREAMING_STARTED)
    add(Intent::ACTION_DREAMING_STOPPED)
    add(Intent::ACTION_EXTERNAL_APPLICATIONS_AVAILABLE)
    add(Intent::ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)
    add(Intent::ACTION_GET_RESTRICTION_ENTRIES)
    add(Intent::ACTION_HEADSET_PLUG)
    add(Intent::ACTION_INPUT_METHOD_CHANGED)
    add(Intent::ACTION_LOCALE_CHANGED)
    add(Intent::ACTION_LOCKED_BOOT_COMPLETED)
    add(Intent::ACTION_MANAGE_PACKAGE_STORAGE)
    add(Intent::ACTION_MANAGED_PROFILE_ADDED)
    add(Intent::ACTION_MANAGED_PROFILE_AVAILABLE)
    add(Intent::ACTION_MANAGED_PROFILE_REMOVED)
    add(Intent::ACTION_MANAGED_PROFILE_UNAVAILABLE)
    add(Intent::ACTION_MANAGED_PROFILE_UNLOCKED)
    add(Intent::ACTION_MEDIA_BAD_REMOVAL)
    add(Intent::ACTION_MEDIA_BUTTON)
    add(Intent::ACTION_MEDIA_CHECKING)
    add(Intent::ACTION_MEDIA_EJECT)
    add(Intent::ACTION_MEDIA_MOUNTED)
    add(Intent::ACTION_MEDIA_NOFS)
    add(Intent::ACTION_MEDIA_REMOVED)
    add(Intent::ACTION_MEDIA_SCANNER_FINISHED)
    add(Intent::ACTION_MEDIA_SCANNER_STARTED)
    add(Intent::ACTION_MEDIA_SHARED)
    add(Intent::ACTION_MEDIA_UNMOUNTABLE)
    add(Intent::ACTION_MEDIA_UNMOUNTED)
    add(Intent::ACTION_MY_PACKAGE_REPLACED)
    add(Intent::ACTION_MY_PACKAGE_SUSPENDED)
    add(Intent::ACTION_MY_PACKAGE_UNSUSPENDED)
    add(Intent::ACTION_PACKAGE_ADDED)
    add(Intent::ACTION_PACKAGE_CHANGED)
    add(Intent::ACTION_PACKAGE_DATA_CLEARED)
    add(Intent::ACTION_PACKAGE_FIRST_LAUNCH)
    add(Intent::ACTION_PACKAGE_FULLY_REMOVED)
    add(Intent::ACTION_PACKAGE_NEEDS_VERIFICATION)
    add(Intent::ACTION_PACKAGE_REMOVED)
    add(Intent::ACTION_PACKAGE_REPLACED)
    add(Intent::ACTION_PACKAGE_RESTARTED)
    add(Intent::ACTION_PACKAGE_VERIFIED)
    add(Intent::ACTION_PACKAGES_SUSPENDED)
    add(Intent::ACTION_PACKAGES_UNSUSPENDED)
    add(Intent::ACTION_POWER_CONNECTED)
    add(Intent::ACTION_POWER_DISCONNECTED)
    add(Intent::ACTION_PROFILE_ACCESSIBLE)
    add(Intent::ACTION_PROFILE_ADDED)
    add(Intent::ACTION_PROFILE_INACCESSIBLE)
    add(Intent::ACTION_PROFILE_REMOVED)
    add(Intent::ACTION_PROVIDER_CHANGED)
    add(Intent::ACTION_REBOOT)
    add(Intent::ACTION_SCREEN_OFF)
    add(Intent::ACTION_SCREEN_ON)
    add(Intent::ACTION_TIMEZONE_CHANGED)
    add(Intent::ACTION_TIME_CHANGED)
    add(Intent::ACTION_TIME_TICK)
    add(Intent::ACTION_UID_REMOVED)
    add(Intent::ACTION_USER_BACKGROUND)
    add(Intent::ACTION_USER_FOREGROUND)
    add(Intent::ACTION_USER_PRESENT)
    add(Intent::ACTION_USER_INITIALIZE)
    add(Intent::ACTION_USER_UNLOCKED)
}