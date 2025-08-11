
package com.buzbuz.smartautoclicker.core.android.intent.flags

import android.content.Intent
import android.os.Build
import kotlin.reflect.KProperty0

/**
 * Get the list of [Intent] actions defined by the Android SDK that can be used for all Intents
 *
 * @return the list of supported flags for the current Android version.
 */
internal fun getAndroidAPIUtilsIntentFlags(): List<KProperty0<Int>> =
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