
package com.buzbuz.smartautoclicker.core.ui.bindings.lists

import android.content.Context

import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.utils.ConditionalDividerItemDecoration

fun newDividerWithoutHeader(context: Context): RecyclerView.ItemDecoration =
    ConditionalDividerItemDecoration(context, DividerItemDecoration.VERTICAL, setOf(R.layout.item_list_header))