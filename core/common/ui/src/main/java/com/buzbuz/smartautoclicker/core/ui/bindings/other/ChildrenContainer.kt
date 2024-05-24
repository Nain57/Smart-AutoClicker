/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.bindings.other

import android.view.View
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeChildrenContainerBinding


fun IncludeChildrenContainerBinding.setIcons(iconIds: List<Int>) {
    if (iconIds.size !in 1..3) throw IllegalArgumentException("Container Children should have 1 to 3 entries")

    iconLeft.setImageResource(iconIds[0])

    if (iconIds.size > 1) {
        iconMiddle.visibility = View.VISIBLE
        textMiddle.visibility = View.VISIBLE
        iconMiddle.setImageResource(iconIds[1])
    } else {
        iconMiddle.visibility = View.GONE
        textMiddle.visibility = View.GONE
    }

    if (iconIds.size > 2) {
        iconRight.visibility = View.VISIBLE
        textRight.visibility = View.VISIBLE
        iconRight.setImageResource(iconIds[2])
    } else {
        iconRight.visibility = View.GONE
        textRight.visibility = View.GONE
    }
}

fun IncludeChildrenContainerBinding.setTexts(texts: List<String>) {
    if (texts.size !in 1..3) throw IllegalArgumentException("Container Children should have 1 to 3 entries")

    textLeft.text = texts[0]
    textMiddle.text = texts[1]
    textRight.text = texts[2]
}