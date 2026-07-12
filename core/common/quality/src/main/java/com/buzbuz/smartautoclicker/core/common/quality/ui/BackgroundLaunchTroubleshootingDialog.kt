/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.common.quality.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.buzbuz.smartautoclicker.core.base.extensions.safeStartWebBrowserActivity
import com.buzbuz.smartautoclicker.core.common.quality.databinding.DialogAccessibilityTroubleshootingBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** A generic, Don’t Kill My App-style explanation for an Android background-launch failure. */
class BackgroundLaunchTroubleshootingDialog : DialogFragment() {

    companion object {
        const val FRAGMENT_TAG = "BackgroundLaunchTroubleshootingDialog"

        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_HELP_URL = "help_url"

        fun newInstance(title: String, message: String, helpUrl: String) =
            BackgroundLaunchTroubleshootingDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_HELP_URL, helpUrl)
                }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val binding = DialogAccessibilityTroubleshootingBinding.inflate(layoutInflater).apply {
            titlePermission.text = args.getString(ARG_TITLE)
            descPermission.text = args.getString(ARG_MESSAGE)
            buttonOpenWebsite.setOnClickListener {
                context?.safeStartWebBrowserActivity(args.getString(ARG_HELP_URL).orEmpty())
            }
            buttonUnderstood.setOnClickListener { dismiss() }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
}
