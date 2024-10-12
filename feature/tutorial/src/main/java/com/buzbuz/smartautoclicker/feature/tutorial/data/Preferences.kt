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
package com.buzbuz.smartautoclicker.feature.tutorial.data

import android.content.Context
import android.content.SharedPreferences

/** @return the shared preferences for the tutorial. */
internal fun Context.getTutorialPreferences(): SharedPreferences =
    getSharedPreferences(
        TUTORIAL_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

/** @return if the tutorial stop with volume down popup have been shown at least once to the user. */
internal fun SharedPreferences.isStopVolumeDownDontShowAgain(): Boolean = getBoolean(
    PREF_TUTORIAL_STOP_VOL_DOWN_DONT_SHOW_AGAIN,
    false,
)

/** Save a new value for the stop with volume down popup shown. */
internal fun SharedPreferences.Editor.putStopVolumeDownDontShowAgain(enabled: Boolean) : SharedPreferences.Editor =
    putBoolean(PREF_TUTORIAL_STOP_VOL_DOWN_DONT_SHOW_AGAIN, enabled)

/** Tutorial SharedPreference name. */
private const val TUTORIAL_PREFERENCES_NAME = "TutorialPreferences"
/** Tells if the stop with volume down button dialog have been shown at least once.  */
private const val PREF_TUTORIAL_STOP_VOL_DOWN_DONT_SHOW_AGAIN = "Tutorial_Stop_Volume_Down_dont_show_again"