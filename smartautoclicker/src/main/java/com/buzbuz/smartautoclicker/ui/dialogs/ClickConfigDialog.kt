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
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.clicks.ClickInfo
import com.buzbuz.smartautoclicker.extensions.setLeftRightCompoundDrawables
import com.buzbuz.smartautoclicker.ui.base.DialogController
import com.buzbuz.smartautoclicker.ui.base.DualChoiceDialogController
import com.buzbuz.smartautoclicker.ui.overlays.ClickSelectorMenu
import com.buzbuz.smartautoclicker.ui.overlays.ConditionSelectorMenu

import kotlinx.android.synthetic.main.dialog_click_config.edit_delay_after
import kotlinx.android.synthetic.main.dialog_click_config.edit_name
import kotlinx.android.synthetic.main.dialog_click_config.layout_condition_operator
import kotlinx.android.synthetic.main.dialog_click_config.list_conditions
import kotlinx.android.synthetic.main.dialog_click_config.text_click_type
import kotlinx.android.synthetic.main.dialog_click_config.text_condition_operator
import kotlinx.android.synthetic.main.dialog_click_config.text_condition_operator_desc
import kotlinx.android.synthetic.main.item_condition.view.image_condition

/**
 * [DialogController] implementation for displaying a click info and allowing the user to edit it.
 *
 * Any changes done on the click by the user will be saved only when the user clicks on the positive button of the
 * dialog. If the dialog is dismissed by any other means, no changes will be kept.
 * If the click info aren't complete (i.e, there is no click conditions, or the click type and position is not defined),
 * the positive button will be disabled, preventing the user to configure an invalid click.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param clickInfo the click to be displayed on the dialog.
 * @param captureSupplier the supplier providing a screen capture for creating a new click condition.
 * @param onConfigurationCompletedCallback the callback to be called in order to save the changes made by the user on
 *                                         the provided click info.
 */
class ClickConfigDialog(
    context: Context,
    private val clickInfo: ClickInfo,
    private val captureSupplier: (Rect, ((Bitmap) -> Unit)) -> Unit,
    private val onConfigurationCompletedCallback: (ClickInfo) -> Unit
) : DialogController() {

    /** Adapter displaying all condition for the click displayed by this dialog. */
    private val conditionsAdapter = ConditionAdapter(::onAddConditionClicked, ::onConditionClicked)

    override val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        .setView(R.layout.dialog_click_config)
        .setPositiveButton(android.R.string.ok, null)
        .setNegativeButton(android.R.string.cancel, null)

    override val dialogTitle: Int = R.string.dialog_click_config_title

    override fun onDialogShown(dialog: AlertDialog) {
        clickInfo.let { click ->
            dialog.apply {
                edit_name.setText(click.name)
                edit_delay_after.setText(click.delayAfterMs.toString())
                text_click_type.setOnClickListener { onConfigureTypeClicked() }
                layout_condition_operator.setOnClickListener { onConfigureOperatorClicked() }
                conditionsAdapter.conditions = ArrayList(click.conditionList)
                list_conditions.adapter = conditionsAdapter
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
        val dialogController = DualChoiceDialogController(
            context!!,
            R.string.dialog_click_type_title,
            R.string.dialog_click_type_single,
            R.string.dialog_click_type_swipe,
            null,
            R.drawable.ic_click,
            null,
            R.drawable.ic_swipe
        ) { choiceClicked ->

            val clickType =
                if (choiceClicked == DualChoiceDialogController.FIRST) ClickInfo.SINGLE else ClickInfo.SWIPE
            showOverlayMenu(ClickSelectorMenu(context!!, clickType) { type, from, to ->
                clickInfo.apply {
                    this.type = type
                    this.from = from
                    this.to = to
                }

                refreshDialogDisplay()
            })
        }

        showSubDialog(dialogController, true)
    }

    /**
     * Called when the user clicks on the click condition operator view.
     * This will open the dialog allowing the user to select the operator to be applied between the click conditions.
     */
    private fun onConfigureOperatorClicked() {
        showSubDialog(DualChoiceDialogController(
            context!!,
            R.string.dialog_condition_operator_title,
            R.string.condition_operator_and_desc,
            R.string.condition_operator_or_desc,
            R.string.condition_operator_and,
            null,
            R.string.condition_operator_or,
            null
        ) { choiceClicked ->
            clickInfo.conditionOperator =
                if (choiceClicked == DualChoiceDialogController.FIRST) ClickInfo.AND else ClickInfo.OR
            refreshDialogDisplay()
        })
    }

    /**
     * Called when the user clicks on the add item in the condition list.
     * This will open the overlay menu for the selection of the area and content for the new click condition. Once
     * selected, the adapter will be refreshed to display the newly selected condition.
     */
    private fun onAddConditionClicked() {
        showOverlayMenu(ConditionSelectorMenu(context!!) { area ->
            captureSupplier.invoke(area) { conditionBitmap ->
                conditionsAdapter.addCondition(area, conditionBitmap)
                refreshDialogDisplay()
            }
        })
    }

    /**
     * Called when the user clicks on a condition in the condition list.
     * This will open the dialog showing the details of the condition and allowing the user to eventually delete it.
     *
     * @param condition the condition to be displayed in the dialog.
     * @param index the index of the condition in the list.
     */
    private fun onConditionClicked(condition: Pair<Rect, Bitmap>, index: Int) {
        showSubDialog(ClickConditionDialog(context!!, condition to index) {
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
            dialog?.apply {
                name = edit_name.text.toString()
                delayAfterMs = edit_delay_after.text.toString().toLong()
            }
            conditionList = conditionsAdapter.conditions!!

            onConfigurationCompletedCallback.invoke(this)
        }

        dismissDialog()
    }

    /** Refresh all values displayed by the dialog. Must be called after any changes made by the user on the click. */
    private fun refreshDialogDisplay() {
        dialog?.apply {
            clickInfo.let { click ->
                text_click_type.apply {
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
                        text_condition_operator.text = context.getString(R.string.condition_operator_and)
                        text_condition_operator_desc.text = context.getString(R.string.condition_operator_and_desc)
                    }
                    ClickInfo.OR -> {
                        text_condition_operator.text = context.getString(R.string.condition_operator_or)
                        text_condition_operator_desc.text = context.getString(R.string.condition_operator_or_desc)
                    }
                }

                changeButtonState(
                    getButton(AlertDialog.BUTTON_POSITIVE),
                    if (click.type != null && conditionsAdapter.conditions!!.isNotEmpty()) View.VISIBLE else View.INVISIBLE,
                    -1) {
                    onOkClicked()
                }
            }
        }
    }

    /**
     * Adapter displaying the conditions for the click displayed by the dialog.
     * Also provide a item displayed in the last position to add a new click condition.
     *
     * @param addConditionClickedListener the listener called when the user clicks on the add item.
     * @param conditionClickedListener the listener called when the user clicks on a condition.
     */
    private class ConditionAdapter(
        private val addConditionClickedListener: () -> Unit,
        private val conditionClickedListener: (Pair<Rect, Bitmap>, Int) -> Unit
    ) : RecyclerView.Adapter<ConditionViewHolder>() {

        /** The list of content bitmap to be shown by this adapter. */
        var conditions: ArrayList<Pair<Rect, Bitmap>>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        /**
         * Add a click condition for the click displayed by the dialog.
         *
         * @param area the area of the condition.
         * @param content the content of the condition.
         */
        fun addCondition(area: Rect, content: Bitmap) {
            conditions?.let {
                it.add(Pair(area, content))
                notifyDataSetChanged()
            }
        }

        /**
         * Remove a click condition for the click displayed by the dialog.
         *
         * @param index the index in the list of the condition to be removed.
         */
        fun removeCondition(index: Int) {
            conditions?.let {
                it.removeAt(index)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int = conditions?.size?.plus(1) ?: 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConditionViewHolder =
            ConditionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_condition, parent, false))

        override fun onBindViewHolder(holder: ConditionViewHolder, position: Int) {
            if (position == itemCount - 1) {
                // The last item is the add item, allowing the user to add a new condition.
                holder.itemView.image_condition.scaleType = ImageView.ScaleType.CENTER
                holder.itemView.image_condition.setImageResource(R.drawable.ic_add)
                holder.itemView.setOnClickListener { addConditionClickedListener.invoke() }
            } else {
                holder.itemView.image_condition.scaleType = ImageView.ScaleType.FIT_CENTER
                holder.itemView.image_condition.setImageBitmap(conditions!![position].second)
                holder.itemView.setOnClickListener { conditionClickedListener.invoke(conditions!![position], position) }
            }
        }
    }

    /**
     * View holder displaying a click condition in the [ConditionAdapter].
     * @param itemView the root view of the item.
     */
    private class ConditionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
