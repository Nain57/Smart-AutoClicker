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
package com.buzbuz.smartautoclicker.core.ui.bindings.dropdown

import android.view.View
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeDropdownWithSelectorBinding
import com.google.android.material.textfield.TextInputLayout

fun IncludeDropdownWithSelectorBinding.setItems(
    items: List<DropdownItem>,
    onItemSelected: (DropdownItem) -> Unit,
    onSelectorClicked: () -> Unit,
    label: String? = null,
    enabled: Boolean = true,
    @DrawableRes disabledIcon: Int? = null,
    onItemBound: ((DropdownItem, View?) -> Unit)? = null,
) {
    textLayout.apply {
        if (enabled) {
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
        } else {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            disabledIcon?.let { setEndIconDrawable(it) }
        }

        isHintEnabled = label != null
        hint = label
    }

    val dropdownViewMonitor = DropdownViewsMonitor()

    onItemBound?.let { onBoundListener ->
        textField.setOnDismissListener {
            items.forEach { item -> onBoundListener(item, null) }
            dropdownViewMonitor.clearBoundViews()
        }
    }

    textField.setAdapter(
        DropdownAdapter(
            items = items,
            onItemSelected = { selectedItem ->
                textField.dismissDropDown()
                onItemSelected(selectedItem)
            },
            onItemViewStateChanged = { item, view, isBound ->
                when {
                    isBound && dropdownViewMonitor.onViewBound(item, view) -> onItemBound?.invoke(item, view)
                    !isBound && dropdownViewMonitor.onViewUnbound(item, view) -> onItemBound?.invoke(item, null)
                }
            },
        )
    )

    layoutSelectorField.setOnClickListener { onSelectorClicked() }
}

fun IncludeDropdownWithSelectorBinding.setSelectedItem(item: DropdownItem) {
    textField.setText(textField.resources.getString(item.title), false)

    textLayout.apply {
        if (item.helperText != null) {
            isHelperTextEnabled = true
            helperText = resources.getString(item.helperText)
        } else {
            isHelperTextEnabled = false
        }

        if (item.icon != null) setStartIconDrawable(item.icon)
    }
}

fun IncludeDropdownWithSelectorBinding.setSelectorState(state: SelectorState) {
    if (state.isClickable) {
        selectorChevron.visibility = View.VISIBLE
        layoutSelectorField.isClickable = true
    } else {
        selectorChevron.visibility = View.GONE
        layoutSelectorField.isClickable = false
    }

    selectorTitle.text = state.title
    selectorSubtext.apply {
        text = state.subText
        visibility = if (state.subText.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    if (state.iconRes != null) {
        selectorIcon.setImageResource(state.iconRes)
        selectorIcon.visibility = View.VISIBLE
    } else {
        selectorIcon.visibility = View.GONE
    }
}

data class SelectorState(
    val isClickable: Boolean,
    val title: String,
    val subText: String?,
    @DrawableRes val iconRes: Int?,
)