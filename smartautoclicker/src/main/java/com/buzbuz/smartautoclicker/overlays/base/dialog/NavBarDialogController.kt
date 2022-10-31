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
package com.buzbuz.smartautoclicker.overlays.base.dialog

import android.content.Context
import android.content.res.Configuration
import android.view.Gravity
import android.view.LayoutInflater

import androidx.annotation.CallSuper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Lifecycle

import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogBaseNavBarBinding
import com.buzbuz.smartautoclicker.databinding.ViewBottomNavBarBinding
import com.buzbuz.smartautoclicker.databinding.IncludeDialogNavigationTopBarBinding
import com.buzbuz.smartautoclicker.overlays.base.bindings.DialogNavigationButton

import com.google.android.material.bottomsheet.BottomSheetDialog

abstract class NavBarDialogController(
    context: Context,
) : OverlayDialogController(context) {

    /** Map of navigation bar item id to their content view. */
    private val contentMap: MutableMap<Int, NavBarDialogContent> = mutableMapOf()

    /** */
    private lateinit var baseViewBinding: DialogBaseNavBarBinding
    /** */
    private lateinit var navBarViewBinding: ViewBottomNavBarBinding
    /** */
    protected lateinit var topBarBinding: IncludeDialogNavigationTopBarBinding
    /** */
    private lateinit var dialogCoordinatorLayout: CoordinatorLayout

    /** */
    abstract val navigationMenuId: Int
    /** */
    abstract fun onCreateContent(navItemId: Int): NavBarDialogContent
    /** */
    abstract fun onDialogButtonPressed(buttonType: DialogNavigationButton)

    override fun onCreateDialog(): BottomSheetDialog {
        baseViewBinding = DialogBaseNavBarBinding.inflate(LayoutInflater.from(context)).apply {

            layoutTopBar.apply {
                buttonSave.setOnClickListener { handleButtonClick(DialogNavigationButton.SAVE) }
                buttonDismiss.setOnClickListener { handleButtonClick(DialogNavigationButton.DISMISS) }
                buttonDelete.setOnClickListener { handleButtonClick(DialogNavigationButton.DELETE) }
            }
        }

        navBarViewBinding = ViewBottomNavBarBinding.inflate(LayoutInflater.from(context)).apply {
            root.apply {
                inflateMenu(navigationMenuId)
                setOnItemSelectedListener { item ->
                    updateContentView(item.itemId)
                    true
                }
            }
        }

        topBarBinding = baseViewBinding.layoutTopBar

        return BottomSheetDialog(context).apply {
            setContentView(baseViewBinding.root)
        }
    }

    @CallSuper
    override fun onDialogCreated(dialog: BottomSheetDialog) {
        dialogCoordinatorLayout = (baseViewBinding.root.parent.parent as CoordinatorLayout)
        if (screenMetrics.orientation == Configuration.ORIENTATION_PORTRAIT) addBottomNavigationView()

        updateContentView(
            itemId = navBarViewBinding.root.selectedItemId,
            forceUpdate = true,
        )
    }

    override fun onStart() {
        super.onStart()
        contentMap[navBarViewBinding.root.selectedItemId]?.resume()
    }

    override fun onOrientationChanged() {
        if (screenMetrics.orientation == Configuration.ORIENTATION_PORTRAIT) {
            addBottomNavigationView()
        } else {
            removeBottomNavigationView()
        }
    }

    override fun onStop() {
        super.onStop()
        contentMap[navBarViewBinding.root.selectedItemId]?.pause()
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        contentMap.values.forEach { content ->
            content.destroy()
        }
    }

    /**
     *
     */
    private fun updateContentView(itemId: Int, forceUpdate: Boolean = false) {
        if (!forceUpdate && navBarViewBinding.root.selectedItemId == itemId) return

        // Get the current content and stop it, if any.
        contentMap[navBarViewBinding.root.selectedItemId]?.apply {
            pause()
            stop()
        }

        // Get new content. If it does not exist yet, create it.
        var content = contentMap[itemId]
        if (content == null) {
            content = createContentView(itemId)
            contentMap[itemId] = content
        }

        content.start()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) content.resume()
    }

    private fun createContentView(itemId: Int): NavBarDialogContent =
        onCreateContent(itemId).apply {
            create(this@NavBarDialogController, baseViewBinding.dialogContent, itemId)
        }

    private fun handleButtonClick(buttonType: DialogNavigationButton) {
        // First notify the contents.
        contentMap.values.forEach { contentInfo ->
            contentInfo.onDialogButtonClicked(buttonType)
        }

        // Then, notify the dialog
        onDialogButtonPressed(buttonType)
    }

    private fun addBottomNavigationView() {
        dialogCoordinatorLayout.addView(
            navBarViewBinding.root,
            CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.BOTTOM
            }
        )
    }

    private fun removeBottomNavigationView() {
        dialogCoordinatorLayout.removeView(navBarViewBinding.root)
    }
}
