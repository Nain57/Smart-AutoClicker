
package com.buzbuz.smartautoclicker.core.android.intent

import android.net.Uri
import com.buzbuz.smartautoclicker.core.android.intent.actions.getAllBroadcastActions

import com.buzbuz.smartautoclicker.core.android.intent.actions.getAndroidAPIStartActivityIntentActions
import com.buzbuz.smartautoclicker.core.android.intent.actions.toAndroidIntentActions
import com.buzbuz.smartautoclicker.core.android.intent.flags.getAndroidAPIBroadcastIntentFlags
import com.buzbuz.smartautoclicker.core.android.intent.flags.getAndroidAPIStartActivityIntentFlags
import com.buzbuz.smartautoclicker.core.android.intent.flags.toAndroidIntentFlags

data class AndroidIntentApi<Value>(
    val value: Value,
    val displayName: String,
    val helpUri: Uri,
)

fun getBroadcastReceptionIntentActions(): List<AndroidIntentApi<String>> =
    getAllBroadcastActions().toAndroidIntentActions()

fun getStartActivityIntentActions(): List<AndroidIntentApi<String>> =
    getAndroidAPIStartActivityIntentActions().toAndroidIntentActions()

fun getStartActivityIntentFlags(): List<AndroidIntentApi<Int>> =
    getAndroidAPIStartActivityIntentFlags().toAndroidIntentFlags()

fun getBroadcastIntentFlags(): List<AndroidIntentApi<Int>> =
    getAndroidAPIBroadcastIntentFlags().toAndroidIntentFlags()