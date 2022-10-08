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
package com.buzbuz.smartautoclicker.overlays.base

import android.content.Context
import android.view.LayoutInflater

import androidx.lifecycle.Lifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayController
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogBaseBinding

import com.google.android.material.bottomsheet.BottomSheetDialog

abstract class NavBarDialogController(
    context: Context,
) : OverlayDialogController(context), NavBarDialog {

    /** Map of navigation bar item id to their content view. */
    private val contentInfoMap: MutableMap<Int, NavBarContentInfo> = mutableMapOf()
    /** */
    private lateinit var baseViewBinding: DialogBaseBinding

    /** */
    abstract val navigationMenuId: Int
    /** */
    abstract fun onCreateContent(navItemId: Int): NavBarDialogContent
    /** */
    abstract fun onDialogButtonPressed(buttonType: DialogButton)

    override fun onCreateDialog(): BottomSheetDialog {
        baseViewBinding = DialogBaseBinding.inflate(LayoutInflater.from(context)).apply {
            bottomNavigation.apply {
                inflateMenu(R.menu.menu_scenario_config)
                setOnItemSelectedListener { item ->
                    updateContentView(item.itemId)
                    true
                }
            }

            buttonPositive.setOnClickListener { handleButtonClick(DialogButton.SAVE) }
            buttonNegative.setOnClickListener { handleButtonClick(DialogButton.DISMISS) }
            buttonNeutral.setOnClickListener { handleButtonClick(DialogButton.DELETE) }
        }

        updateContentView(
            itemId = baseViewBinding.bottomNavigation.selectedItemId,
            forceUpdate = true,
        )

        return BottomSheetDialog(context).apply {
            setContentView(baseViewBinding.root)
        }
    }

    override fun onStart() {
        super.onStart()
        contentInfoMap[baseViewBinding.bottomNavigation.selectedItemId]?.content?.resume()
    }

    override fun onStop() {
        super.onStop()
        contentInfoMap[baseViewBinding.bottomNavigation.selectedItemId]?.content?.pause()
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        contentInfoMap.values.forEach { contentInfo ->
            contentInfo.content.destroy()
        }
    }

    override fun showSubOverlayController(overlay: OverlayController, hideCurrent: Boolean) {
        showSubOverlay(overlay, hideCurrent)
    }

    override fun setSaveButtonState(contentId: Int, changed: Boolean) {
        var haveChanged = false
        contentInfoMap[contentId]?.let { contentInfo ->
            if (contentInfo.saveEnabled != changed) {
                contentInfo.saveEnabled = changed
                haveChanged = true
            }
        }

        if (haveChanged) updateSaveButtonState()
    }

    protected fun setTitle(title: String) {
        baseViewBinding.dialogTitle.text = title
    }

    private fun updateSaveButtonState() {
        var isEnabled = true
        contentInfoMap.values.forEach { navBarContentInfo ->
            isEnabled = isEnabled && navBarContentInfo.saveEnabled
        }

        baseViewBinding.buttonPositive.isEnabled = isEnabled
    }

    /**
     *
     */
    private fun updateContentView(itemId: Int, forceUpdate: Boolean = false) {
        if (!forceUpdate && baseViewBinding.bottomNavigation.selectedItemId == itemId) return

        // Get the current content and stop it, if any.
        contentInfoMap[baseViewBinding.bottomNavigation.selectedItemId]?.content?.apply {
            pause()
            stop()
        }

        // Get new content. If it does not exist yet, create it.
        var content = contentInfoMap[itemId]?.content
        if (content == null) {
            content = createContentView(itemId)
            contentInfoMap[itemId] = NavBarContentInfo(content)
        }

        content.start()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) content.resume()
    }

    private fun createContentView(itemId: Int): NavBarDialogContent =
        onCreateContent(itemId).apply {
            create(this@NavBarDialogController, baseViewBinding.dialogContent, itemId)
        }

    private fun handleButtonClick(buttonType: DialogButton) {
        // First notify the contents.
        contentInfoMap.values.forEach { contentInfo ->
            contentInfo.content.onDialogButtonClicked(buttonType)
        }

        // Then, notify the dialog
        onDialogButtonPressed(buttonType)
    }
}

private class NavBarContentInfo(
    val content: NavBarDialogContent,
    var saveEnabled: Boolean = true,
)
