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

import android.os.Build
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw

import com.buzbuz.smartautoclicker.core.ui.bindings.setTextOrGone
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeTitleAndDescriptionBinding


internal fun IncludeTitleAndDescriptionBinding.setTitle(titleText: String) {
    title.setTextOrGone(titleText)
}

internal fun IncludeTitleAndDescriptionBinding.setupDescriptions(descriptions: List<String>) {
    if (descriptions.isEmpty()) {
        description.setTextOrGone(null)
        return
    }

    description.visibility = View.VISIBLE
    description.doOnLayout {
        var maxLinesCount = 0
        descriptions.forEach { descriptionText ->
            maxLinesCount = maxOf(maxLinesCount, description.getTextLineCount(descriptionText))
        }

        description.doOnPreDraw {
            description.setLines(maxLinesCount)
        }

        val oldState = description.tag as? DescriptionsState
        val descriptionIndex = oldState?.displayedIndex ?: 0
        if (descriptionIndex in descriptions.indices) description.text = descriptions[descriptionIndex]

        description.tag = oldState?.copy(descriptions = descriptions, maxLinesCount = maxLinesCount)
            ?: DescriptionsState(descriptions, null, maxLinesCount)
    }
}

internal fun IncludeTitleAndDescriptionBinding.setDescription(text: String?) {
    description.setTextOrGone(text)
}

internal fun IncludeTitleAndDescriptionBinding.setDescription(index: Int) {
    val state = description.tag as? DescriptionsState
    if (state == null) {
        description.tag = DescriptionsState(emptyList(), index, null)
        return
    }

    if (index !in state.descriptions.indices) return

    state.maxLinesCount?.let(description::setLines)
    description.text = state.descriptions[index]
    description.tag = state.copy(displayedIndex = index)
}

private fun TextView.getTextLineCount(textToShow: CharSequence): Int {
    if (width == 0 || layout == null) {
        Log.w(TAG, "Can't get text line count, layout width is 0")
        return 0
    }

    return getStaticLayout(textToShow).lineCount
}

private fun TextView.getStaticLayout(textToShow: CharSequence): StaticLayout = StaticLayout.Builder
    .obtain(textToShow, 0, textToShow.length, layout.paint, getTextAreaWidth())
    .setAlignment(layout.alignment)
    .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
    .setIncludePad(includeFontPadding)
    .setBreakStrategy(breakStrategy)
    .setHyphenationFrequency(hyphenationFrequency)
    .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setJustificationMode(justificationMode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setUseLineSpacingFromFallbacks(isFallbackLineSpacing)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setTextDirection((textDirectionHeuristic as TextDirectionHeuristic?)!!)
    }
    .build()


private fun TextView.getTextAreaWidth(): Int =
    width - getCompoundPaddingLeft() - getCompoundPaddingRight()


private data class DescriptionsState(
    val descriptions: List<String> = emptyList(),
    val displayedIndex: Int? = null,
    val maxLinesCount: Int? = null,
)

private const val TAG = "TitleAndDescription"