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
package com.buzbuz.smartautoclicker.core.android.intent

import android.net.Uri
import com.buzbuz.smartautoclicker.core.android.intent.actions.getAndroidAPIStartActivityIntentActions
import kotlin.reflect.KProperty0


data class AndroidIntentAction(
    val value: String,
    val displayName: String,
    val helpUri: Uri,
)

fun getStartActivityIntentActions(): List<AndroidIntentAction> =
    getAndroidAPIStartActivityIntentActions().map { it.toIntentAction() }

private fun KProperty0<String>.toIntentAction(): AndroidIntentAction =
    AndroidIntentAction(
        value = get(),
        displayName = name.toActionName(),
        helpUri = getActionDocumentationUri(),
    )

private fun String.toActionName(): String =
    removePrefix(PREFIX_INTENT_ACTION_VARIABLE_NAME)
        .replace("_", " ")

private fun KProperty0<String>.getActionDocumentationUri(): Uri =
    Uri.parse("$PREFIX_ANDROID_DOCUMENTATION_INTENT_ACTIONS_URL$name")

private const val PREFIX_INTENT_ACTION_VARIABLE_NAME = "ACTION_"
private const val PREFIX_ANDROID_DOCUMENTATION_INTENT_ACTIONS_URL =
    "https://developer.android.com/reference/android/content/Intent#"