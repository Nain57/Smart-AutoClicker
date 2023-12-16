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
import kotlin.reflect.KProperty0

internal fun getAllBroadcastActions(): List<KProperty0<String>> = buildList {
    add(Intent::ACTION_AIRPLANE_MODE_CHANGED) //s
    add(Intent::ACTION_APPLICATION_LOCALE_CHANGED) //s
    add(Intent::ACTION_APPLICATION_RESTRICTIONS_CHANGED) //s
    add(Intent::ACTION_BOOT_COMPLETED) //s
    add(Intent::ACTION_BATTERY_CHANGED) //s
    add(Intent::ACTION_BATTERY_LOW) //s
    add(Intent::ACTION_BATTERY_OKAY) //s
    add(Intent::ACTION_CAMERA_BUTTON)
    add(Intent::ACTION_CONFIGURATION_CHANGED)//s
    add(Intent::ACTION_DATE_CHANGED)//s
    add(Intent::ACTION_DOCK_EVENT)//s
    add(Intent::ACTION_DREAMING_STARTED)//s
    add(Intent::ACTION_DREAMING_STOPPED)//s
    add(Intent::ACTION_EXTERNAL_APPLICATIONS_AVAILABLE)//s
    add(Intent::ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)//s
    add(Intent::ACTION_GET_RESTRICTION_ENTRIES)
    add(Intent::ACTION_HEADSET_PLUG)//s
    add(Intent::ACTION_INPUT_METHOD_CHANGED)//s
    add(Intent::ACTION_LOCALE_CHANGED)//s
    add(Intent::ACTION_LOCKED_BOOT_COMPLETED)//s
    add(Intent::ACTION_MANAGE_PACKAGE_STORAGE)//s
    add(Intent::ACTION_MANAGED_PROFILE_ADDED)//s
    add(Intent::ACTION_MANAGED_PROFILE_AVAILABLE)//s
    add(Intent::ACTION_MANAGED_PROFILE_REMOVED)//s
    add(Intent::ACTION_MANAGED_PROFILE_UNAVAILABLE)//s
    add(Intent::ACTION_MANAGED_PROFILE_UNLOCKED)//s
    add(Intent::ACTION_MEDIA_BAD_REMOVAL)//s
    add(Intent::ACTION_MEDIA_BUTTON)
    add(Intent::ACTION_MEDIA_CHECKING)//s
    add(Intent::ACTION_MEDIA_EJECT)
    add(Intent::ACTION_MEDIA_MOUNTED)//s
    add(Intent::ACTION_MEDIA_NOFS)//s
    add(Intent::ACTION_MEDIA_REMOVED)//s
    add(Intent::ACTION_MEDIA_SCANNER_FINISHED)//s
    add(Intent::ACTION_MEDIA_SCANNER_STARTED)//s
    add(Intent::ACTION_MEDIA_SHARED)//s
    add(Intent::ACTION_MEDIA_UNMOUNTABLE)//s
    add(Intent::ACTION_MEDIA_UNMOUNTED)//s
    add(Intent::ACTION_MY_PACKAGE_REPLACED)//s
    add(Intent::ACTION_MY_PACKAGE_SUSPENDED)//s
    add(Intent::ACTION_MY_PACKAGE_UNSUSPENDED)//s
    add(Intent::ACTION_PACKAGE_ADDED)//s
    add(Intent::ACTION_PACKAGE_CHANGED)//s
    add(Intent::ACTION_PACKAGE_DATA_CLEARED)//s
    add(Intent::ACTION_PACKAGE_FIRST_LAUNCH)//s
    add(Intent::ACTION_PACKAGE_FULLY_REMOVED)//s
    add(Intent::ACTION_PACKAGE_NEEDS_VERIFICATION)//s
    add(Intent::ACTION_PACKAGE_REMOVED)//s
    add(Intent::ACTION_PACKAGE_REPLACED)//s
    add(Intent::ACTION_PACKAGE_RESTARTED)//s
    add(Intent::ACTION_PACKAGE_VERIFIED)//s
    add(Intent::ACTION_PACKAGES_SUSPENDED)//s
    add(Intent::ACTION_PACKAGES_UNSUSPENDED)//s
    add(Intent::ACTION_POWER_CONNECTED)//s
    add(Intent::ACTION_POWER_DISCONNECTED)//s
    add(Intent::ACTION_PROFILE_ACCESSIBLE)//s
    add(Intent::ACTION_PROFILE_ADDED)//s
    add(Intent::ACTION_PROFILE_INACCESSIBLE)//s
    add(Intent::ACTION_PROFILE_REMOVED)//s
    add(Intent::ACTION_PROVIDER_CHANGED)//s
    add(Intent::ACTION_REBOOT)//s
    add(Intent::ACTION_SCREEN_OFF)//s
    add(Intent::ACTION_SCREEN_ON)//s
    add(Intent::ACTION_TIMEZONE_CHANGED)//s
    add(Intent::ACTION_TIME_CHANGED)//s
    add(Intent::ACTION_TIME_TICK)//s
    add(Intent::ACTION_UID_REMOVED)//s
    add(Intent::ACTION_USER_BACKGROUND)//s
    add(Intent::ACTION_USER_FOREGROUND)//s
    add(Intent::ACTION_USER_PRESENT)//s
    add(Intent::ACTION_USER_INITIALIZE)//s
    add(Intent::ACTION_USER_UNLOCKED)//s
}