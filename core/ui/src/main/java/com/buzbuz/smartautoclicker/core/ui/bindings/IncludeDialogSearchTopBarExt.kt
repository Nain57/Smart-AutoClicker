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
package com.buzbuz.smartautoclicker.core.ui.bindings

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeDialogSearchTopBarBinding


fun IncludeDialogSearchTopBarBinding.setup(@StringRes title: Int, @StringRes searchHint: Int) {
    dialogTitle.setText(title)
    searchEdit.setHint(searchHint)

    buttonSearchCancel.setOnClickListener {
        if (searchEdit.visibility == View.GONE) toSearchMode()
        else toTitleMode()
    }
    toTitleMode()
}

fun IncludeDialogSearchTopBarBinding.setOnTextChangedListener(onSearchTextChanged: (String) -> Unit) {
    searchEdit.doAfterTextChanged { onSearchTextChanged(it.toString()) }
}

fun IncludeDialogSearchTopBarBinding.setOnDismissClickedListener(onDismissClicked: () -> Unit) {
    buttonDismiss.setOnClickListener { onDismissClicked() }
}

private fun IncludeDialogSearchTopBarBinding.toTitleMode() {
    buttonDismiss.visibility = View.VISIBLE
    dialogTitle.visibility = View.VISIBLE
    buttonSearchCancel.setIconResource(R.drawable.abc_ic_search_api_material)

    searchEdit.apply {
        visibility = View.GONE

        context.getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(searchEdit.windowToken, 0)
        clearFocus()
        text?.clear()
    }
}

private fun IncludeDialogSearchTopBarBinding.toSearchMode() {
    buttonDismiss.visibility = View.GONE
    dialogTitle.visibility = View.GONE
    buttonSearchCancel.setIconResource(R.drawable.ic_cancel)

    searchEdit.apply {
        visibility = View.VISIBLE

        requestFocus()
        context.getSystemService(InputMethodManager::class.java)
            .showSoftInput(searchEdit, InputMethodManager.SHOW_IMPLICIT)
    }
}