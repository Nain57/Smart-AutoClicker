/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.baseui.dialog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

/**
 * Set a view as custom title for the dialog and set its title name.
 * In order to work correctly, the provided layout must provide a [TextView] with the id [android.R.id.title].
 *
 * @param titleViewRes the resource identifier for the view to inflate as the title.
 * @param title the resource identifier for the text to use as title of the dialog.
 */
fun AlertDialog.Builder.setCustomTitle(@LayoutRes titleViewRes: Int, @StringRes title: Int): AlertDialog.Builder {
    @SuppressLint("InflateParams") // Dialog views have no parent at inflation time
    val titleView = context.getSystemService(LayoutInflater::class.java)!!.inflate(titleViewRes, null)
    titleView.findViewById<TextView>(android.R.id.title).setText(title)
    setCustomTitle(titleView)
    return this
}

/**
 * Set a view as custom title for the dialog and set its title name.
 * In order to work correctly, the provided layout must provide a [TextView] with the id [android.R.id.title].
 *
 * @param titleViewRes the resource identifier for the view to inflate as the title.
 * @param title the resource identifier for the text to use as title of the dialog.
 */
fun AlertDialog.Builder.setCustomTitle(
    @LayoutRes titleViewRes: Int,
    @StringRes title: Int,
    @DrawableRes icon: Int,
    @ColorRes tint: Int = -1,
): AlertDialog.Builder {
    @SuppressLint("InflateParams") // Dialog views have no parent at inflation time
    val titleView = context.getSystemService(LayoutInflater::class.java)!!.inflate(titleViewRes, null)
    titleView.findViewById<TextView>(android.R.id.title).setText(title)
    titleView.findViewById<ImageView>(android.R.id.icon).apply {
        visibility = View.VISIBLE
        setImageResource(icon)
        setColorFilter(ContextCompat.getColor(context, tint), android.graphics.PorterDuff.Mode.SRC_IN)
    }
    setCustomTitle(titleView)
    return this
}