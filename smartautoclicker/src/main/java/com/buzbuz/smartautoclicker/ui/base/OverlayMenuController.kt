/*
 * Copyright (C) 2020 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.ui.base

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.view.children

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.TYPE_COMPAT_OVERLAY
import com.buzbuz.smartautoclicker.extensions.displaySize

import kotlinx.android.synthetic.main.overlay_menu.view.layout_buttons

/**
 * Controller for a menu displayed as an overlay shown from a service.
 *
 * This class ensure that all overlay menu opened from a service will have the same behaviour. It provides basic
 * lifecycle alike methods to ease the view initialization/cleaning, as well as a menu item enabling/disabling
 * management and the moving of the menu by pressing the move item. It also provides the management of an overlay view,
 * a view that can be shown/hide as an overlay over the currently displayed activity.
 *
 * The layout being inflated as the menu is defined by the abstract member [menuLayout]. This layout MUST contains a
 * [android.view.ViewGroup] with the id [R.id.layout_buttons] containing all menu items. The two items supported by
 * default are not mandatory and if you not need it, you just have to not declare it in your layout.
 * Two basic menu items management is provided out of the box:
 * - [R.id.btn_move]: the button allowing the move the overlay menu when drag and drop by the user.
 * - [R.id.btn_hide_overlay]: the button allowing to show/hide the overlay view on the screen. When hidden, the user can
 * click on the activity overlaid.
 *
 * The overlay view is defined by the abstract member [screenOverlayView]. This view can be shown/hidden on a press by
 * the user on the [R.id.btn_hide_overlay] button.
 *
 * @param context the Android context to be used to display the overlay menu and view.
 */
abstract class OverlayMenuController(protected val context: Context) {

    private companion object {
        /** Tag for logs. */
        private const val TAG = "OverlayMenuController"
        /** Name of the preference file. */
        private const val PREFERENCE_NAME = "OverlayMenuController"
        /** Preference key referring to the X position of the menu during the last call to [dismiss]. */
        private const val PREFERENCE_MENU_X_KEY = "Menu_X_Position"
        /** Preference key referring to the Y position of the menu during the last call to [dismiss]. */
        private const val PREFERENCE_MENU_Y_KEY = "Menu_Y_Position"
    }

    /** The layout parameters of the menu layout. */
    private val menuLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        TYPE_COMPAT_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT)
    /** The layout parameters of the overlay view. */
    private val overlayLayoutParams:  WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        TYPE_COMPAT_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT)

    /** The shared preference storing the position of the menu in order to save/restore the last user position. */
    private val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    /** The root view of the menu overlay. Inflated from [menuLayoutRes]. */
    private var menuLayout: View? = null
    /** Listener notified upon menu overlay dismissing. Provided when [show] is called. */
    private var dismissListener: (() -> Unit)? = null
    /** The initial position of the overlay menu when pressing the move menu item. */
    private var moveInitialMenuPosition = 0 to 0
    /** The initial position of the touch event that as initiated the move of the overlay menu. */
    private var moveInitialTouchPosition = 0 to 0
    /** The Android window manager. Used to add/remove the overlay menu and view. */
    protected val windowManager = context.getSystemService(WindowManager::class.java)!!

    /** The display size. Used for avoiding moving the menu outside the screen. */
    private val displaySize = windowManager.displaySize

    /** The layout id of the overlay menu to be inflated. Must be a [androidx.annotation.LayoutRes]. */
    protected abstract val menuLayoutRes: Int
    /**
     * The view to be displayed between the current activity and the overlay menu.
     * It can be shown/hidden by pressing on the menu item with the id [R.id.btn_hide_overlay]. If null, pressing this
     * button will have no effect.
     */
    protected abstract val screenOverlayView: View?

    /**
     * Show the overlay menu and view, if defined.
     * Once shown, [onMenuShown] will be called.
     *
     * @param onDismissListener the listener notified open the overlay menu dismissing.
     */
    fun show(onDismissListener: (() -> Unit)? = null) {
        dismissListener = onDismissListener

        Log.d(TAG, "create overlay: $this")

        // First add the overlay, if any. It needs to be below the menu or user won't be able to click on the menu.
        screenOverlayView?.let {
            windowManager.addView(it, overlayLayoutParams)
        }

        // Restore the last menu position, if any.
        menuLayoutParams.gravity = Gravity.TOP or Gravity.START
        if (sharedPreferences.contains(PREFERENCE_MENU_X_KEY) && sharedPreferences.contains(PREFERENCE_MENU_Y_KEY)) {
            menuLayoutParams.x = sharedPreferences.getInt(PREFERENCE_MENU_X_KEY, 0)
            menuLayoutParams.y = sharedPreferences.getInt(PREFERENCE_MENU_Y_KEY, 0)
        }

        // Inflate the menu
        menuLayout = context.getSystemService(LayoutInflater::class.java)!!
            .inflate(menuLayoutRes, null).apply {
                for (view in layout_buttons.children) {
                    @SuppressLint("ClickableViewAccessibility") // View is only drag and drop, no click
                    when (view.id) {
                        R.id.btn_move -> view.setOnTouchListener { _: View, event: MotionEvent -> onMoveTouched(event) }
                        R.id.btn_hide_overlay -> view.setOnClickListener { onHideOverlayClicked() }
                        else -> view.setOnClickListener { v -> onItemClicked(v.id) }
                    }
                }
            }
        windowManager.addView(menuLayout, menuLayoutParams)
        setMenuItemViewEnabled(R.id.btn_hide_overlay, false , true)

        Log.d(TAG, "overlay shown: $this")
        onMenuShown()
    }

    /**
     * Dismiss the overlay menu and view.
     * Once dismissed, [onMenuDismissed] will be called, then the listener provided with the previous [show] call.
     */
    fun dismiss() {
        Log.d(TAG, "dismiss overlay: $this")

        sharedPreferences.edit()
            .putInt(PREFERENCE_MENU_X_KEY, menuLayoutParams.x)
            .putInt(PREFERENCE_MENU_Y_KEY, menuLayoutParams.y)
            .apply()

        screenOverlayView?.let { windowManager.removeView(it) }
        windowManager.removeView(menuLayout)

        onMenuDismissed()

        menuLayout = null
        dismissListener?.invoke()
        dismissListener = null
    }

    /**
     * Set the enabled state of a menu item.
     *
     * @param viewId the view identifier of the menu item to change the state of.
     * @param enabled true to enable the view, false to disable it.
     * @param clickable true to keep the view clickable, false to ignore all clicks on the view. False by default.
     */
    protected fun setMenuItemViewEnabled(@IdRes viewId: Int, enabled: Boolean, clickable: Boolean = false) {
        menuLayout?.findViewById<View>(viewId)?.apply {
            isEnabled = enabled || clickable
            alpha = if (enabled) 1.0f else 0.4f
        }
    }

    /**
     * Set the drawable resource of a menu item.
     *
     * @param viewId the view identifier of the menu item to change the drawable of.
     * @param imageId the identifier of the new drawable.
     */
    protected fun setMenuItemViewImageResource(@IdRes viewId: Int, @DrawableRes imageId: Int) {
        (menuLayout?.findViewById<View>(viewId) as ImageView).setImageResource(imageId)
    }

    /** Called once the overlay menu is shown. */
    protected open fun onMenuShown() {}

    /** Called once the overlay menu is dismissed. */
    protected open fun onMenuDismissed() {}

    /**
     * Called when the user has clicked on a menu item.
     * Will not be called for [R.id.btn_hide_overlay] and [R.id.btn_move].
     *
     * @param viewId the identifier of the view clicked.
     */
    protected abstract fun onItemClicked(@IdRes viewId: Int)

    /**
     * Called when the user clicks on the [R.id.btn_hide_overlay] menu item.
     * Will show/hide the overlay view, allowing the user the click on the Activity bellow the overlays.
     */
    private fun onHideOverlayClicked() {
        screenOverlayView?.apply {
            if (visibility == View.VISIBLE) {
                visibility = View.GONE
                setMenuItemViewEnabled(R.id.btn_hide_overlay, true , true)
            } else {
                visibility = View.VISIBLE
                setMenuItemViewEnabled(R.id.btn_hide_overlay, false , true)
            }
        }
    }

    /**
     * Called when the user touch the [R.id.btn_move] menu item.
     * Handle the long press and move on this button in order to drag and drop the overlay menu on the screen.
     *
     * @param event the touch event occurring on the menu item.
     *
     * @return true if the event is handled, false if not.
     */
    private fun onMoveTouched(event: MotionEvent) : Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                menuLayout!!.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                moveInitialMenuPosition = menuLayoutParams.x to menuLayoutParams.y
                moveInitialTouchPosition = event.rawX.toInt() to event.rawY.toInt()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                setMenuLayoutPosition(
                    moveInitialMenuPosition.first + (event.rawX.toInt() - moveInitialTouchPosition.first),
                    moveInitialMenuPosition.second + (event.rawY.toInt() - moveInitialTouchPosition.second)
                )
                windowManager.updateViewLayout(menuLayout, menuLayoutParams)
                true
            }
            else -> false
        }
    }

    /**
     * Safe setter for the position of the overlay menu ensuring it will not be displayed outside the screen.
     *
     * @param x the horizontal position.
     * @param y the vertical position.
     */
    private fun setMenuLayoutPosition(x: Int, y: Int) {
        menuLayoutParams.x = x.coerceIn(0, displaySize.x - menuLayout!!.width)
        menuLayoutParams.y = y.coerceIn(0, displaySize.y - menuLayout!!.height)
    }
}