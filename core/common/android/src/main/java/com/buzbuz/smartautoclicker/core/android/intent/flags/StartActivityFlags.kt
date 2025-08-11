
package com.buzbuz.smartautoclicker.core.android.intent.flags

import android.content.Intent
import android.os.Build
import kotlin.reflect.KProperty0

/**
 * Get the list of [Intent] actions defined by the Android SDK that can be used for a Intent
 * starting an Activity.
 *
 * @return the list of supported flags for the current Android version.
 */
internal fun getAndroidAPIStartActivityIntentFlags(withUtils: Boolean = true): List<KProperty0<Int>> =
    buildList {
        add(Intent::FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        add(Intent::FLAG_ACTIVITY_CLEAR_TASK)
        add(Intent::FLAG_ACTIVITY_CLEAR_TOP)
        add(Intent::FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        add(Intent::FLAG_ACTIVITY_FORWARD_RESULT)
        add(Intent::FLAG_ACTIVITY_LAUNCH_ADJACENT)
        add(Intent::FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        add(Intent::FLAG_ACTIVITY_MULTIPLE_TASK)
        add(Intent::FLAG_ACTIVITY_NEW_DOCUMENT)
        add(Intent::FLAG_ACTIVITY_NEW_TASK)
        add(Intent::FLAG_ACTIVITY_NO_ANIMATION)
        add(Intent::FLAG_ACTIVITY_NO_HISTORY)
        add(Intent::FLAG_ACTIVITY_NO_USER_ACTION)
        add(Intent::FLAG_ACTIVITY_PREVIOUS_IS_TOP)
        add(Intent::FLAG_ACTIVITY_SINGLE_TOP)
        add(Intent::FLAG_ACTIVITY_REORDER_TO_FRONT)
        add(Intent::FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        add(Intent::FLAG_ACTIVITY_RETAIN_IN_RECENTS)
        add(Intent::FLAG_ACTIVITY_TASK_ON_HOME)

        // Sdk 30
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(Intent::FLAG_ACTIVITY_REQUIRE_DEFAULT)
            add(Intent::FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
        }

        // Sdk 28
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(Intent::FLAG_ACTIVITY_MATCH_EXTERNAL)
        }

        if (withUtils) addAll(getAndroidAPIUtilsIntentFlags())
    }

