/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.actions.text

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TextExecutor @Inject constructor() {

    fun writeText(service: AccessibilityService, text: String, validateInput: Boolean) {
        // Find the view to write on
        val focusedItem = service.findFocusInput() ?: let {
            Log.d(TAG, "Cannot write text, no focused item found")
            return
        }

        // Write the text
        if (!focusedItem.writeText(text)) {
            Log.d(TAG, "Cannot write text, focused item can't be written on")
            return
        }

        // Nothing to validate? We can stop here
        if (validateInput && !focusedItem.validateInput()) {
            Log.d(TAG, "Cannot validate text input, focused item can't be validated")
            return
        }
    }
}

private fun AccessibilityService.findFocusInput(): AccessibilityNodeInfo? =
    findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

private fun AccessibilityNodeInfo.writeText(textToWrite: String): Boolean =
    performAction(
        AccessibilityNodeInfo.ACTION_SET_TEXT,
        Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToWrite)
        }
    )

private fun AccessibilityNodeInfo.validateInput(): Boolean {
    // Recent API just need to request ENTER
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false

    // Check if validation is supported by the view
    val actionImeEnter = AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER
    if (!actionList.contains(actionImeEnter)) return false

    return performAction(actionImeEnter.id)
}

private const val TAG = "TextExecutor"