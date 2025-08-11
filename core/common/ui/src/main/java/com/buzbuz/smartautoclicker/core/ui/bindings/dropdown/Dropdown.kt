
package com.buzbuz.smartautoclicker.core.ui.bindings.dropdown

import android.view.View
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeInputFieldDropdownBinding
import com.google.android.material.textfield.TextInputLayout


fun <T: DropdownItem> IncludeInputFieldDropdownBinding.setItems(
    items: List<T>,
    onItemSelected: (T) -> Unit,
    label: String? = null,
    enabled: Boolean = true,
    @DrawableRes disabledIcon: Int? = null,
    onDisabledClick: (() -> Unit)? = null,
    onItemBound: ((T, View?) -> Unit)? = null,
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

    if (enabled) {
        disabledTouchHandler.visibility = View.GONE
    } else {
        onDisabledClick?.let {
            disabledTouchHandler.apply {
                visibility = View.VISIBLE
                setOnClickListener { it() }
            }
        }
    }
}

fun IncludeInputFieldDropdownBinding.setSelectedItem(item: DropdownItem) {
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
