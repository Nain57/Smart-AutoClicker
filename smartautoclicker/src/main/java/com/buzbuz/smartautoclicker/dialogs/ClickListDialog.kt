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

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.setCustomTitle
import com.buzbuz.smartautoclicker.extensions.setLeftCompoundDrawable
import com.buzbuz.smartautoclicker.baseui.overlays.OverlayDialogController
import com.buzbuz.smartautoclicker.database.ClickInfo
import com.buzbuz.smartautoclicker.model.DetectorModel

import kotlinx.android.synthetic.main.merge_loadable_list.empty
import kotlinx.android.synthetic.main.merge_loadable_list.list
import kotlinx.android.synthetic.main.merge_loadable_list.loading
import kotlinx.android.synthetic.main.item_click.view.btn_action
import kotlinx.android.synthetic.main.item_click.view.name

import java.util.Collections

/**
 * [OverlayDialogController] implementation for displaying a list of clicks.
 *
 * This dialog allows the user to create/copy/edit/reorder the clicks for a scenario. To handle all those cases,
 * several display mode are declared using the [Mode] values. The current mode can be changed through the [mode] member.
 *
 * @param context the Android Context for the dialog shown by this controller.
 */
class ClickListDialog(context: Context) : OverlayDialogController(context) {

    private companion object {

        /** Define the different display mode for the dialog. */
        @IntDef(EDITION, COPY, REORDER)
        @Retention(AnnotationRetention.SOURCE)
        private annotation class Mode
        /**
         * Shown by default when the dialog is displayed. The action button is shown on each item in [ClickListAdapter]
         * and clicking on it will delete the click. Clicking on an item will open the dialog shown by
         * [ClickConfigDialog]. The dialog buttons actions are:
         *  - [AlertDialog.BUTTON_POSITIVE]: The dialog is dismissed.
         *  - [AlertDialog.BUTTON_NEGATIVE]: Goes to reorder mode.
         *  - [AlertDialog.BUTTON_NEUTRAL]: Opens the dialog shown by [DualChoiceDialog] proposing to create
         *  a new click of copy one. If there is no click on the list, it will directly open the dialog shown by
         *  [ClickConditionDialog] (as you can't copy from nothing).
         */
        private const val EDITION = 1
        /**
         * Shown when clicking on reorder. The action button is hide on each item in [ClickListAdapter]. Clicking on an
         * item will open the dialog shown by [ClickConditionDialog], allowing you to modify the copied click.
         * The dialogs buttons are:
         *  - [AlertDialog.BUTTON_POSITIVE]: The clicks order changes are validated. Goes to edition mode.
         *  - [AlertDialog.BUTTON_NEGATIVE]: The clicks order changes are discarded. Goes to edition mode.
         *  - [AlertDialog.BUTTON_NEUTRAL]: The button is hide.
         */
        private const val COPY = 2
        /**
         * Shown when clicking on reorder. The action button show the move icon on each item in [ClickListAdapter]. Long
         * clicking and moving on an item will drag and drop the item in order to allow clicks reordering. The dialogs
         * buttons are:
         *  - [AlertDialog.BUTTON_POSITIVE]: The clicks order changes are validated. Goes to edition mode.
         *  - [AlertDialog.BUTTON_NEGATIVE]: The clicks order changes are discarded. Goes to edition mode.
         *  - [AlertDialog.BUTTON_NEUTRAL]: The button is hide.
         */
        private const val REORDER = 3
    }

    /** Adapter displaying the list of clicks. */
    private lateinit var adapter: ClickListAdapter
    /** TouchHelper applied to [adapter] when in [REORDER] mode allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(ClickReorderTouchHelper())

    /** Display mode for the dialog. Changing this value will automatically update the dialog Ui. */
    @Mode private var mode: Int? = null
        set(value) {
            field = value
            adapter.mode = value
            when (value) {
                EDITION -> toEditionMode()
                COPY -> toCopyMode()
                REORDER -> toReorderMode()
            }
        }

    override fun onCreateDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_click_list_title)
            .setView(R.layout.dialog_click_list)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(R.string.dialog_click_list_reorder, null)
            .setNeutralButton(R.string.dialog_click_list_add, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        DetectorModel.get().apply {
            adapter = ClickListAdapter(::onItemClicked, ::deleteClick)
            scenarioClicks.observe(this@ClickListDialog, ::onClickListChanged)
        }

        dialog.apply {
            list.addItemDecoration(DividerItemDecoration(context,
                DividerItemDecoration.VERTICAL))
            list.adapter = adapter
            empty.setText(R.string.dialog_click_list_no_clicks)
        }
        mode = EDITION
    }

    override fun onVisibilityChanged(visible: Boolean) {
        if (visible) mode = EDITION
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        DetectorModel.get().scenarioClicks.removeObservers(this)
        mode = null
    }

    /**
     * Refresh the list of click displayed by the dialog.
     *
     * @param clicks the new list of clicks.
     */
    private fun onClickListChanged(clicks: List<ClickInfo>?) {
        dialog?.apply {
            loading.visibility = View.GONE
            if (clicks.isNullOrEmpty()) {
                list.visibility = View.GONE
                empty.visibility = View.VISIBLE
            } else {
                list.visibility = View.VISIBLE
                empty.visibility = View.GONE
            }

            adapter.clicks = clicks?.toMutableList()

            // In edition, buttons displays depends on the click count, refresh them
            if (mode == EDITION) {
                toEditionMode()
            }
        }
    }

    /** Change the Ui mode to [EDITION]. */
    private fun toEditionMode() {
        dialog?.apply {
            itemTouchHelper.attachToRecyclerView(null)
            changeButtonState(getButton(AlertDialog.BUTTON_POSITIVE), View.VISIBLE,
                android.R.string.ok) { dismiss() }
            changeButtonState(getButton(AlertDialog.BUTTON_NEGATIVE),
                if (adapter.itemCount > 1) View.VISIBLE else View.INVISIBLE,
                R.string.dialog_click_list_reorder) { mode = REORDER }
            changeButtonState(getButton(AlertDialog.BUTTON_NEUTRAL), View.VISIBLE, R.string.dialog_click_list_add) {
                onAddClicked()
            }
        }
    }

    /** Change the Ui mode to [COPY]. */
    private fun toCopyMode() {
        dialog?.apply {
            itemTouchHelper.attachToRecyclerView(null)
            changeButtonState(getButton(DialogInterface.BUTTON_POSITIVE), View.VISIBLE,
                android.R.string.cancel) { mode = EDITION }
            changeButtonState(getButton(DialogInterface.BUTTON_NEUTRAL), View.GONE)
            changeButtonState(getButton(DialogInterface.BUTTON_NEGATIVE), View.GONE)
        }
    }

    /** Change the Ui mode to [REORDER]. */
    private fun toReorderMode() {
        dialog?.let {
            itemTouchHelper.attachToRecyclerView(it.list)
            changeButtonState(it.getButton(AlertDialog.BUTTON_POSITIVE), View.VISIBLE, android.R.string.ok) {
                DetectorModel.get().updateClicksPriority(adapter.clicks!!)
                mode = EDITION
            }
            changeButtonState(it.getButton(AlertDialog.BUTTON_NEGATIVE), View.VISIBLE, android.R.string.cancel) {
                adapter.cancelReorder()
                mode = EDITION
            }
            changeButtonState(it.getButton(AlertDialog.BUTTON_NEUTRAL), View.GONE)
        }
    }

    /**
     * Called when the user clicks on the "Add" button.
     * According to the current click count for the current scenario, it will directly starts the click configuration
     * dialog to create a new click, or it will display the New/Copy dialog first.
     */
    private fun onAddClicked() {
        if (adapter.itemCount > 0) {
            showSubOverlay(DualChoiceDialog(
                context,
                R.string.dialog_click_add_title,
                R.string.dialog_click_add_create,
                R.string.dialog_click_add_copy,
                R.drawable.ic_add,
                R.drawable.ic_copy
            ) { choiceClicked ->
                when (choiceClicked) {
                    DualChoiceDialog.FIRST -> addClick()
                    DualChoiceDialog.SECOND -> mode = COPY
                }
            })
        } else {
            addClick()
        }
    }

    /**
     * Called when the user clicks on an item.
     *
     * @param click the click corresponding to the clicked item.
     */
    private fun onItemClicked(click: ClickInfo) {
        when (mode) {
            EDITION -> editClick(click)
            COPY -> copyClick(click)
        }
    }

    /** Opens the dialog allowing the user to add a new click. */
    private fun addClick() {
        showSubOverlay(
            ClickConfigDialog(context, ClickInfo(context.getString(R.string.dialog_click_config_name_default))),
            true
        )
    }

    /**
     * Opens the dialog allowing the user to create a new click from an existing one.
     *
     * @param click the click item to be copied.
     */
    private fun copyClick(click: ClickInfo) {
        showSubOverlay(ClickConfigDialog(context, click.copyAsNew()), true)
    }

    /**
     * Opens the dialog allowing the user to edit an existing click.
     *
     * @param click the click item to be edited.
     */
    private fun editClick(click: ClickInfo) {
        showSubOverlay(ClickConfigDialog(context, click.copy()), true)
    }

    /**
     * Adapter displaying a list of clicks.
     *
     * This adapter supports different display mode through it's [mode] member:
     *  - [EDITION]: clicking on an item calls [itemClickedListener], and the delete button is shown.
     *  - [COPY]: clicking on an item calls [itemClickedListener], and the delete button is hidden.
     *  - [REORDER]: user can't clicks on an item, items can be reordered through drag and drop, the delete button is
     *               replaced by the drag and drop icon.
     *
     * @param itemClickedListener listener called when the user clicks on an click item when in [EDITION] or [COPY]
     *                            mode.
     * @param deleteClickedListener listener called when the user clicks on the delete button on a click item.
     */
    private class ClickListAdapter(
        private val itemClickedListener: (ClickInfo) -> Unit,
        private val deleteClickedListener: ((ClickInfo) -> Unit)
    ) : RecyclerView.Adapter<ClickViewHolder>() {

        /** The list of clicks displayed by this adapter. */
        var clicks: List<ClickInfo>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }
        /**
         * Original position of the clicks when entering the [REORDER] mode.
         * If [cancelReorder] is called, this list will be used to restore the original positions before user changes.
         */
        var backupClicks: List<ClickInfo>? = null
        /** Set the Ui mode for the adapter. This will trigger a refresh of the list. */
        @Mode var mode: Int? = null
            set(value) {
                field = value
                backupClicks = if (value == REORDER) clicks?.toMutableList() else null
                notifyDataSetChanged()
            }

        override fun getItemCount(): Int = clicks?.size ?: 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClickViewHolder =
            ClickViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_click, parent, false))

        override fun onBindViewHolder(holder: ClickViewHolder, position: Int) {
            val click = clicks!![position]
            holder.itemView.name.text = click.name
            val drawable = if (click.type == ClickInfo.SINGLE) R.drawable.ic_click else R.drawable.ic_swipe
            holder.itemView.name.setLeftCompoundDrawable(drawable)

            when (mode) {
                EDITION -> {
                    holder.itemView.setOnClickListener { itemClickedListener.invoke(click) }
                    holder.itemView.btn_action.visibility = View.VISIBLE
                    holder.itemView.btn_action.setImageResource(R.drawable.ic_cancel)
                    holder.itemView.btn_action.setOnClickListener { deleteClickedListener.invoke(click) }
                }
                COPY -> {
                    holder.itemView.setOnClickListener { itemClickedListener.invoke(click) }
                    holder.itemView.btn_action.visibility = View.GONE
                }
                REORDER -> {
                    holder.itemView.setOnClickListener(null)
                    holder.itemView.btn_action.visibility = View.VISIBLE
                    holder.itemView.btn_action.setImageResource(R.drawable.ic_drag)
                    holder.itemView.btn_action.setOnClickListener(null)
                }
            }
        }

        /**
         * Swap the position of two clicks in the list.
         * If the ui is not in [REORDER] mode, this method will have no effect.
         *
         * @param from the position of the click to be moved.
         * @param to the new position of the click to be moved.
         */
        fun moveClicks(from: Int, to: Int) {
            if (mode != REORDER) {
                return
            }

            clicks?.let {
                Collections.swap(it, from, to)
                notifyItemMoved(from, to)
            }
        }

        /** Cancel all reordering changes made by the user.  */
        fun cancelReorder() {
            clicks = backupClicks?.toMutableList()
        }
    }

    /**
     * View holder displaying a click in the [ClickListAdapter].
     * @param itemView the root view of the item.
     */
    private class ClickViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    /**
     * ItemTouchHelper attached to the [adapter] when in [REORDER] mode in order for the user to change the order of
     * the clicks.
     */
    private class ClickReorderTouchHelper
        : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            (recyclerView.adapter as ClickListAdapter).moveClicks(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // Nothing do to
        }
    }
}