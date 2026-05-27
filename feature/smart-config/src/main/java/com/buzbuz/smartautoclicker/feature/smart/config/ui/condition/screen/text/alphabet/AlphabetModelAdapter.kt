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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemAlphabetBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemAlphabetHeaderBinding

class AlphabetModelItemAdapter(
    private val onItemClicked: (item: AlphabetSelectionItem) -> Unit,
) : ListAdapter<AlphabetSelectionItem, AlphabetSelectionViewHolder>(AlphabetItemDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is AlphabetSelectionItem.Header -> R.layout.item_alphabet_header
            is AlphabetSelectionItem.Alphabet -> R.layout.item_alphabet
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlphabetSelectionViewHolder =
            when (viewType) {
                    R.layout.item_alphabet_header -> AlphabetSelectionViewHolder.Header(
                        ItemAlphabetHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                        )

                    R.layout.item_alphabet -> AlphabetSelectionViewHolder.Item(
                        ItemAlphabetBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                            onItemClicked,
                        )

                    else -> throw IllegalArgumentException("Unsupported item type")
                }


       override fun onBindViewHolder(holder: AlphabetSelectionViewHolder, position: Int) =
           when (holder) {
               is AlphabetSelectionViewHolder.Header ->
                   holder.onBind((getItem(position) as AlphabetSelectionItem.Header))
               is AlphabetSelectionViewHolder.Item ->
                   holder.onBind((getItem(position) as AlphabetSelectionItem.Alphabet))
           }
    }

sealed class AlphabetSelectionViewHolder(viewBinding: ViewBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    internal class Header (
        val viewBinding: ItemAlphabetHeaderBinding,
    ): AlphabetSelectionViewHolder(viewBinding) {

        fun onBind(header: AlphabetSelectionItem.Header) {
            viewBinding.languageHeaderText.setText(header.text)
        }
    }

    internal class Item (
        val viewBinding: ItemAlphabetBinding,
        val onItemClicked: (AlphabetSelectionItem) -> Unit,
    ): AlphabetSelectionViewHolder(viewBinding) {

        fun onBind(item: AlphabetSelectionItem.Alphabet) {
            viewBinding.apply {
                alphabetName.setText(item.alphabetName)
                alphabetDesc.setText(item.alphabetDesc)

                root.setOnClickListener { onItemClicked(item) }
                buttonEnabledState.setOnClickListener { onItemClicked(item) }
                buttonDownload.setOnClickListener { onItemClicked(item) }

                when (item.downloadState) {
                    AlphabetDownloadUiState.Error,
                    AlphabetDownloadUiState.NotDownloaded -> {
                        textDownloadProgress.visibility = View.GONE
                        buttonEnabledState.visibility = View.GONE
                        buttonDownload.visibility = View.VISIBLE
                        imageInstalled.visibility = View.GONE
                    }

                    is AlphabetDownloadUiState.Downloading -> {
                        textDownloadProgress.visibility = View.VISIBLE
                        textDownloadProgress.text = item.downloadState.progressText
                        buttonEnabledState.visibility = View.GONE
                        buttonDownload.visibility = View.GONE
                        imageInstalled.visibility = View.GONE
                    }

                    AlphabetDownloadUiState.Downloaded -> {
                        textDownloadProgress.visibility = View.GONE
                        buttonDownload.visibility = View.GONE

                        if (item.selectableWhenInstalled) {
                            buttonEnabledState.visibility = View.VISIBLE
                            buttonEnabledState.isChecked = item.selected
                            imageInstalled.visibility = View.GONE
                        } else {
                            buttonEnabledState.visibility = View.GONE
                            imageInstalled.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }
}

internal object AlphabetItemDiffUtilCallback: DiffUtil.ItemCallback<AlphabetSelectionItem>() {

    override fun areItemsTheSame(oldItem: AlphabetSelectionItem, newItem: AlphabetSelectionItem): Boolean =
        (oldItem is AlphabetSelectionItem.Header && newItem is AlphabetSelectionItem.Header) ||
                (oldItem is AlphabetSelectionItem.Alphabet && newItem is AlphabetSelectionItem.Alphabet
                        && oldItem.alphabet == newItem.alphabet)

    override fun areContentsTheSame(oldItem: AlphabetSelectionItem, newItem: AlphabetSelectionItem): Boolean =
        oldItem == newItem
}