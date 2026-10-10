/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.bindings.buttons

import android.content.res.ColorStateList
import androidx.core.view.isVisible

import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeButtonFingerSelectorBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel

import androidx.appcompat.R as AppCompatR

fun IncludeButtonFingerSelectorBinding.setFingerCount(fingerCount: Int) {
    if (fingerCount !in 1..5) return

    var currentCount = 1
    if (fingerTwo.isVisible) currentCount++
    if (fingerThree.isVisible) currentCount++
    if (fingerFour.isVisible) currentCount++
    if (fingerFive.isVisible) currentCount++
    if (currentCount == fingerCount) return

    fingerTwo.isVisible = fingerCount >= 2
    fingerThree.isVisible = fingerCount >= 3
    fingerFour.isVisible = fingerCount >= 4
    fingerFive.isVisible = fingerCount >= 5

    fingerAdd.isVisible = !fingerFive.isVisible
    toggleButton.isSelectionRequired = true
    toggleButton.isSingleSelection = true

    toggleButton.post { applyPositionalShapes() }
}

fun IncludeButtonFingerSelectorBinding.setChecked(fingerIndex: Int, errorState: Map<Int, Boolean>) {
    val newCheckedView = getButton(fingerIndex)
    if (newCheckedView.id != toggleButton.checkedButtonId) {
        toggleButton.check(newCheckedView.id)
    }

    updateButtonErrorColor(fingerOne, errorState[0] ?: false)
    updateButtonErrorColor(fingerTwo, errorState[1] ?: false)
    updateButtonErrorColor(fingerThree, errorState[2] ?: false)
    updateButtonErrorColor(fingerFour, errorState[3] ?: false)
    updateButtonErrorColor(fingerFive, errorState[4] ?: false)

    applyPositionalShapes()
}

fun IncludeButtonFingerSelectorBinding.setOnAddClickedListener(onAddClicked: () -> Unit) {
    fingerAdd.setOnClickListener { onAddClicked() }
}

fun IncludeButtonFingerSelectorBinding.setOnFingerClickedListener(onFingerClicked: (index: Int) -> Unit) {
    toggleButton.addOnButtonCheckedListener { _, _, _ -> applyPositionalShapes() }
    fingerOne.setOnClickListener { onFingerClicked(0) }
    fingerTwo.setOnClickListener { onFingerClicked(1) }
    fingerThree.setOnClickListener { onFingerClicked(2) }
    fingerFour.setOnClickListener { onFingerClicked(3) }
    fingerFive.setOnClickListener { onFingerClicked(4) }
}

private fun IncludeButtonFingerSelectorBinding.getButton(fingerIndex: Int): MaterialButton =
    when (fingerIndex) {
        0 -> fingerOne
        1 -> fingerTwo
        2 -> fingerThree
        3 -> fingerFour
        4 -> fingerFive
        else -> fingerOne
    }

private fun IncludeButtonFingerSelectorBinding.fingerIndex(button: MaterialButton): Int =
    when (button) {
        fingerOne -> 0
        fingerTwo -> 1
        fingerThree -> 2
        fingerFour -> 3
        fingerFive -> 4
        else -> 0
    }

@Suppress("UNCHECKED_CAST")
private fun IncludeButtonFingerSelectorBinding.updateButtonErrorColor(button: MaterialButton, inError: Boolean) {
    if (button.getTag(button.id) == null) {
        button.setTag(button.id, button.textColors to button.iconTint)
    }
    val (originalText, originalIcon) = button.getTag(button.id) as Pair<ColorStateList, ColorStateList?>

    val isChecked = button.id == toggleButton.checkedButtonId
    if (inError && !isChecked) {
        val error = ColorStateList.valueOf(MaterialColors.getColor(button, AppCompatR.attr.colorError))
        button.setTextColor(error)
        button.iconTint = error
        println("TOTO: set Error Color for ${fingerIndex(button)}")
    } else {
        button.setTextColor(originalText)
        button.iconTint = originalIcon
        println("TOTO: set normal Color for ${fingerIndex(button)}")
    }
}

/**
 * MaterialButtonGroup applies a StateListShapeAppearanceModel whose checked state gives every button a full pill,
 * ignoring position. We replace it with a plain ShapeAppearanceModel (no state variation) that matches the button's
 * actual visual position in the row.
 */
private fun IncludeButtonFingerSelectorBinding.applyPositionalShapes() {
    val visibleFingerButtons = buildList {
        add(fingerOne)
        if (fingerTwo.isVisible) add(fingerTwo)
        if (fingerThree.isVisible) add(fingerThree)
        if (fingerFour.isVisible) add(fingerFour)
        if (fingerFive.isVisible) add(fingerFive)
    }

    val fullCorner = RelativeCornerSize(0.5f)
    val addVisible = fingerAdd.isVisible

    visibleFingerButtons.forEachIndexed { index, button ->
        val isFirst = index == 0
        val isLast = index == visibleFingerButtons.lastIndex && !addVisible
        button.shapeAppearanceModel = ShapeAppearanceModel.builder().apply {
            if (isFirst) { setTopLeftCornerSize(fullCorner); setBottomLeftCornerSize(fullCorner) }
            if (isLast) { setTopRightCornerSize(fullCorner); setBottomRightCornerSize(fullCorner) }
        }.build()
    }

    if (addVisible) {
        fingerAdd.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setTopRightCornerSize(fullCorner)
            .setBottomRightCornerSize(fullCorner)
            .build()
    }
}
