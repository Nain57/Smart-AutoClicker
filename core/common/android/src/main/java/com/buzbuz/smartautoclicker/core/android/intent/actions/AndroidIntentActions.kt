
package com.buzbuz.smartautoclicker.core.android.intent.actions

import android.net.Uri
import com.buzbuz.smartautoclicker.core.android.intent.AndroidIntentApi
import kotlin.reflect.KProperty0

internal fun List<KProperty0<String>>.toAndroidIntentActions(): List<AndroidIntentApi<String>> =
    map { actionProperty ->
        AndroidIntentApi(
            value = actionProperty.get(),
            displayName = actionProperty.name.toActionName(),
            helpUri = actionProperty.getActionDocumentationUri(),
        )
    }

private fun String.toActionName(): String =
    removePrefix(PREFIX_INTENT_ACTION_VARIABLE_NAME)
        .replace("_", " ")

private fun KProperty0<String>.getActionDocumentationUri(): Uri =
    Uri.parse("$PREFIX_ANDROID_DOCUMENTATION_INTENT_ACTIONS_URL$name")

private const val PREFIX_INTENT_ACTION_VARIABLE_NAME = "ACTION_"
private const val PREFIX_ANDROID_DOCUMENTATION_INTENT_ACTIONS_URL =
    "https://developer.android.com/reference/android/content/Intent#"