/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.details

import android.view.View
import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ContentDebugReportLoadableListBinding

internal fun ContentDebugReportLoadableListBinding.setEmptyText(
    @StringRes id: Int,
    @StringRes secondaryId: Int? = null,
) {
    emptyText.setText(id)

    if (secondaryId == null) {
        emptySecondary.visibility = View.GONE
    } else {
        emptySecondary.visibility = View.VISIBLE
        emptySecondaryText.setText(secondaryId)
    }
}

internal fun ContentDebugReportLoadableListBinding.updateState(items: Collection<Any>?) {
    loading.visibility = if (items == null) View.VISIBLE else View.GONE
    list.visibility = if (!items.isNullOrEmpty()) View.VISIBLE else View.GONE
    empty.visibility = if (items != null && items.isEmpty()) View.VISIBLE else View.GONE
    fastScroller.visibility = if (!items.isNullOrEmpty()) View.VISIBLE else View.GONE
}
