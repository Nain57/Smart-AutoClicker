
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