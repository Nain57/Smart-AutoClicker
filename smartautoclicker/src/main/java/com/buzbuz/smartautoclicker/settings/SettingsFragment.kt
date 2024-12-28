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
package com.buzbuz.smartautoclicker.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle

import com.buzbuz.smartautoclicker.databinding.FragmentSettingsBinding

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var viewBinding: FragmentSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.fieldLegacyActionsUi.apply {
            setTitle(requireContext().getString(R.string.field_legacy_action_ui_title))
            setDescription(requireContext().getString(R.string.field_legacy_action_ui_desc))
            setOnClickListener(viewModel::toggleLegacyActionUi)
        }

        viewBinding.fieldLegacyNotificationUi.apply {
            setTitle(requireContext().getString(R.string.field_legacy_notification_ui_title))
            setDescription(requireContext().getString(R.string.field_legacy_notification_ui_desc))
            setOnClickListener(viewModel::toggleLegacyNotificationUi)
        }

        viewBinding.fieldForceEntireScreen.apply {
            setTitle(requireContext().getString(R.string.field_force_entire_screen_title))
            setDescription(requireContext().getString(R.string.field_force_entire_screen_desc))
            setOnClickListener(viewModel::toggleForceEntireScreenCapture)
        }

        viewBinding.fieldPrivacySettings.apply {
            setTitle(requireContext().getString(R.string.field_privacy))
            setOnClickListener { viewModel.showPrivacySettings(requireActivity()) }
        }

        viewBinding.fieldRemoveAds.apply {
            setTitle(requireContext().getString(R.string.field_remove_ads))
            setOnClickListener { viewModel.showPurchaseActivity(requireActivity()) }
        }

        viewBinding.fieldTroubleshooting.apply {
            setTitle(requireContext().getString(R.string.field_troubleshooting))
            setOnClickListener { viewModel.showTroubleshootingDialog(requireActivity()) }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isLegacyActionUiEnabled.collect(viewBinding.fieldLegacyActionsUi::setChecked) }
                launch { viewModel.isLegacyNotificationUiEnabled.collect(viewBinding.fieldLegacyNotificationUi::setChecked) }
                launch { viewModel.isEntireScreenCaptureForced.collect(viewBinding.fieldForceEntireScreen::setChecked) }
                launch { viewModel.shouldShowEntireScreenCapture.collect(::updateForceEntireScreenVisibility) }
                launch { viewModel.shouldShowPrivacySettings.collect(::updatePrivacySettingsVisibility) }
                launch { viewModel.shouldShowPurchase.collect(::updateRemoveAdsVisibility) }
            }
        }
    }

    private fun updateForceEntireScreenVisibility(shouldBeVisible: Boolean) {
        if (shouldBeVisible) {
            viewBinding.dividerForceEntireScreen.visibility = View.VISIBLE
            viewBinding.fieldForceEntireScreen.root.visibility = View.VISIBLE
        } else {
            viewBinding.dividerForceEntireScreen.visibility = View.GONE
            viewBinding.fieldForceEntireScreen.root.visibility = View.GONE
        }
    }

    private fun updatePrivacySettingsVisibility(shouldBeVisible: Boolean) {
        if (shouldBeVisible) {
            viewBinding.dividerPrivacySettings.visibility = View.VISIBLE
            viewBinding.fieldPrivacySettings.root.visibility = View.VISIBLE
        } else {
            viewBinding.dividerPrivacySettings.visibility = View.GONE
            viewBinding.fieldPrivacySettings.root.visibility = View.GONE
        }
    }

    private fun updateRemoveAdsVisibility(shouldBeVisible: Boolean) {
        if (shouldBeVisible) {
            viewBinding.dividerRemoveAds.visibility = View.VISIBLE
            viewBinding.fieldRemoveAds.root.visibility = View.VISIBLE
        } else {
            viewBinding.dividerRemoveAds.visibility = View.GONE
            viewBinding.fieldRemoveAds.root.visibility = View.GONE
        }
    }
}