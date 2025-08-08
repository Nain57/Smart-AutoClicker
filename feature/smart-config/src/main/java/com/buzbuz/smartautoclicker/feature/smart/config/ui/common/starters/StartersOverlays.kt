
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.starters

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

import com.buzbuz.smartautoclicker.core.base.data.getNotificationSettingsIntent
import com.buzbuz.smartautoclicker.core.base.data.getOpenWebBrowserIntent
import com.buzbuz.smartautoclicker.core.base.data.getOpenWebBrowserPickerIntent
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.ActivityStarterOverlayMenu


internal fun newWebBrowserStarterOverlay(uri: Uri) = ActivityStarterOverlayMenu(
    intent = getOpenWebBrowserIntent(uri),
    fallbackIntent = getOpenWebBrowserPickerIntent(uri),
)

internal fun newRestartMediaProjectionStarterOverlay(context: Context) = ActivityStarterOverlayMenu(
    intent = RestartMediaProjectionActivity.getStartIntent(context),
)

internal fun newNotificationPermissionStarterOverlay(context: Context) = ActivityStarterOverlayMenu(
    intent = RequestNotificationPermissionActivity.getStartIntent(context)
)

@RequiresApi(Build.VERSION_CODES.O)
internal fun newNotificationSettingsStarterOverlay() = ActivityStarterOverlayMenu(
    intent = getNotificationSettingsIntent(),
)