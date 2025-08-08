
package com.buzbuz.smartautoclicker.core.common.quality.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.buzbuz.smartautoclicker.core.base.extensions.safeStartWebBrowserActivity

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
        context?.safeStartWebBrowserActivity("https://dontkillmyapp.com")
    }
}