/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.overlays.dialog

import android.content.res.Configuration
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Lifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.databinding.DialogBaseNavBarBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeCreateCopyButtonsBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeDialogNavigationTopBarBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.ViewBottomNavBarBinding

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationBarView

abstract class NavBarDialog(@StyleRes theme: Int) : OverlayDialog(theme) {

    /** Map of navigation bar item id to their content view. */
    private val contentMap: MutableMap<Int, NavBarDialogContent> = mutableMapOf()

    /** */
    private lateinit var baseViewBinding: DialogBaseNavBarBinding

    /** */
    protected lateinit var navBarView: NavigationBarView
    /** */
    lateinit var createCopyButtons: IncludeCreateCopyButtonsBinding
    /** */
    lateinit var topBarBinding: IncludeDialogNavigationTopBarBinding

    /** */
    abstract val navigationMenuId: Int
    /** */
    abstract fun onCreateContent(navItemId: Int): NavBarDialogContent
    /** */
    abstract fun onDialogButtonPressed(buttonType: DialogNavigationButton)
    /** */
    open fun onContentViewChanged(navItemId: Int) = Unit

    override fun onCreateView(): ViewGroup {
        baseViewBinding = DialogBaseNavBarBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                buttonSave.setOnClickListener { handleButtonClick(DialogNavigationButton.SAVE) }
                buttonDismiss.setOnClickListener { handleButtonClick(DialogNavigationButton.DISMISS) }
                buttonDelete.setOnClickListener { handleButtonClick(DialogNavigationButton.DELETE) }
            }
        }
        topBarBinding = baseViewBinding.layoutTopBar

        // In portrait, we need to inject the navigation view as a child of the dialog's CoordinatorLayout in order to
        // correctly handle the dialog scrolling behaviour without moving the navigation view from the bottom.
        // This issue does not occurs in landscape mode, as the NavigationBar is replaced by a NavigationRail, which
        // is sticky to the dialog start.
        if (displayMetrics.orientation == Configuration.ORIENTATION_PORTRAIT) {
            navBarView = ViewBottomNavBarBinding.inflate(LayoutInflater.from(context)).root
            createCopyButtons = IncludeCreateCopyButtonsBinding.inflate(LayoutInflater.from(context))
        } else {
            navBarView = baseViewBinding.navBar
                ?: throw IllegalStateException("Landscape layout must contains a NavigationRailView")
            createCopyButtons = baseViewBinding.createCopyButtons
                ?: throw IllegalStateException("Landscape layout must contains a create copy buttons")
        }

        // Generic setup of the navigation
        navBarView.apply {
            inflateMenu(navigationMenuId)
            setOnItemSelectedListener { item ->
                updateContentView(item.itemId)
                true
            }
        }

        return baseViewBinding.root
    }

    @CallSuper
    override fun onDialogCreated(dialog: BottomSheetDialog) {
        // Setup dialog views. We need to do it here as it is the first place where the dialog is created and where we
        // can access its views.
        if (displayMetrics.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setupPortraitViews()
        }

        updateContentView(
            itemId = navBarView.selectedItemId,
            forceUpdate = true,
        )
    }

    override fun onStart() {
        super.onStart()
        contentMap[navBarView.selectedItemId]?.resume()
    }

    override fun onStop() {
        super.onStop()
        contentMap[navBarView.selectedItemId]?.pause()
    }

    override fun onDestroy() {
        contentMap.values.forEach { content ->
            content.destroy()
        }
        contentMap.clear()
        super.onDestroy()
    }

    protected fun setMissingInputBadge(navItemId: Int, haveMissingInput: Boolean) {
        navBarView.getOrCreateBadge(navItemId).isVisible = haveMissingInput
    }

    private fun setupPortraitViews() {
        dialogCoordinatorLayout?.apply {
            // Add the navigation bar.
            addView(
                navBarView,
                CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    gravity = Gravity.BOTTOM
                }
            )

            // Add create/copy floating action buttons.
            addView(
                createCopyButtons.root,
                CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                }
            )
        }
    }

    private fun createContentView(itemId: Int): NavBarDialogContent =
        onCreateContent(itemId).apply {
            create(this@NavBarDialog, baseViewBinding.dialogContent, itemId)
        }

    /**
     *
     */
    private fun updateContentView(itemId: Int, forceUpdate: Boolean = false) {
        if (!forceUpdate && navBarView.selectedItemId == itemId) return

        // Get the current content and stop it, if any.
        contentMap[navBarView.selectedItemId]?.apply {
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
        onContentViewChanged(itemId)

        createCopyButtons.root.visibility =
            if (content.createCopyButtonsAreAvailable()) View.VISIBLE
            else View.GONE

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) content.resume()
    }

    internal fun debounceInteraction(interaction: () -> Unit) {
        debounceUserInteraction(interaction)
    }

    private fun handleButtonClick(buttonType: DialogNavigationButton) {
        debounceUserInteraction {
            // First notify the contents.
            contentMap.values.forEach { contentInfo ->
                contentInfo.onDialogButtonClicked(buttonType)
            }

            // Then, notify the dialog
            onDialogButtonPressed(buttonType)
        }
    }
}
