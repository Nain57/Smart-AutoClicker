
package com.buzbuz.smartautoclicker.feature.notifications.common

import android.os.Build
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.feature.notifications.R

@DrawableRes
internal fun notificationIconResId(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.drawable.ic_notification_vector
    else R.drawable.ic_action_notification