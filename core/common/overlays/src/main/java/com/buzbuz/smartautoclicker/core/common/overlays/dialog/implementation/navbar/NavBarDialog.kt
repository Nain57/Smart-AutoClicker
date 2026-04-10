/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar

import android.content.res.Configuration
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager

import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle

import kotlin.math.max

import com.buzbuz.smartautoclicker.core.common.overlays.databinding.DialogBaseNavBarBinding
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeCreateCopyButtonsBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeDialogNavigationTopBarBinding

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationBarView

abstract class NavBarDialog(@StyleRes theme: Int) : OverlayDialog(theme) {

    /** Map of navigation bar item id to their content view. */
    private val contentMap: MutableMap<Int, NavBarDialogContent> = mutableMapOf()

    private var portraitKeyboardListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var lastNavigationBarBottomPx: Int = 0

    private lateinit var baseViewBinding: DialogBaseNavBarBinding
    protected lateinit var navBarView: NavigationBarView
    lateinit var createCopyButtons: IncludeCreateCopyButtonsBinding
    lateinit var topBarBinding: IncludeDialogNavigationTopBarBinding

    abstract fun inflateMenu(navBarView: NavigationBarView)

    abstract fun onCreateContent(navItemId: Int): NavBarDialogContent

    abstract fun onDialogButtonPressed(buttonType: DialogNavigationButton)

    open fun onContentViewChanged(navItemId: Int) = Unit

    override fun applySoftInputMode(window: Window) {
        if (displayConfigManager.displayConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN,
            )
        } else {
            super.applySoftInputMode(window)
        }
    }

    override fun onCreateView(): ViewGroup {
        baseViewBinding = DialogBaseNavBarBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                buttonSave.setDebouncedOnClickListener { handleButtonClick(DialogNavigationButton.SAVE) }
                buttonDismiss.setDebouncedOnClickListener { handleButtonClick(DialogNavigationButton.DISMISS) }
                buttonDelete.setDebouncedOnClickListener { handleButtonClick(DialogNavigationButton.DELETE) }
            }
        }
        topBarBinding = baseViewBinding.layoutTopBar

        navBarView = baseViewBinding.navBar as NavigationBarView
        createCopyButtons = baseViewBinding.createCopyButtons

        navBarView.apply {
            inflateMenu(this)
            setOnItemSelectedListener { item ->
                updateContentView(item.itemId)
                true
            }
        }

        return baseViewBinding.root
    }

    @CallSuper
    override fun onDialogCreated(dialog: BottomSheetDialog) {
        updateContentView(
            itemId = navBarView.selectedItemId,
            forceUpdate = true,
        )
        if (displayConfigManager.displayConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            installPortraitBottomInsetHandling(dialog)
        }
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
        clearPortraitBottomInsetHandling()
        contentMap.values.forEach { content ->
            content.destroy()
        }
        contentMap.clear()
        super.onDestroy()
    }

    protected fun setMissingInputBadge(navItemId: Int, haveMissingInput: Boolean) {
        navBarView.getOrCreateBadge(navItemId).isVisible = haveMissingInput
    }

    /**
     * Do not use translationY on [design_bottom_sheet]: the coordinator clips children and the bottom nav disappears.
     * Pad the sheet [root] instead so the nav stays visible, and the middle [NestedScrollView] gets a real height limit
     * so it can scroll when the keyboard is open.
     */
    private fun installPortraitBottomInsetHandling(dialog: BottomSheetDialog) {
        val decor = dialog.window?.decorView ?: return
        val thresholdPx = (100 * decor.resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
            lastNavigationBarBottomPx = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            applyPortraitRootBottomPadding(decor, thresholdPx)
            insets
        }

        portraitKeyboardListener = ViewTreeObserver.OnGlobalLayoutListener {
            applyPortraitRootBottomPadding(decor, thresholdPx)
        }
        decor.viewTreeObserver.addOnGlobalLayoutListener(portraitKeyboardListener!!)

        ViewCompat.requestApplyInsets(decor)
    }

    private fun applyPortraitRootBottomPadding(decor: View, thresholdPx: Int) {
        if (!::baseViewBinding.isInitialized) return
        val root = baseViewBinding.root
        val r = Rect()
        decor.getWindowVisibleDisplayFrame(r)
        val gapPx = (decor.rootView.height - r.bottom).coerceAtLeast(0)
        val imeInset = ViewCompat.getRootWindowInsets(decor)
            ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        val fromKeyboard = max(
            if (gapPx >= thresholdPx) gapPx else 0,
            imeInset,
        )
        val bottom = if (fromKeyboard > 0) {
            fromKeyboard
        } else {
            max(gapPx, lastNavigationBarBottomPx)
        }
        if (root.paddingBottom != bottom) {
            root.setPadding(0, 0, 0, bottom)
            root.requestLayout()
        }
    }

    private fun clearPortraitBottomInsetHandling() {
        portraitKeyboardListener?.let { listener ->
            dialog?.window?.decorView?.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
        }
        portraitKeyboardListener = null
        dialog?.window?.decorView?.let { ViewCompat.setOnApplyWindowInsetsListener(it, null) }
        if (::baseViewBinding.isInitialized) {
            baseViewBinding.root.setPadding(0, 0, 0, 0)
        }
    }

    private fun createContentView(itemId: Int): NavBarDialogContent =
        onCreateContent(itemId).apply {
            create(this@NavBarDialog, baseViewBinding.dialogContent, itemId)
        }

    private fun updateContentView(itemId: Int, forceUpdate: Boolean = false) {
        if (!forceUpdate && navBarView.selectedItemId == itemId) return

        contentMap[navBarView.selectedItemId]?.apply {
            pause()
            stop()
        }

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
        contentMap.values.forEach { contentInfo ->
            contentInfo.onDialogButtonClicked(buttonType)
        }
        onDialogButtonPressed(buttonType)
    }
}
