/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.baseui

import android.content.Context
import android.content.res.Configuration

import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper

import com.google.android.material.color.DynamicColors

/**
 * Create a new context theme wrapper from an Android Application Context.
 *
 * As a context not attached to an Activity doesn't receive UI configuration updates or theme information, it can't be
 * used with some Android UI APIs such as a dialog. To fix this, we need to wrap the application context with a
 * [ContextThemeWrapper] and update its configuration manually with the correct UI information.
 *
 * @param applicationContext the Android Application Context.
 * @param theme the theme to use for this context.
 * @param currentOrientation the current device screen orientation.
 */
internal fun newOverlayContextThemeWrapper(
    applicationContext: Context,
    @StyleRes theme: Int,
    currentOrientation: Int,
) : Context = DynamicColors.wrapContextIfAvailable(
    ContextThemeWrapper(applicationContext, theme).apply {
        applyOverrideConfiguration(
            Configuration(applicationContext.resources.configuration).apply {
                orientation = currentOrientation
            }
        )
    }
)