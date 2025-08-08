
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeFieldEventChildrenBinding


internal fun IncludeFieldEventChildrenBinding.setTitle(
    @StringRes titleRes: Int,
    @StringRes emptyTitleRes: Int,
) {
    title.setText(titleRes)
    title.tag = FieldTitles(titleRes, emptyTitleRes)
}

internal fun IncludeFieldEventChildrenBinding.setEmptyDescription(@StringRes descRes: Int) {
    emptyDescription.setText(descRes)
}

@SuppressLint("ClickableViewAccessibility")
internal fun IncludeFieldEventChildrenBinding.setOnClickListener(listener: (() -> Unit)?) {
    if (listener != null) {
        root.setOnClickListener { listener() }
        list.setEmptySpaceClickListener(listener)
    } else {
        root.setOnClickListener(null)
        list.setEmptySpaceClickListener(null)
    }
}

internal fun <Item> IncludeFieldEventChildrenBinding.setAdapter(adapter: ListAdapter<Item, *>) {
    list.adapter = adapter
}

internal fun <Item> IncludeFieldEventChildrenBinding.setItems(items: List<Item>) {
    if (items.isEmpty()) toEmptyState()
    else toListState(items)
}

private fun IncludeFieldEventChildrenBinding.toEmptyState() {
    (title.tag as? FieldTitles)?.let { titles ->
        title.text = root.context.getText(titles.emptyTitleRes)
    }
    emptyDescription.visibility = View.VISIBLE
    list.visibility = View.GONE
}

private fun <Item> IncludeFieldEventChildrenBinding.toListState(items: List<Item>) {
    (title.tag as? FieldTitles)?.let { titles ->
        title.text = root.context.getText(titles.titleRes)
    }
    emptyDescription.visibility = View.GONE
    list.apply {
        visibility = View.VISIBLE
        getListAdapter<Item>()?.submitList(items)
    }
}

@Suppress("UNCHECKED_CAST")
private fun <Item> RecyclerView.getListAdapter(): ListAdapter<Item, *>? =
    adapter as? ListAdapter<Item, *>


private fun RecyclerView.setEmptySpaceClickListener(listener: (() -> Unit)?) {
    if (listener == null) {
        setOnTouchListener(null)
        setOnClickListener(null)
        return
    }

    setOnClickListener { listener() }

    val clickDetector = GestureDetector(
        context,
        object: GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean = true
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean = true
        }
    )
    setOnTouchListener { v, event ->
        if (clickDetector.onTouchEvent(event)) {
            v.performClick()
            true
        } else false
    }
}

private data class FieldTitles(
    @StringRes val titleRes: Int,
    @StringRes val emptyTitleRes: Int,
)