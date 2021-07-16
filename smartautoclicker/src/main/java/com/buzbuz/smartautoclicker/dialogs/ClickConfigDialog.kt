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
package com.buzbuz.smartautoclicker.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.setCustomTitle
import com.buzbuz.smartautoclicker.extensions.setLeftRightCompoundDrawables
import com.buzbuz.smartautoclicker.baseui.overlays.OverlayDialogController
import com.buzbuz.smartautoclicker.database.ClickCondition
import com.buzbuz.smartautoclicker.database.ClickInfo
import com.buzbuz.smartautoclicker.databinding.DialogClickConfigBinding
import com.buzbuz.smartautoclicker.model.DetectorModel
import com.buzbuz.smartautoclicker.overlays.ClickSelectorMenu
import com.buzbuz.smartautoclicker.overlays.ConditionSelectorMenu

/**
 * [OverlayDialogController] implementation for displaying a click info and allowing the user to edit it.
 *
 * Any changes done on the click by the user will be saved only when the user clicks on the positive button of the
 * dialog. If the dialog is dismissed by any other means, no changes will be kept.
 * If the click info aren't complete (i.e, there is no click conditions, or the click type and position is not defined),
 * the positive button will be disabled, preventing the user to configure an invalid click.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param clickInfo the click to be displayed on the dialog.
 */
class ClickConfigDialog(
    context: Context,
    private val clickInfo: ClickInfo
) : OverlayDialogController(context) {

    /** Adapter displaying all condition for the click displayed by this dialog. */
    private val conditionsAdapter = ConditionAdapter(::onAddConditionClicked, ::onConditionClicked)

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogClickConfigBinding

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogClickConfigBinding.inflate(LayoutInflater.from(context))
        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_click_config_title)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDialogCreated(dialog: AlertDialog) {
        clickInfo.let { click ->
            viewBinding.apply {
                root.setOnTouchListener(hideSoftInputTouchListener)
                editName.setText(click.name)
                editName.setSelection(click.name.length)
                editDelayAfter.setText(click.delayAfterMs.toString())
                textClickType.setOnClickListener { onConfigureTypeClicked() }
                layoutConditionOperator.setOnClickListener { onConfigureOperatorClicked() }
                conditionsAdapter.conditions = ArrayList(click.conditionList)
                listConditions.adapter = conditionsAdapter
            }
        }

        refreshDialogDisplay()
    }

    /**
     * Called when the user clicks on the click type view.
     * This will open the dialog allowing the user to select if this click is a single click or a swipe, leading to
     * the opening of the overlay for the selection of the click/swipe position.
     */
    private fun onConfigureTypeClicked() {
        val dialogController = DualChoiceDialog(
            context,
            R.string.dialog_click_type_title,
            R.string.dialog_click_type_single,
            R.string.dialog_click_type_swipe,
            R.drawable.ic_click,
            R.drawable.ic_swipe
        ) { choiceClicked ->

            val clickType = if (choiceClicked == DualChoiceDialog.FIRST) ClickInfo.SINGLE else ClickInfo.SWIPE
            showSubOverlay(ClickSelectorMenu(context, clickType) { type, from, to ->
                clickInfo.apply {
                    this.type = type
                    this.from = from
                    this.to = to
                }

                refreshDialogDisplay()
            }, true)
        }

        showSubOverlay(dialogController, true)
    }

    /**
     * Called when the user clicks on the click condition operator view.
     * This will open the dialog allowing the user to select the operator to be applied between the click conditions.
     */
    private fun onConfigureOperatorClicked() {
        showSubOverlay(DualChoiceDialog(
            context,
            R.string.dialog_condition_operator_title,
            R.string.condition_operator_and_desc,
            R.string.condition_operator_or_desc,
            R.drawable.ic_all_conditions,
            R.drawable.ic_one_condition,
        ) { choiceClicked ->
            clickInfo.conditionOperator =
                if (choiceClicked == DualChoiceDialog.FIRST) ClickInfo.AND else ClickInfo.OR
            refreshDialogDisplay()
        })
    }

    /**
     * Called when the user clicks on the add item in the condition list.
     * This will open the overlay menu for the selection of the area and content for the new click condition. Once
     * selected, the adapter will be refreshed to display the newly selected condition.
     */
    private fun onAddConditionClicked() {
        val conditionSelectorMenu = ConditionSelectorMenu(context) { area, bitmap ->
            conditionsAdapter.addCondition(area, bitmap)
            refreshDialogDisplay()
        }
        showSubOverlay(conditionSelectorMenu, true)
    }

    /**
     * Called when the user clicks on a condition in the condition list.
     * This will open the dialog showing the details of the condition and allowing the user to eventually delete it.
     *
     * @param condition the condition to be displayed in the dialog.
     * @param index the index of the condition in the list.
     */
    private fun onConditionClicked(condition: ClickCondition, index: Int) {
        showSubOverlay(ClickConditionDialog(context, condition to index) {
            conditionsAdapter.removeCondition(it)
            refreshDialogDisplay()
        })
    }

    /**
     * Called when the user clicks on the ok button to close the dialog.
     * This will close the dialog and save all changes made by the user on the click.
     */
    private fun onOkClicked() {
        clickInfo.apply {
            name = viewBinding.editName.text.toString()
            delayAfterMs = viewBinding.editDelayAfter.text.toString().toLong()
            conditionList = conditionsAdapter.getAllConditions()

            if (clickInfo.id == 0L) {
                DetectorModel.get().addClick(clickInfo)
            } else {
                DetectorModel.get().updateClick(clickInfo)
            }
        }

        dismiss()
    }

    /** Refresh all values displayed by the dialog. Must be called after any changes made by the user on the click. */
    private fun refreshDialogDisplay() {
        dialog?.apply {
            clickInfo.let { click ->
                viewBinding.textClickType.apply {
                    when (click.type) {
                        ClickInfo.SINGLE -> {
                            text = context.getString(R.string.dialog_click_config_type_single, click.from?.x,
                                click.from?.y)
                            setLeftRightCompoundDrawables(R.drawable.ic_click, R.drawable.ic_chevron)
                        }
                        ClickInfo.SWIPE -> {
                            text = context.getString(R.string.dialog_click_config_type_swipe, click.from?.x,
                                click.from?.y, click.to!!.x, click.to!!.y)
                            setLeftRightCompoundDrawables(R.drawable.ic_swipe, R.drawable.ic_chevron)
                        }
                        else -> {
                            text = context.getString(R.string.dialog_click_config_type_none)
                            setLeftRightCompoundDrawables(R.drawable.ic_add, R.drawable.ic_chevron)
                        }
                    }
                }

                when (click.conditionOperator) {
                    ClickInfo.AND -> {
                        viewBinding.textConditionOperatorDesc.apply {
                            setLeftRightCompoundDrawables(R.drawable.ic_all_conditions, R.drawable.ic_chevron)
                            text = context.getString(R.string.condition_operator_and)
                        }
                    }
                    ClickInfo.OR -> {
                        viewBinding.textConditionOperatorDesc.apply {
                            setLeftRightCompoundDrawables(R.drawable.ic_one_condition, R.drawable.ic_chevron)
                            text = context.getString(R.string.condition_operator_or)
                        }
                    }
                }

                changeButtonState(
                    getButton(AlertDialog.BUTTON_POSITIVE),
                    if (click.type != null && conditionsAdapter.getAllConditions().isNotEmpty()) View.VISIBLE else View.INVISIBLE
                ) {
                    onOkClicked()
                }
            }
        }
    }
}
