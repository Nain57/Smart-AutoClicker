
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