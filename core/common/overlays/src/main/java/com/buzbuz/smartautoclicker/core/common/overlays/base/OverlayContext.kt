/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.overlays.base

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.view.Display

import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper

import com.google.android.material.color.DynamicColors

/**
 * Get a new context wrapper for an overlay.
 *
 * This is required because an overlay can be attached to a context without UI configuration changes notification,
 * which can leads to an invalid theming for the dialog, an invalid rotation ...
 *
 * @param parentContext the context of the parent overlay, or the application context for the root one.
 * @param theme the theme declared by the overlay, if any.
 * @param orientation provides the current orientation of the display the overlay is shown on. Only called if a
 *                    theme needs to be applied, as it might not be available for theme less overlays.
 */
internal fun newOverlayContext(parentContext: Context, @StyleRes theme: Int?, orientation: () -> Int): Context {
    val themes = parentContext.getOverlayThemes() + listOfNotNull(theme)

    val baseContext = OverlayWindowManagerContext(
        displayContext = parentContext.createDefaultDisplayContext(),
        windowManagerContext = parentContext,
        themes = themes,
    )
    if (themes.isEmpty()) return baseContext

    val themedContext = ContextThemeWrapper(baseContext, themes.first()).apply {
        applyOverrideConfiguration(
            Configuration(applicationContext.resources.configuration).apply {
                this.orientation = orientation()
            }
        )
    }
    themes.drop(1).forEach { themeResId -> themedContext.theme.applyStyle(themeResId, true) }

    return DynamicColors.wrapContextIfAvailable(themedContext)
}

/**
 * Get the themes applied to the overlay this context belongs to, from the least to the most specific one.
 *
 * The themes of the whole overlay stack must be reapplied on each new overlay context: copying the resolved theme of
 * the parent context with [android.content.res.Resources.Theme.setTo] is not an option, as it only copies the
 * framework attributes when both themes comes from different [android.content.res.Resources] instances (which is the
 * case here, due to the display context and the configuration override) on Android 9 and below.
 */
private fun Context.getOverlayThemes(): List<Int> {
    var currentContext: Context? = this
    while (currentContext is ContextWrapper) {
        if (currentContext is OverlayWindowManagerContext) return currentContext.themes
        currentContext = currentContext.baseContext
    }

    return emptyList()
}

/**
 * Get a context associated with the default display.
 *
 * The views of an overlay can request the display of their context (the text selection floating toolbar does, for
 * instance). As the application context is not associated with any display, such request throws and crashes the
 * application, so the base context of all overlays must be associated with the default display.
 */
private fun Context.createDefaultDisplayContext(): Context {
    val display = getSystemService(DisplayManager::class.java)
        ?.getDisplay(Display.DEFAULT_DISPLAY)
        ?: return this
    return createDisplayContext(display) ?: this
}

/**
 * Context serving the WindowManager of another context.
 *
 * An overlay context is based on a display context, and such context provides its own WindowManager instance, without
 * the accessibility overlay window token AccessibilityService.getSystemService sets on its own one. Adding a
 * TYPE_ACCESSIBILITY_OVERLAY window without that token is rejected with a BadTokenException.
 *
 * AccessibilityService.createDisplayContext only restores that token from Android 11, and only on the context it
 * returns until Android 13, where it started returning a wrapper restoring it for the derived contexts as well. As
 * every overlay derives its context from the one of its parent, the WindowManager of the accessibility service must
 * be kept for the whole overlay stack.
 *
 * @param themes the themes applied to the overlay owning this context, from the least to the most specific one. Kept
 *               here to allow the overlays created from this context to reapply them on their own context.
 */
private class OverlayWindowManagerContext(
    displayContext: Context,
    private val windowManagerContext: Context,
    val themes: List<Int>,
) : ContextWrapper(displayContext) {

    override fun getSystemService(name: String): Any? =
        if (name == WINDOW_SERVICE) windowManagerContext.getSystemService(name)
        else super.getSystemService(name)
}
