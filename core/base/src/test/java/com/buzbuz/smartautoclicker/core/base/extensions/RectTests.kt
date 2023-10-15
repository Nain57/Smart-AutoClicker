package com.buzbuz.smartautoclicker.core.base.extensions

import android.graphics.Point
import android.graphics.Rect
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests for the extensions for [Rect]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class RectTests {

    @Test
    fun sizeZero() {
        val rect = Rect(0, 0, 0, 0)
        Assert.assertEquals(Point(0, 0), rect.size())
    }

    @Test
    fun sizePositiveValues() {
        val rect = Rect(10, 10, 110, 110)
        Assert.assertEquals(Point(100, 100), rect.size())
    }

    @Test
    fun sizeNegativeValues() {
        val rect = Rect(-100, -100, 100, 100)
        Assert.assertEquals(Point(200, 200), rect.size())
    }
}