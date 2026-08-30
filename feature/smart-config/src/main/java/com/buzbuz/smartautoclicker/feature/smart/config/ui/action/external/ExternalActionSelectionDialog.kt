/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.external

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogBaseListBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemCounterNameBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class ExternalActionSelectionDialog(
    private val onExternalActionSelected: (String) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private val viewModel: ExternalActionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { externalActionViewModel() },
    )
    private lateinit var viewBinding: DialogBaseListBinding
    private lateinit var adapter: ExternalActionSelectionAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseListBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_external_action_selection)
                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            floatingButtonsLayout.visibility = View.GONE

            adapter = ExternalActionSelectionAdapter { selectedName ->
                debounceUserInteraction {
                    onExternalActionSelected(selectedName)
                    back()
                }
            }

            layoutLoadableList.apply {
                setEmptyText(R.string.message_empty_external_action_list_title, R.string.message_empty_external_action_list_desc)
                list.adapter = adapter
                list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.knownExternalActionNames.collect(::updateExternalActionNames) }
            }
        }
    }

    private fun updateExternalActionNames(names: List<String>) {
        viewBinding.layoutLoadableList.updateState(names)
        adapter.submitList(names)
    }
}

private class ExternalActionSelectionAdapter(
    private val onExternalActionSelected: (String) -> Unit,
) : ListAdapter<String, ExternalActionSelectionViewHolder>(ExternalActionSelectionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExternalActionSelectionViewHolder =
        ExternalActionSelectionViewHolder(
            ItemCounterNameBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onExternalActionSelected,
        )

    override fun onBindViewHolder(holder: ExternalActionSelectionViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }
}

private object ExternalActionSelectionDiffUtilCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}

private class ExternalActionSelectionViewHolder(
    private val viewBinding: ItemCounterNameBinding,
    private val onExternalActionSelected: (String) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(name: String) {
        viewBinding.title.text = name
        viewBinding.description.setText(R.string.field_external_action_selection_desc)
        viewBinding.root.setOnClickListener { onExternalActionSelected(name) }
    }
}
