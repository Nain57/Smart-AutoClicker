
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