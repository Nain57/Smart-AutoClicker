package com.buzbuz.smartautoclicker.core.ui.bindings.dropdown

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class DropdownItem(
    @StringRes val title: Int,
    @StringRes val helperText: Int? = null,
    @DrawableRes val icon: Int? = null,
)