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
package com.buzbuz.smartautoclicker.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.TextView

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.isNull
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

import org.robolectric.annotation.Config

import java.lang.reflect.Field
import java.lang.reflect.Modifier

/** Tests for the extensions for [TextView]. */
@RunWith(MockitoJUnitRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class TextViewTests {

    private companion object {
        /** Drawable resources id for the tests. */
        @DrawableRes private const val TEST_DATA_DRAWABLE_RES = 42
        /** Another drawable resources id for the tests. */
        @DrawableRes private const val TEST_DATA_DRAWABLE_RES_2 = 1664
        /** Color value for the tests. */
        @ColorInt private const val TEST_DATA_COLOR_INT = 51

        /** Set [android.os.Build.VERSION.SDK_INT] to 24. Ugly dirty reflection, bouhhhhhhh ! */
        @BeforeClass @JvmStatic fun setup() {
            val field = Build.VERSION::class.java.getField("SDK_INT")
            field.isAccessible = true

            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.getModifiers() and Modifier.FINAL.inv())

            field.set(null, 24)
        }
    }

    /** A mocked version of the Android context. */
    @Mock private lateinit var mockContext: Context
    /** A mocked version of a drawable inflated via [TEST_DATA_DRAWABLE_RES]. */
    @Mock private lateinit var mockDrawable: Drawable
    /** A mocked version of a drawable inflated via [TEST_DATA_DRAWABLE_RES_2]. */
    @Mock private lateinit var mockDrawable2: Drawable
    /** The object the extension is on. Mocked to check internal calls, we are testing the extension, not the object. */
    @Mock private lateinit var textView: TextView

    @Before
    fun setUp() {
        `when`(textView.context).thenReturn(mockContext)
        `when`(mockContext.getDrawable(TEST_DATA_DRAWABLE_RES)).thenReturn(mockDrawable)
        `when`(mockContext.getDrawable(TEST_DATA_DRAWABLE_RES_2)).thenReturn(mockDrawable2)
        `when`(textView.compoundDrawablesRelative).thenReturn(arrayOf(mockDrawable, null, mockDrawable2, null))
    }

    @Test
    fun setLeftCompoundDrawable() {
        textView.setLeftCompoundDrawable(TEST_DATA_DRAWABLE_RES)
        verify(textView).setCompoundDrawablesRelativeWithIntrinsicBounds(eq(mockDrawable), isNull(), isNull(), isNull())
        verify(mockDrawable, never()).setTint(anyInt())
        verify(mockDrawable2, never()).setTint(anyInt())
    }

    @Test
    fun setLeftCompoundDrawableWithTint() {
        textView.setLeftCompoundDrawable(TEST_DATA_DRAWABLE_RES, TEST_DATA_COLOR_INT)
        verify(textView).setCompoundDrawablesRelativeWithIntrinsicBounds(eq(mockDrawable), isNull(), isNull(), isNull())
        verify(mockDrawable).setTint(TEST_DATA_COLOR_INT)
    }

    @Test
    fun setLeftRightCompoundDrawables() {
        textView.setLeftRightCompoundDrawables(TEST_DATA_DRAWABLE_RES, TEST_DATA_DRAWABLE_RES_2)
        verify(textView).setCompoundDrawablesRelativeWithIntrinsicBounds(eq(mockDrawable), isNull(), eq(mockDrawable2), isNull())
        verify(mockDrawable, never()).setTint(anyInt())
        verify(mockDrawable2, never()).setTint(anyInt())
    }

    @Test
    fun setLeftRightCompoundDrawablesWithTint() {
        textView.setLeftRightCompoundDrawables(TEST_DATA_DRAWABLE_RES, TEST_DATA_DRAWABLE_RES_2, TEST_DATA_COLOR_INT)
        verify(textView).setCompoundDrawablesRelativeWithIntrinsicBounds(eq(mockDrawable), isNull(), eq(mockDrawable2), isNull())
        verify(mockDrawable).setTint(TEST_DATA_COLOR_INT)
        verify(mockDrawable2, never()).setTint(anyInt())
    }
}