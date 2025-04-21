/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.languages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding

import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemLanguageDownloadBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemLanguageDownloadHeaderBinding


class LanguageFilesDownloadAdapter(
    private val onDownloadClicked: (item: LanguageFilesUiItem.Language) -> Unit,
) : ListAdapter<LanguageFilesUiItem, LanguageDownloadViewHolder>(LanguageFilesUiItemDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is LanguageFilesUiItem.Header -> R.layout.item_language_download_header
            is LanguageFilesUiItem.Language -> R.layout.item_language_download
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageDownloadViewHolder =
        when (viewType) {
            R.layout.item_language_download_header -> LanguageDownloadHeaderViewHolder(
                ItemLanguageDownloadHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            )

            R.layout.item_language_download -> LanguageDownloadItemViewHolder(
                ItemLanguageDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onDownloadClicked,
            )

            else -> throw IllegalArgumentException("Unsupported item type")
        }


    override fun onBindViewHolder(holder: LanguageDownloadViewHolder, position: Int) =
        when (holder) {
            is LanguageDownloadHeaderViewHolder -> holder.onBind((getItem(position) as LanguageFilesUiItem.Header))
            is LanguageDownloadItemViewHolder -> holder.onBind((getItem(position) as LanguageFilesUiItem.Language))
        }
}


sealed class LanguageDownloadViewHolder(viewBinding: ViewBinding) : ViewHolder(viewBinding.root)

internal class LanguageDownloadHeaderViewHolder (
    val viewBinding: ItemLanguageDownloadHeaderBinding,
): LanguageDownloadViewHolder(viewBinding) {

    fun onBind(header: LanguageFilesUiItem.Header) {
        viewBinding.languageHeaderText.setText(header.text)
    }
}

internal class LanguageDownloadItemViewHolder (
    val viewBinding: ItemLanguageDownloadBinding,
    val onDownloadClicked: (LanguageFilesUiItem.Language) -> Unit,
): LanguageDownloadViewHolder(viewBinding) {

    fun onBind(item: LanguageFilesUiItem.Language) {
        viewBinding.apply {
            languageName.setText(item.languageName)

            when (item.downloadState) {
                LanguageFileDownloadUiState.Error,
                LanguageFileDownloadUiState.NotDownloaded -> {
                    textDownloadProgress.visibility = View.GONE
                    iconDownloadComplete.visibility = View.GONE
                    buttonDownload.visibility = View.VISIBLE
                    buttonDownload.setOnClickListener { onDownloadClicked(item) }
                }

                is LanguageFileDownloadUiState.Downloading -> {
                    textDownloadProgress.visibility = View.VISIBLE
                    textDownloadProgress.text = item.downloadState.progressText
                    iconDownloadComplete.visibility = View.GONE
                    buttonDownload.visibility = View.GONE
                    buttonDownload.setOnClickListener(null)
                }

                LanguageFileDownloadUiState.Downloaded -> {
                    textDownloadProgress.visibility = View.GONE
                    iconDownloadComplete.visibility = View.VISIBLE
                    buttonDownload.visibility = View.GONE
                    buttonDownload.setOnClickListener(null)
                }
            }
        }
    }
}


internal object LanguageFilesUiItemDiffUtilCallback: DiffUtil.ItemCallback<LanguageFilesUiItem>() {

    override fun areItemsTheSame(oldItem: LanguageFilesUiItem, newItem: LanguageFilesUiItem): Boolean =
        (oldItem is LanguageFilesUiItem.Header && newItem is LanguageFilesUiItem.Header) ||
                (oldItem is LanguageFilesUiItem.Language && newItem is LanguageFilesUiItem.Language
                        && oldItem.trainedTextLanguage == newItem.trainedTextLanguage)

    override fun areContentsTheSame(oldItem: LanguageFilesUiItem, newItem: LanguageFilesUiItem): Boolean =
        oldItem == newItem
}