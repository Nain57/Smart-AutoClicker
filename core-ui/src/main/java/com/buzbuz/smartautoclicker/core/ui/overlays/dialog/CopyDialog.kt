/*
 * Copyright (C) 2022 Kevin Buzeau
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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.StyleRes
import androidx.appcompat.widget.SearchView

import com.buzbuz.smartautoclicker.core.ui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.databinding.DialogBaseCopyBinding

abstract class CopyDialog(
    context: Context,
    @StyleRes theme: Int,
) : OverlayDialogController(context, theme) {

    /** ViewBinding containing the views for this dialog. */
    protected lateinit var viewBinding: DialogBaseCopyBinding
    /** The resource id for the dialog title. */
    protected abstract val titleRes: Int
    /** The resource id for the text displayed when there is nothing to copy. */
    protected abstract val emptyRes: Int

    final override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseCopyBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(titleRes)
                buttonDismiss.setOnClickListener { destroy() }

                search.apply {
                    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?) = false
                        override fun onQueryTextChange(newText: String?): Boolean {
                            onSearchQueryChanged(newText)
                            return true
                        }
                    })
                    setOnSearchClickListener {
                        dialogTitle.visibility = View.GONE
                        buttonDismiss.visibility = View.GONE
                    }
                    setOnCloseListener {
                        dialogTitle.visibility = View.VISIBLE
                        buttonDismiss.visibility = View.VISIBLE
                        false
                    }
                }
            }

            layoutLoadableList.setEmptyText(emptyRes)
        }

        return viewBinding.root
    }

    abstract fun onSearchQueryChanged(newText: String?)
}
