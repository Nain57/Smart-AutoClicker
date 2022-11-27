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
package com.buzbuz.smartautoclicker.overlays.base.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import androidx.appcompat.widget.SearchView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogBaseCopyBinding
import com.buzbuz.smartautoclicker.overlays.base.bindings.setEmptyText
import com.buzbuz.smartautoclicker.overlays.base.utils.setIconTint

abstract class CopyDialog(
    context: Context,
) : OverlayDialogController(context, R.style.AppTheme) {

    /** ViewBinding containing the views for this dialog. */
    protected lateinit var viewBinding: DialogBaseCopyBinding

    final override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseCopyBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_copy_from)
                buttonDismiss.setOnClickListener { destroy() }

                search.apply {
                    findViewById<ImageView>(androidx.appcompat.R.id.search_button).setIconTint(R.color.overlayViewPrimary)
                    findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn).setIconTint(R.color.overlayViewPrimary)

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

            layoutLoadableList.setEmptyText(R.string.message_empty_copy)
        }

        return viewBinding.root
    }

    abstract fun onSearchQueryChanged(newText: String?)
}
