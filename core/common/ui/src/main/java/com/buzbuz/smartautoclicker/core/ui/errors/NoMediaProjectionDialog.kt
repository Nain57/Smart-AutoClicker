/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.errors

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.buzbuz.smartautoclicker.core.ui.R

import com.google.android.material.dialog.MaterialAlertDialogBuilder


fun Context.createNoMediaProjectionDialog(onValidated: () -> Unit): AlertDialog =
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_overlay_title_warning)
        .setMessage(R.string.message_error_screen_capture_permission_dialog_not_found)
        .setPositiveButton(android.R.string.ok, null)
        .create()
        .also { it.setOnDismissListener { onValidated() } }