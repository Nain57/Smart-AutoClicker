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

import android.net.Uri
import com.buzbuz.smartautoclicker.core.android.intent.AndroidIntentApi
import kotlin.reflect.KProperty0

internal fun List<KProperty0<Int>>.toAndroidIntentFlags(): List<AndroidIntentApi<Int>> =
    map { flagProperty ->
        AndroidIntentApi(
            value = flagProperty.get(),
            displayName = flagProperty.name.toFlagName(),
            helpUri = flagProperty.getFlagDocumentationUri(),
        )
    }

private fun String.toFlagName(): String =
    removePrefix(PREFIX_INTENT_FLAG_VARIABLE_NAME)
        .replace("_", " ")

private fun KProperty0<Int>.getFlagDocumentationUri(): Uri =
    Uri.parse("$PREFIX_ANDROID_DOCUMENTATION_INTENT_FLAGS_URL$name")

private const val PREFIX_INTENT_FLAG_VARIABLE_NAME = "FLAG_"
private const val PREFIX_ANDROID_DOCUMENTATION_INTENT_FLAGS_URL =
    "https://developer.android.com/reference/android/content/Intent#"