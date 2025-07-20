/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.tests.scaling

import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buzbuz.smartautoclicker.core.processing.data.scaling.grow
import com.buzbuz.smartautoclicker.core.processing.data.scaling.scale
import com.buzbuz.smartautoclicker.core.processing.data.scaling.toArea
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScalingExtTests {

    @Test
    fun `toArea with positive coordinates should create Rect from origin`() {
        // Given
        val point = Point(100, 200)

        // When
        val area = point.toArea()

        // Then
        Assert.assertEquals(Rect(0, 0, 100, 200), area)
    }

    @Test
    fun `toArea with zero coordinates should create empty Rect at origin`() {
        // Given
        val point = Point(0, 0)

        // When
        val area = point.toArea()

        // Then
        Assert.assertEquals(Rect(0, 0, 0, 0), area)
    }

    @Test
    fun `toArea with one coordinate zero should create Rect with one dimension zero`() {
        // Given
        val point1 = Point(0, 50)
        val point2 = Point(50, 0)

        // When
        val area1 = point1.toArea()
        val area2 = point2.toArea()

        // Then
        Assert.assertEquals(Rect(0, 0, 0, 50), area1)
        Assert.assertEquals(Rect(0, 0, 50, 0), area2)
    }

    @Test
    fun `scale with ratio 1 should return the same Point`() {
        // Given
        val originalPoint = Point(10, 20)
        val scalingRatio = 1.0

        // When
        val scaledPoint = originalPoint.scale(scalingRatio)

        // Then
        Assert.assertEquals("X coordinate should remain unchanged", 10, scaledPoint.x)
        Assert.assertEquals("Y coordinate should remain unchanged", 20, scaledPoint.y)
    }

    @Test
    fun `scale with ratio greater than 1 should return a new scaled Point`() {
        // Given
        val originalPoint = Point(10, 20)
        val scalingRatio = 2.5

        // When
        val scaledPoint = originalPoint.scale(scalingRatio)

        // Then
        Assert.assertEquals(Point(25, 50), scaledPoint)
    }

    @Test
    fun `scale with ratio less than 1 should return a new scaled Point`() {
        // Given
        val originalPoint = Point(100, 200)
        val scalingRatio = 0.5

        // When
        val scaledPoint = originalPoint.scale(scalingRatio)

        // Then
        Assert.assertEquals(Point(50, 100), scaledPoint)
    }

    @Test
    fun `scale with ratio resulting in fractional values should round correctly`() {
        // Given
        val point1 = Point(10, 11)
        val scalingRatio1 = 0.75 // 10*0.75=7.5 (rounds to 8), 11*0.75=8.25 (rounds to 8)

        val point2 = Point(7, 8)
        val scalingRatio2 = 1.1 // 7*1.1=7.7 (rounds to 8), 8*1.1=8.8 (rounds to 9)


        // When
        val scaledPoint1 = point1.scale(scalingRatio1)
        val scaledPoint2 = point2.scale(scalingRatio2)

        // Then
        val expectedPoint1 = Point(8, 8)
        val expectedPoint2 = Point(8, 9)
        Assert.assertEquals("Scaling $point1 by $scalingRatio1", expectedPoint1, scaledPoint1)
        Assert.assertEquals("Scaling $point2 by $scalingRatio2", expectedPoint2, scaledPoint2)
    }

    @Test
    fun `scale with zero coordinates should result in zero coordinates`() {
        // Given
        val originalPoint = Point(0, 0)
        val scalingRatio = 5.5

        // When
        val scaledPoint = originalPoint.scale(scalingRatio)

        // Then
        Assert.assertEquals(Point(0, 0), scaledPoint)
    }

    @Test
    fun `scale with zero ratio should result in zero coordinates`() {
        // Given
        val originalPoint = Point(100, 200)
        val scalingRatio = 0.0

        // When
        val scaledPoint = originalPoint.scale(scalingRatio)

        // Then
        val expectedPoint = Point(0, 0)
        Assert.assertEquals(expectedPoint, scaledPoint)
    }

    @Test
    fun `scale Rect with ratio 1 should return the same Rect`() {
        // Given
        val originalRect = Rect(10, 20, 30, 40)
        val scalingRatio = 1.0

        // When
        val scaledRect = originalRect.scale(scalingRatio)

        // Then
        Assert.assertEquals(Rect(10, 20, 30, 40), scaledRect)
    }

    @Test
    fun `scale Rect with positive coordinates and ratio greater than 1`() {
        // Given
        val originalRect = Rect(10, 20, 30, 50)
        val scalingRatio = 2.0

        // When
        val scaledRect = originalRect.scale(scalingRatio)

        // Then
        Assert.assertEquals(Rect(20, 40, 60, 100), scaledRect)
    }

    @Test
    fun `scale Rect with positive coordinates and ratio less than 1`() {
        // Given
        val originalRect = Rect(100, 200, 140, 280)
        val scalingRatio = 0.5

        // When
        val scaledRect = originalRect.scale(scalingRatio)

        // Then
        Assert.assertEquals(Rect(50, 100, 70, 140), scaledRect)
    }

    @Test
    fun `scale Rect with rounding for coordinates and dimensions`() {
        // Given
        val originalRect = Rect(10, 11, 21, 33)
        val scalingRatio = 0.75

        // When
        val scaledRect = originalRect.scale(scalingRatio)

        // Then
        val expectedRect = Rect(8, 8, 16, 25)
        Assert.assertEquals("Scaling $originalRect by $scalingRatio", expectedRect, scaledRect)
    }

    @Test
    fun `scale Rect starting at origin`() {
        // Given
        val originalRect = Rect(0, 0, 10, 20)
        val scalingRatio = 1.5

        // When
        val scaledRect = originalRect.scale(scalingRatio)

        // Then
        Assert.assertEquals(Rect(0, 0, 15, 30), scaledRect)
    }

    @Test
    fun `scale empty Rect at origin with ratio`() {
        // Given
        val originalRect = Rect(0, 0, 0, 0)
        val scalingRatio = 5.0

        // When
        val scaledRect = originalRect.scale(scalingRatio)

        // Then
        Assert.assertEquals(Rect(0, 0, 0, 0), scaledRect)
    }

    @Test
    fun `scale Rect with zero width or height`() {
        // Given
        val rectZeroWidth = Rect(10, 20, 10, 40)
        val rectZeroHeight = Rect(10, 20, 30, 20)
        val scalingRatio = 2.0

        // When
        val scaledRectZW = rectZeroWidth.scale(scalingRatio)
        val scaledRectZH = rectZeroHeight.scale(scalingRatio)

        // Then
        // For rectZeroWidth:
        // left = 20, top = 40, newWidth = 0, newHeight = 40 -> right = 20, bottom = 80
        Assert.assertEquals(Rect(20, 40, 20, 80), scaledRectZW)

        // For rectZeroHeight:
        // left = 20, top = 40, newWidth = 40, newHeight = 0 -> right = 60, bottom = 40
        Assert.assertEquals(Rect(20, 40, 60, 40), scaledRectZH)
    }

    @Test
    fun `scale Rect with ratio 0 should result in an empty Rect at scaled origin`() {
        // Given
        val originalRect = Rect(10, 20, 30, 50) // width = 20, height = 30
        val scalingRatio = 0.0

        // When
        val scaledRect = originalRect.scale(scalingRatio)

        // Then
        Assert.assertEquals(Rect(0, 0, 0, 0), scaledRect)
    }

    @Test
    fun `scale Rect that results in an inverted Rect due to extreme rounding`() {
        // This is an edge case to see how width/height rounding to 0 affects the outcome given a very small Rect
        val originalRect = Rect(10, 10, 11, 11) // width = 1, height = 1
        val scalingRatio = 0.1 // This will make width and height round to 0

        // When
        val scaledRect = originalRect.scale(scalingRatio)

        // Then
        Assert.assertEquals(Rect(1, 1, 1, 1), scaledRect)
    }

    @Test
    fun `grow Rect within bounds with default growValue`() {
        // Given
        val originalRect = Rect(10, 20, 30, 40)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 1 // Default

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(9, 19, 31, 41), grownRect)
    }

    @Test
    fun `grow Rect within bounds with explicit positive growValue`() {
        // Given
        val originalRect = Rect(10, 20, 30, 40)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 5

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(5, 15, 35, 45), grownRect)
    }

    @Test
    fun `grow Rect hitting left bound`() {
        // Given
        val originalRect = Rect(5, 20, 30, 40)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 10 // Will try to make left -5, but bounded by 0

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(0, 10, 40, 50), grownRect)
    }

    @Test
    fun `grow Rect hitting top bound`() {
        // Given
        val originalRect = Rect(10, 5, 30, 40)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 10 // Will try to make top -5, but bounded by 0

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(0, 0, 40, 50), grownRect)
    }

    @Test
    fun `grow Rect hitting right bound`() {
        // Given
        val originalRect = Rect(70, 20, 95, 40)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 10 // Will try to make right 105, but bounded by 100

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(60, 10, 100, 50), grownRect)
    }

    @Test
    fun `grow Rect hitting bottom bound`() {
        // Given
        val originalRect = Rect(10, 70, 30, 95)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 10 // Will try to make bottom 105, but bounded by 100

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(0, 60, 40, 100), grownRect)
    }

    @Test
    fun `grow Rect hitting all bounds`() {
        // Given
        val originalRect = Rect(5, 5, 95, 95)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 10

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(bounds, grownRect)
    }

    @Test
    fun `grow Rect when original is already at bounds`() {
        // Given
        val originalRect = Rect(0, 0, 100, 100)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 5

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(bounds, grownRect)
    }

    @Test
    fun `grow Rect with growValue 0 should return original Rect values`() {
        // Given
        val originalRect = Rect(10, 20, 30, 40)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 0

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(10, 20, 30, 40), grownRect)
    }

    @Test
    fun `grow Rect with negative growValue should shrink Rect`() {
        // Given
        val originalRect = Rect(10, 20, 30, 40)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = -5 // Effectively shrinking

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(15, 25, 25, 35), grownRect)
    }

    @Test
    fun `grow Rect with negative growValue causing it to invert (right less than left)`() {
        // Given
        val originalRect = Rect(10, 20, 15, 40) // width = 5
        val bounds = Rect(0, 0, 100, 100)
        val growValue = -3 // left = 10 - (-3) = 13, right = 15 + (-3) = 12

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(13, 23, 12, 37), grownRect)
    }

    @Test
    fun `grow Rect when original is outside bounds (top-left)`() {
        // Given
        val originalRect = Rect(-20, -20, -10, -10)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 5

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(0, 0, 0, 0), grownRect)
    }

    @Test
    fun `grow Rect when original is outside bounds (bottom-right)`() {
        // Given
        val originalRect = Rect(110, 110, 120, 120)
        val bounds = Rect(0, 0, 100, 100)
        val growValue = 5

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(Rect(100, 100, 100, 100), grownRect)
    }

    @Test
    fun `grow Rect with bounds that are smaller than the rect (results in bounds)`() {
        // Given
        val originalRect = Rect(0, 0, 200, 200)
        val bounds = Rect(50, 50, 150, 150)
        val growValue = 10

        // When
        val grownRect = originalRect.grow(bounds, growValue)

        // Then
        Assert.assertEquals(bounds, grownRect)
    }
}