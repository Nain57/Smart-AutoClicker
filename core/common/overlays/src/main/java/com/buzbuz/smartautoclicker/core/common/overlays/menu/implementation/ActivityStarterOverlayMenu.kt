
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup

import com.buzbuz.smartautoclicker.core.base.extensions.safeStartActivity
import com.buzbuz.smartautoclicker.core.common.overlays.databinding.OverlayMenuBackToPreviousBinding
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.common.overlays.R


open class ActivityStarterOverlayMenu(
    private val intent: Intent,
    private val fallbackIntent: Intent? = null,
) : OverlayMenu() {

    private var cannotStart: Boolean = false

    override fun onCreate() {
        super.onCreate()

        if (context.safeStartActivity(intent)) return

        val fallback = fallbackIntent
        if (fallback != null && context.safeStartActivity(fallback)) return

        Log.e(TAG, "Can't start any of the activities")
        cannotStart = true
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        OverlayMenuBackToPreviousBinding.inflate(layoutInflater).root

    override fun onMenuItemClicked(viewId: Int) {
        if (viewId == R.id.btn_back) back()
    }
}

private const val TAG = "ActivityStarterOverlayMenu"