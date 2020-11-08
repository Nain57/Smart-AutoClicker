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
package com.buzbuz.smartautoclicker.baseui.utils

import android.view.View
import android.view.WindowManager

import org.junit.Assert.assertNotNull

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

/**
 * Holder class for the parameters of [WindowManager.addView].
 * @param view the view added to the window manager
 * @param params the layout params for this view.
 */
data class WmAddedView(
    val view: View,
    val params: WindowManager.LayoutParams
)

/**
 * Capture the parameters for a single call to [WindowManager.addView].
 * @param mockWindowManager the window manager that will add the views
 * @return the [WindowManager.addView] method parameters.
 */
fun captureWindowManagerAddedMenuView(mockWindowManager: WindowManager) : WmAddedView {
    val wmAddViewCaptor = ArgumentCaptor.forClass(View::class.java)
    val wmAddViewParamsCaptor = ArgumentCaptor.forClass(WindowManager.LayoutParams::class.java)
    verify(mockWindowManager).addView(wmAddViewCaptor.capture(), wmAddViewParamsCaptor.capture())

    assertNotNull("Can't get menu view, it is null", wmAddViewCaptor.value)
    assertNotNull("Can't get menu view layout params, they are null", wmAddViewParamsCaptor.value)

    return WmAddedView(wmAddViewCaptor.value, wmAddViewParamsCaptor.value)
}

/**
 * Capture the parameters for two calls to [WindowManager.addView].
 * @param mockWindowManager the window manager that will add the views
 * @return the [WindowManager.addView] method parameters.
 */
fun captureWindowManagerAddedViews(mockWindowManager: WindowManager) : Pair<WmAddedView, WmAddedView> {
    val wmAddViewCaptor = ArgumentCaptor.forClass(View::class.java)
    val wmAddViewParamsCaptor = ArgumentCaptor.forClass(WindowManager.LayoutParams::class.java)
    verify(mockWindowManager, times(2))
        .addView(wmAddViewCaptor.capture(), wmAddViewParamsCaptor.capture())

    assertNotNull("Can't get menu view, it is null", wmAddViewCaptor.allValues[0])
    assertNotNull("Can't get menu view layout params, they are null", wmAddViewParamsCaptor.allValues[0])
    assertNotNull("Can't get overlay view, it is null", wmAddViewCaptor.allValues[1])
    assertNotNull("Can't get overlay view layout params, they are null", wmAddViewParamsCaptor.allValues[1])

    return WmAddedView(wmAddViewCaptor.allValues[0], wmAddViewParamsCaptor.allValues[0]) to
            WmAddedView(wmAddViewCaptor.allValues[1], wmAddViewParamsCaptor.allValues[1])
}
