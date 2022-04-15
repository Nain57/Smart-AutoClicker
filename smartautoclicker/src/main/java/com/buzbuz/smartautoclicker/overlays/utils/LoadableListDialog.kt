/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.overlays.utils

import android.content.Context
import android.view.View

import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog

import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.MergeLoadableListBinding

/**
 * [OverlayDialogController] implementation for a dialog displaying a list of loadable items.
 * @param context the Android Context for the dialog shown by this controller.
 */
abstract class LoadableListDialog(
    context: Context,
) : OverlayDialogController(context) {

    /** ViewBinding containing the views for the loadable list merge layout. */
    protected lateinit var listBinding: MergeLoadableListBinding

    /** String res id for the case where the list is empty. */
    protected abstract val emptyTextId: Int

    /** @return the root view for the [MergeLoadableListBinding]. */
    protected abstract fun getListBindingRoot(): View

    @CallSuper
    override fun onDialogCreated(dialog: AlertDialog) {
        listBinding = MergeLoadableListBinding.bind(getListBindingRoot()).apply {
            empty.setText(emptyTextId)
        }
    }

    /**
     * Update the view visibility according to the items state.
     * Loading is the initial state.
     *
     * @param items the current items to be displayed in the list.
     */
    protected fun updateLayoutState(items: List<Any>?) {
        listBinding.apply {
            when {
                items == null -> {
                    loading.visibility = View.VISIBLE
                    list.visibility = View.GONE
                    empty.visibility = View.GONE
                }
                items.isEmpty() -> {
                    loading.visibility = View.GONE
                    list.visibility = View.GONE
                    empty.visibility = View.VISIBLE
                }
                else -> {
                    loading.visibility = View.GONE
                    list.visibility = View.VISIBLE
                    empty.visibility = View.GONE
                }
            }
        }
    }
}