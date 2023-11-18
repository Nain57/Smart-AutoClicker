/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.overlays.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.databinding.DialogBaseMultiChoiceBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.ItemMultiChoiceBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.ItemMultiChoiceSmallBinding

import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * [OverlayDialog] implementation for a dialog displaying a list of choices to the user.
 *
 * @param T the type of choices in the list. Must extends [DialogChoice].
 * @param theme the resource id of the theme to apply.
 * @param dialogTitleText the title of the dialog.
 * @param choices the choices to be displayed.
 * @param onChoiceSelected the callback to be notified upon user choice selection.
 */
open class MultiChoiceDialog<T : DialogChoice>(
    @StyleRes theme: Int,
    @StringRes private val dialogTitleText: Int,
    private val choices: List<T>,
    private val onChoiceSelected: (T) -> Unit,
    private val onCanceled: (() -> Unit)? = null,
) : OverlayDialog(theme) {

    /** ViewBinding containing the views for this dialog. */
    protected lateinit var viewBinding: DialogBaseMultiChoiceBinding
    /** The adapter displaying the choices. */
    protected lateinit var adapter: ChoiceAdapter<T>

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseMultiChoiceBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(dialogTitleText)
                buttonDismiss.setOnClickListener {
                    debounceUserInteraction {
                        onCanceled?.invoke()
                        back()
                    }
                }
            }

            adapter = ChoiceAdapter(
                choices = choices,
                onChoiceSelected = { choice ->
                    debounceUserInteraction {
                        back()
                        onChoiceSelected(choice)
                    }
                },
                onChoiceViewBound = ::onChoiceViewBound,
            )
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewBinding.list.adapter = adapter
    }

    open fun onChoiceViewBound(choice: T, view: View?) = Unit
}

/**
 * Adapter displaying the choices in the dialog.
 *
 * @param T the type of choices in the list.
 * @param choices the choices to be displayed in the list.
 * @param onChoiceSelected called when the user clicks on a choice.
 */
class ChoiceAdapter<T : DialogChoice>(
    private val choices: List<T>,
    private val onChoiceSelected: (T) -> Unit,
    private val onChoiceViewBound: ((T, View?) -> Unit),
): RecyclerView.Adapter<MultiChoiceViewHolder<T>>() {

    override fun getItemCount(): Int = choices.size

    override fun getItemViewType(position: Int): Int {
        val item = choices[position]

        return when {
            item.description == null && item.iconId == null -> R.layout.item_multi_choice_small
            else -> R.layout.item_multi_choice
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiChoiceViewHolder<T> =
        when (viewType) {
            R.layout.item_multi_choice_small ->
                SmallChoiceViewHolder(ItemMultiChoiceSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_multi_choice ->
                ChoiceViewHolder(ItemMultiChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: MultiChoiceViewHolder<T>, position: Int) {
        choices[position].let { choice ->
            holder.onBind(choice, onChoiceSelected)
            onChoiceViewBound(choice, holder.itemView)
        }
    }

    override fun onViewRecycled(holder: MultiChoiceViewHolder<T>) {
        super.onViewRecycled(holder)
        onChoiceViewBound(choices[holder.bindingAdapterPosition], null)
    }
}

/**
 * Base view holder for a choice.
 * @param itemView the root view of the item.
 */
abstract class MultiChoiceViewHolder<T : DialogChoice>(itemView: View): ViewHolder(itemView) {

    /**
     * Binds a choice to this view holder.
     * @param choice the choice object to be bound.
     * @param onChoiceSelected listener upon user click on the choice item.
     */
    abstract fun onBind(choice: T, onChoiceSelected: (T) -> Unit)
}

/**
 * View holder for a choice with an icon and a description.
 * @param holderViewBinding the view binding containing the holder root view.
 */
private class ChoiceViewHolder<T : DialogChoice>(
    val holderViewBinding: ItemMultiChoiceBinding,
) : MultiChoiceViewHolder<T>(holderViewBinding.root) {

    override fun onBind(choice: T, onChoiceSelected: (T) -> Unit) {
        holderViewBinding.apply {
            root.setOnClickListener { onChoiceSelected.invoke(choice) }

            choiceTitle.setText(choice.title)
            choiceDescription.apply {
                if (choice.description != null) {
                    visibility = View.VISIBLE
                    setText(choice.description)
                } else {
                    visibility = View.GONE
                }
            }
            choiceIcon.apply {
                if (choice.iconId != null) {
                    visibility = View.VISIBLE
                    setImageResource(choice.iconId)
                } else {
                    visibility = View.GONE
                    setImageResource(0)
                }
            }

            if (choice.enabled) {
                choiceTitle.alpha = ENABLED_ITEM_ALPHA
                choiceDescription.alpha = ENABLED_ITEM_ALPHA
                choiceIcon.alpha = ENABLED_ITEM_ALPHA
                choiceChevron.setImageResource(R.drawable.ic_chevron_right)
            } else {
                choiceTitle.alpha = DISABLED_ITEM_ALPHA
                choiceDescription.alpha = DISABLED_ITEM_ALPHA
                choiceIcon.alpha = DISABLED_ITEM_ALPHA
                choice.disabledIconId?.let { choiceChevron.setImageResource(it) }
            }
        }
    }
}

private const val ENABLED_ITEM_ALPHA = 1f
private const val DISABLED_ITEM_ALPHA = 0.5f

/**
 * View holder for a choice with only a title.
 * @param holderViewBinding the view binding containing the holder root view.
 */
private class SmallChoiceViewHolder<T : DialogChoice>(
    val holderViewBinding: ItemMultiChoiceSmallBinding,
) : MultiChoiceViewHolder<T>(holderViewBinding.root) {

    override fun onBind(choice: T, onChoiceSelected: (T) -> Unit) {
        holderViewBinding.apply {
            root.setOnClickListener { onChoiceSelected.invoke(choice) }
            choiceTitle.setText(choice.title)
        }
    }
}

/** Base class for a dialog choice. */
open class DialogChoice(
    @StringRes val title: Int,
    @StringRes val description: Int? = null,
    @DrawableRes val iconId: Int? = null,
    val enabled: Boolean = true,
    @DrawableRes val disabledIconId: Int? = null,
)