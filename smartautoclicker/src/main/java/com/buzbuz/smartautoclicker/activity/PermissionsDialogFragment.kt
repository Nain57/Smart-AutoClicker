/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.SmartAutoClickerService
import com.buzbuz.smartautoclicker.activity.PermissionsDialogFragment.PermissionDialogListener

/**
 * Displays the state of the permission and provide a way to access their respective settings.
 * Activity attaching this fragment must implements [PermissionDialogListener] to be notified upon grant success to all
 * permissions.
 */
class PermissionsDialogFragment : DialogFragment() {

    companion object {

        /** Intent extra bundle key for the Android settings app. */
        private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"

        /** Intent extra bundle key for the Android settings app. */
        private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

        /**
         * Creates a new instance of this fragment.
         * @return the new fragment.
         */
        fun newInstance(): PermissionsDialogFragment {
            return PermissionsDialogFragment()
        }
    }

    /**
     * Listener to be implementation by the activity attaching this fragment receiving the all permission granted
     * information.
     */
    interface PermissionDialogListener {
        /** Called when all permissions are granted and the user press ok. */
        fun onPermissionsGranted()
    }

    /** ViewModel providing the click scenarios data to the UI. */
    private val scenarioViewModel: ScenarioViewModel by activityViewModels()

    /** View for the overlay permission. */
    private var overlayValid by mutableStateOf(false)

    /** View for the state of the overlay permission. */
    private var accessValid by mutableStateOf(false)


    @Preview(showBackground = true)
    @Composable
    fun DialogPermissions() {
        overlayValid = scenarioViewModel.isOverlayPermissionValid()
        accessValid = scenarioViewModel.isAccessibilityPermissionValid()
        Column() {
            Text(
                text = stringResource(id = R.string.dialog_permissions_title),
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(color = colorResource(R.color.primaryDark))
                    .fillMaxWidth()
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(8.dp)
            )
            Text(text = stringResource(id = R.string.dialog_permissions_header), modifier = Modifier.padding(8.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onOverlayClicked() })
            {

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.dialog_permission_overlay),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.dialog_permission_overlay_desc),
                        modifier = Modifier.padding(4.dp)
                    )
                }

                Image(
                    colorFilter = colorPick(overlayValid),
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                    painter = painterResource(id = iconPick(overlayValid)),
                    contentDescription = stringResource(id = R.string.content_desc_overlay_state)
                )

            }
            Divider(color = Color.LightGray, thickness = 1.dp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onAccessibilityClicked() })
            {

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.dialog_permission_accessibility),
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.dialog_permission_accessibility_desc),
                        modifier = Modifier.padding(4.dp)
                    )
                }

                Image(
                    colorFilter = colorPick(accessValid),
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                    painter = painterResource(id = iconPick(accessValid)),
                    contentDescription = stringResource(id = R.string.content_desc_overlay_state)
                )


            }
        }
    }

    private fun colorPick(flag: Boolean): ColorFilter {
        return if (flag)
            ColorFilter.tint(color = Color.Green)
        else
            ColorFilter.tint(color = Color.Red)
    }

    private fun iconPick(flag: Boolean): Int {
        return if (flag) {
            R.drawable.ic_confirm
        } else {
            R.drawable.ic_cancel
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                DialogPermissions()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayValid = scenarioViewModel.isOverlayPermissionValid()
        accessValid = scenarioViewModel.isAccessibilityPermissionValid()
        if (overlayValid and accessValid) {
            (activity as PermissionDialogListener).onPermissionsGranted()
            dialog?.dismiss()
        }
    }

    /**
     * Called when the user clicks on the overlay permission state.
     * This will start the Android Settings Activity for the overlay permission of this application.
     */
    private fun onOverlayClicked() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${requireContext().packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)

        requireContext().startActivity(intent)
    }

    /**
     * Called when the user clicks on the Accessibility Service permission state.
     * This will open the Android Settings Activity for the list of available Accessibility Service. Note that it seems
     * impossible to directly start this application accessibility service management screen, but only the list of all
     * Accessibility services.
     */
    private fun onAccessibilityClicked() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)

        val bundle = Bundle()
        val showArgs = requireContext().packageName + "/" + SmartAutoClickerService::class.java.name
        bundle.putString(EXTRA_FRAGMENT_ARG_KEY, showArgs)
        intent.putExtra(EXTRA_FRAGMENT_ARG_KEY, showArgs)
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)

        requireContext().startActivity(intent)
    }
}