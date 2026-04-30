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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
        val focusedItem = service.findTextInputNode() ?: let {
            Log.d(TAG, "Cannot write text, no focused item found")
            return
        }

        // Write the text, if not successful, try to paste it
        if (!focusedItem.writeText(text)) {
            if (!focusedItem.pasteText(service, text)) {
                Log.d(TAG, "Cannot write text, focused item can't be written on")
                return
            }
        }

        // Nothing to validate? We can stop here
        if (validateInput && !focusedItem.validateInput()) {
            Log.d(TAG, "Cannot validate text input, focused item can't be validated")
            return
        }
    }
}

private fun AccessibilityService.findTextInputNode(): AccessibilityNodeInfo? =
    findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.findTextInputNode()
        ?: return null

private fun AccessibilityNodeInfo.findTextInputNode(): AccessibilityNodeInfo? {
    val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
    stack.add(this to 0)

    while (stack.isNotEmpty()) {
        // Process next node in stack
        val (node, depth) = stack.removeLast()

        // Check if it is the focused node
        if (node.isFocused) return node
        if (depth >= FOCUS_FINDER_MAX_DEPTH) continue

        // Push children onto the stack
        for (i in node.childCount - 1 downTo 0) {
            node.getChild(i)?.let { child ->
                stack.add(child to (depth + 1))
            }
        }
    }

    Log.d(TAG, "Can't find focusable view for input within children.")
    return null
}

private fun AccessibilityNodeInfo.writeText(textToWrite: String): Boolean =
    performAction(
        AccessibilityNodeInfo.ACTION_SET_TEXT,
        Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToWrite)
        }
    )

private fun AccessibilityNodeInfo.pasteText(service: AccessibilityService, text: String): Boolean {
    val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return false

    clipboard.setPrimaryClip(ClipData.newPlainText("text", text))

    return performAction(AccessibilityNodeInfo.ACTION_PASTE)
}

private fun AccessibilityNodeInfo.validateInput(): Boolean {
    // Recent API just need to request ENTER
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false

    // Check if validation is supported by the view
    val actionImeEnter = AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER
    if (!actionList.contains(actionImeEnter)) return false

    return performAction(actionImeEnter.id)
}

private const val FOCUS_FINDER_MAX_DEPTH = 10
private const val TAG = "TextExecutor"