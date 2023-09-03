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
package com.buzbuz.smartautoclicker.feature.billing.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.View

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.feature.billing.R
import com.buzbuz.smartautoclicker.feature.billing.databinding.FragmentProModeBillingDialogBinding

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

internal class ProModeBillingDialogFragment : DialogFragment() {

    companion object {
        /** Tag for pro mode billing dialog fragment. */
        const val FRAGMENT_TAG = "ProModeBillingDialog"
    }

    /** ViewModel providing the click scenarios data to the UI. */
    private val billingViewModel: ProModeBillingViewModel by activityViewModels()
    /** The view binding on the views of this dialog. */
    private lateinit var viewBinding: FragmentProModeBillingDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { billingViewModel.dialogState.collect(::updateDialogState) }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = FragmentProModeBillingDialogBinding.inflate(layoutInflater)

        viewBinding.apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.billing_pro_mode_dialog_title)

                buttonDismiss.setOnClickListener { dismiss() }
                buttonSave.visibility = View.GONE
                buttonDelete.visibility = View.GONE
            }

            buyButton.setOnClickListener { onBuyButtonClick() }
        }

        return BottomSheetDialog(requireContext()).apply {
            setContentView(viewBinding.root)
            setCancelable(false)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    this@ProModeBillingDialogFragment.dismiss()
                    true
                } else {
                    false
                }
            }

            create()

            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }

    private fun updateDialogState(state: DialogState) {
        viewBinding.apply {
            when (state) {
                DialogState.Connecting -> {
                    layoutPurchased.visibility = View.GONE
                    layoutNotPurchased.visibility = View.INVISIBLE
                    layoutConnecting.visibility = View.VISIBLE
                }

                DialogState.Purchased -> {
                    layoutPurchased.visibility = View.VISIBLE
                    layoutNotPurchased.visibility = View.INVISIBLE
                    layoutConnecting.visibility = View.GONE
                }

                is DialogState.NotPurchased -> {
                    layoutPurchased.visibility = View.GONE
                    layoutNotPurchased.visibility = View.VISIBLE
                    layoutConnecting.visibility = View.GONE

                    if (state.billingReasonText.isNullOrEmpty()) {
                        reasonText.text = null
                        reasonText.visibility = View.GONE
                    } else {
                        reasonText.text = state.billingReasonText
                        reasonText.visibility = View.VISIBLE
                    }

                    buyButton.text = state.acceptButtonText
                }
            }
        }
    }

    private fun onBuyButtonClick() {
        activity?.let { billingViewModel.launchPlayStoreBillingFlow(it) }
    }
}