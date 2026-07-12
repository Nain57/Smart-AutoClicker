/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.buzbuz.smartautoclicker.feature.externallaunch.R
import com.buzbuz.smartautoclicker.feature.externallaunch.databinding.ItemLocalePluginScenarioBinding

internal class LocalePluginScenarioAdapter(context: Context) :
    ArrayAdapter<LocalePluginScenarioItem>(context, R.layout.item_locale_plugin_scenario, mutableListOf()) {

    fun replace(items: List<LocalePluginScenarioItem>) {
        clear()
        addAll(items)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    private fun bind(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null) {
            ItemLocalePluginScenarioBinding.inflate(LayoutInflater.from(context), parent, false)
        } else {
            ItemLocalePluginScenarioBinding.bind(convertView)
        }
        val item = getItem(position) ?: return binding.root
        binding.name.text = item.name
        binding.type.setText(
            if (item.isSmart) R.string.locale_plugin_type_smart else R.string.locale_plugin_type_dumb
        )
        binding.icon.setImageResource(
            if (item.isSmart) {
                com.buzbuz.smartautoclicker.core.ui.R.drawable.ic_screen_event
            } else {
                com.buzbuz.smartautoclicker.core.ui.R.drawable.ic_click
            }
        )
        return binding.root
    }
}
