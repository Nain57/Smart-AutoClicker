/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.baseui.dialog

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.widget.TextView

import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

import org.robolectric.annotation.Config

import java.lang.NullPointerException

/** Tests for the extensions for [androidx.appcompat.app.AlertDialog]. */
@RunWith(MockitoJUnitRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AlertDialogTests {

    private companion object {
        /** Custom title view layout resources for the tests. */
        @LayoutRes private const val TEST_DATA_TITLE_LAYOUT_RES = 42
        /** Title string resource id for the tests. */
        @StringRes private const val TEST_DATA_TITLE_STRING_RES = 51
    }

    /** A mocked version of the Android context. */
    @Mock private lateinit var mockContext: Context
    /** A mocked version of the Android layout inflater. */
    @Mock private lateinit var mockLayoutInflater: LayoutInflater
    /** A mocked version of the title view to be inflated via [TEST_DATA_TITLE_LAYOUT_RES]. */
    @Mock private lateinit var mockCustomView: TextView
    /** The object the extension is on. Mocked to check internal calls, we are testing the extension, not the object. */
    @Mock private lateinit var dialogBuilder: AlertDialog.Builder

    @Before
    fun setUp() {
        `when`(dialogBuilder.context).thenReturn(mockContext)
        `when`(mockContext.getSystemService(LayoutInflater::class.java)).thenReturn(mockLayoutInflater)
        `when`(mockLayoutInflater.inflate(TEST_DATA_TITLE_LAYOUT_RES, null)).thenReturn(mockCustomView)
    }

    @Test
    fun setCustomTitleOk() {
        val titleTextView = Mockito.mock(TextView::class.java)
        `when`(mockCustomView.findViewById<TextView>(android.R.id.title)).thenReturn(titleTextView)

        dialogBuilder.setCustomTitle(TEST_DATA_TITLE_LAYOUT_RES, TEST_DATA_TITLE_STRING_RES)

        verify(titleTextView).setText(TEST_DATA_TITLE_STRING_RES)
        verify(dialogBuilder).setCustomTitle(mockCustomView)
    }

    @Test(expected = NullPointerException::class)
    fun setCustomTitleNoTextView() {
        `when`(mockCustomView.findViewById<TextView>(android.R.id.title)).thenReturn(null)

        dialogBuilder.setCustomTitle(TEST_DATA_TITLE_LAYOUT_RES, TEST_DATA_TITLE_STRING_RES)
    }
}