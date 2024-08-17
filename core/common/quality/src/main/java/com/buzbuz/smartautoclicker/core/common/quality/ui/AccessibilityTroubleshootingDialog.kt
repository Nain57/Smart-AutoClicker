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
package com.buzbuz.smartautoclicker.core.common.quality.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.buzbuz.smartautoclicker.core.base.extensions.startWebBrowserActivity

import com.buzbuz.smartautoclicker.core.common.quality.databinding.DialogAccessibilityTroubleshootingBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AccessibilityTroubleshootingDialog : DialogFragment() {

    internal companion object {
        /** Tag for dialog fragment. */
        internal const val FRAGMENT_TAG_TROUBLESHOOTING_DIALOG = "AccessibilityTroubleshootingDialog"
        /** Fragment result key for notifying the dialog is closed. */
        internal const val FRAGMENT_RESULT_KEY_TROUBLESHOOTING = ":$FRAGMENT_TAG_TROUBLESHOOTING_DIALOG:result"
    }

    private lateinit var viewBinding: DialogAccessibilityTroubleshootingBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = DialogAccessibilityTroubleshootingBinding.inflate(layoutInflater).apply {
            buttonOpenWebsite.setOnClickListener { showDontKillMyApp() }
            buttonUnderstood.setOnClickListener { dismiss() }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(viewBinding.root)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        setFragmentResult(
            requestKey = FRAGMENT_RESULT_KEY_TROUBLESHOOTING,
            result = Bundle.EMPTY,
        )
    }

    private fun showDontKillMyApp() {
        context?.startWebBrowserActivity("https://dontkillmyapp.com")
    }
}