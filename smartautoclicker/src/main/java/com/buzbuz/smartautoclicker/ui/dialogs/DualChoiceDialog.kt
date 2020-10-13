/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.ui.dialogs

import android.content.Context
import android.view.View
import androidx.annotation.IntDef
import androidx.appcompat.app.AlertDialog

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.overlays.OverlayDialogController
import com.buzbuz.smartautoclicker.core.extensions.setCustomTitle

import kotlinx.android.synthetic.main.dialog_dual_choice.layout_choice_first
import kotlinx.android.synthetic.main.dialog_dual_choice.layout_choice_second
import kotlinx.android.synthetic.main.include_choice_item.view.text_pre
import kotlinx.android.synthetic.main.include_choice_item.view.text_title

/**
 * [OverlayDialogController] implementation for a dialog displaying a list of two choices to the user.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param title the title of the dialog.
 * @param firstTitle the title for the first choice.
 * @param secondTitle the title for the second choice.
 * @param firstPreText the pre-text (displayed before the title) for the first choice. Can be null.
 * @param firstIcon the icon for the first choice. Can be null.
 * @param secondPreText the pre-text (displayed before the title) for the second choice. Can be null.
 * @param secondIcon the icon for the second choice. Can be null.
 * @param onChoiceSelected the callback to be notified upon user choice selection.
 */
class DualChoiceDialog(
    context: Context,
    private val title: Int,
    private val firstTitle: Int,
    private val secondTitle: Int,
    private val firstPreText: Int?,
    private val firstIcon: Int?,
    private val secondPreText: Int?,
    private val secondIcon: Int?,
    private val onChoiceSelected: (Int) -> Unit
) : OverlayDialogController(context) {

    companion object {
        /** Type for the choices in the dialog. */
        @IntDef(FIRST, SECOND)
        @Retention(AnnotationRetention.SOURCE)
        annotation class Choice
        /** The first choice displayed in the dialog. */
        const val FIRST = 1
        /** The second choice displayed in the dialog. */
        const val SECOND = 2
    }

    override fun onCreateDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, title)
            .setView(R.layout.dialog_dual_choice)
            .setNegativeButton(android.R.string.cancel, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        dialog.apply {
            layout_choice_first.setOnClickListener{ onChoiceClicked(FIRST) }
            updateChoiceDisplay(layout_choice_first, firstTitle, firstPreText, firstIcon)
            layout_choice_second.setOnClickListener{ onChoiceClicked(SECOND) }
            updateChoiceDisplay(layout_choice_second, secondTitle, secondPreText, secondIcon)
        }
    }

    /**
     * Called when the user clicks on a choice.
     * This will notify the provided choice callback and dismiss the dialog.
     *
     * @param choice the choice clicked by the user. Can be any values from [Choice].
     */
    private fun onChoiceClicked(@Choice choice: Int) {
        onChoiceSelected.invoke(choice)
        dismiss()
    }

    /**
     * Update the values displayed for a choice in the dialog.
     *
     * @param choiceLayout the root layout of the choice.
     * @param title the title displayed for the choice.
     * @param preText the pre-text for the choice (the small text before the title).
     *                Can be null to hide the pre-text view.
     * @param icon the icon for the choice. Can be null to hide the icon view.
     */
    private fun updateChoiceDisplay(choiceLayout: View, title: Int, preText: Int? = null, icon: Int ? = null) {
        choiceLayout.apply {
            text_title.setText(title)
            if (preText != null) {
                text_pre.apply {
                    setText(preText)
                    visibility = View.VISIBLE
                }
            } else if (icon != null) {
                text_title.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, R.drawable.ic_chevron, 0)
            }
        }
    }
}