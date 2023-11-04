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
package com.buzbuz.smartautoclicker.activity.permissions

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle

import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult

import com.buzbuz.smartautoclicker.databinding.DialogPermissionBinding

import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PermissionDialogFragment : DialogFragment() {

    companion object {

        /** Tag for permission dialog fragment. */
        const val FRAGMENT_TAG_PERMISSION_DIALOG = "PermissionDialog"

        /** Fragment result key for the permission granted or not state once dialog is closed. */
        const val FRAGMENT_RESULT_KEY_PERMISSION_STATE = ":$FRAGMENT_TAG_PERMISSION_DIALOG:state"
        /**
         * Key for [FRAGMENT_RESULT_KEY_PERMISSION_STATE] result bundle.
         * Boolean indicating the permission state.
         */
        const val EXTRA_RESULT_KEY_PERMISSION_STATE = "$FRAGMENT_RESULT_KEY_PERMISSION_STATE:isGranted"

        /** Intent extra bundle key for the permission enum name argument. */
        private const val ARGUMENT_PERMISSION_TYPE = ":$FRAGMENT_TAG_PERMISSION_DIALOG:permission"

        /**
         * Creates a new instance of this fragment.
         * @return the new fragment.
         */
        fun newInstance(permissionType: Permission.Type) : PermissionDialogFragment {
            return PermissionDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGUMENT_PERMISSION_TYPE, permissionType.name)
                }
            }
        }
    }

    /** Permission type argument. */
    private val permissionArgument: Permission by lazy {
        arguments?.getString(ARGUMENT_PERMISSION_TYPE)?.let { typeName ->
            Permission.Type.valueOf(typeName).permission
        } ?: throw IllegalArgumentException("Undefined permission")
    }

    /** ViewBinding for this dialog. */
    private lateinit var viewBinding: DialogPermissionBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        (permissionArgument as? Permission.Dangerous)?.initResultLauncher(this) { isGranted ->
            if (isGranted || permissionArgument.isOptional()) dismiss()
        }

        viewBinding = DialogPermissionBinding.inflate(layoutInflater).apply {
            titlePermission.setText(permissionArgument.titleRes)
            descPermission.setText(permissionArgument.descriptionRes)

            buttonRequestPermission.setOnClickListener {
                permissionArgument.startRequestFlow(requireContext())
            }

            buttonDenyPermission.setOnClickListener {
                dismiss()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(viewBinding.root)
            .create()
    }

    override fun onResume() {
        super.onResume()

        if (permissionArgument is Permission.Special &&
            (permissionArgument.isGranted(requireContext()) || permissionArgument.isOptional())) {
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        setFragmentResult(
            requestKey = FRAGMENT_RESULT_KEY_PERMISSION_STATE,
            result = bundleOf(EXTRA_RESULT_KEY_PERMISSION_STATE to permissionArgument.isGranted(requireContext())),
        )
    }
}