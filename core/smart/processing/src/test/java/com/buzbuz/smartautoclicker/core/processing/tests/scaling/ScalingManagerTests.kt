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

import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfig
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.model.DetectionType
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.processing.data.scaling.ScalingManager

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config
import kotlin.intArrayOf

import org.mockito.Mockito.`when` as mockWhen

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScalingManagerTests {

    private companion object {
        private const val TEST_MAX_QUALITY = 10000.0
        private val TEST_DEFAULT_SCREEN_SIZE = Point(1080, 1920)
    }

    private lateinit var scalingManager: ScalingManager
    @Mock private lateinit var mockDisplayConfigManager: DisplayConfigManager


    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)

        scalingManager = ScalingManager(mockDisplayConfigManager)
    }

    @Test
    fun `quality above max should result in scaledScreenSize unchanged`() {
        // Given
        val event1Id = 1L
        val screenSize = Point(1000, 1000)
        val condition1Area = Rect(100, 100, 200, 200)
        val condition1 = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = condition1Area,
            type = EXACT,
        )
        val event1 = createTestEvent(event1Id, listOf(condition1))

        // When
        mockDisplayConfigManager.mockDisplayConfig(screenSize)
        val scaledScreenSize = scalingManager.startScaling(TEST_MAX_QUALITY, listOf(event1))

        // Then
        Assert.assertEquals("scaledScreenSize should be original screen size",
            screenSize, scaledScreenSize)
    }

    @Test
    fun `quality below max should change scaledScreenSize accordingly`() {
        // Given
        val event1Id = 1L
        val screenSize = Point(1000, 2000) // Max side = 2000
        val quality = 1000.0 // Results in scalingRatio = 1000.0 / 2000.0 = 0.5
        val condition1Area = Rect(100, 100, 200, 200)
        val condition1 = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = condition1Area,
            type = EXACT,
        )
        val event1 = createTestEvent(event1Id, listOf(condition1))

        // When
        mockDisplayConfigManager.mockDisplayConfig(screenSize)
        val scaledScreenSize = scalingManager.startScaling(quality, listOf(event1))

        // Then
        Assert.assertEquals(Point(500, 1000), scaledScreenSize)
    }

    @Test
    fun `scaledScreenSize should be refreshed upon refreshScaling call`() {
        // Given
        val event1Id = 1L
        val screenSize = Point(1000, 2000) // Max side = 2000
        val quality = 1000.0 // Results in scalingRatio = 1000.0 / 2000.0 = 0.5
        val condition1Area = Rect(100, 100, 200, 200)
        val condition1 = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = condition1Area,
            type = EXACT,
        )
        val event1 = createTestEvent(event1Id, listOf(condition1))

        // When
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)
        scalingManager.startScaling(quality, listOf(event1))

        mockDisplayConfigManager.mockDisplayConfig(screenSize)
        val refreshedScreenSize = scalingManager.refreshScaling()

        // Then
        Assert.assertEquals(Point(500, 1000), refreshedScreenSize)
    }

    @Test
    fun `conditionScalingInfo correctly populated for multiple conditions and events`() {
        // Given
        val event1Id = 1L
        val event2Id = 2L
        val condition1 = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = Rect(10, 20, 30, 40),
            type = EXACT,
        )
        val condition2 = createTestCondition(
            id = 2L,
            evtId = event1Id,
            area = Rect(50, 60, 80, 90),
            type = EXACT,
        )
        val condition3 = createTestCondition(
            id = 3L,
            evtId = event2Id,
            area = Rect(100, 110, 120, 130),
            type = EXACT,
        )
        val event1 = createTestEvent(event1Id, listOf(condition1, condition2))
        val event2 = createTestEvent(event2Id, listOf(condition3))

        // When
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)
        scalingManager.startScaling(TEST_MAX_QUALITY, listOf(event1, event2))

        // Then
        Assert.assertNotNull(scalingManager.getImageConditionScalingInfo(condition1))
        Assert.assertNotNull(scalingManager.getImageConditionScalingInfo(condition2))
        Assert.assertNotNull(scalingManager.getImageConditionScalingInfo(condition3))
    }

    @Test
    fun `conditionScalingInfo correctly cleared when scaling is stopped`() {
        // Given
        val event1Id = 1L
        val event2Id = 2L
        val condition1 = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = Rect(10, 20, 30, 40),
            type = EXACT,
        )
        val condition2 = createTestCondition(
            id = 2L,
            evtId = event1Id,
            area = Rect(50, 60, 80, 90),
            type = EXACT,
        )
        val condition3 = createTestCondition(
            id = 3L,
            evtId = event2Id,
            area = Rect(100, 110, 120, 130),
            type = EXACT,
        )
        val event1 = createTestEvent(event1Id, listOf(condition1, condition2))
        val event2 = createTestEvent(event2Id, listOf(condition3))

        // When
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)
        scalingManager.startScaling(TEST_MAX_QUALITY, listOf(event1, event2))
        scalingManager.stopScaling()

        // Then
        Assert.assertNull(scalingManager.getImageConditionScalingInfo(condition1))
        Assert.assertNull(scalingManager.getImageConditionScalingInfo(condition2))
        Assert.assertNull(scalingManager.getImageConditionScalingInfo(condition3))
    }

    @Test
    fun `conditionScalingInfo is valid for EXACT condition with no scaling`() {
        // Given
        val event1Id = 1L
        val conditionArea = Rect(10, 20, 30, 40)
        val condition = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = conditionArea,
            type = EXACT,
        )
        val event1 = createTestEvent(event1Id, listOf(condition))

        // When
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)
        scalingManager.startScaling(TEST_MAX_QUALITY, listOf(event1))

        // Then
        val scalingInfo = scalingManager.getImageConditionScalingInfo(condition)
        Assert.assertNotNull(scalingInfo)
        Assert.assertEquals(condition, scalingInfo!!.imageCondition)
        Assert.assertEquals(conditionArea, scalingInfo.imageArea)
        Assert.assertEquals(Rect(9, 19, 31, 41), scalingInfo.detectionArea)
    }

    @Test
    fun `conditionScalingInfo is valid for EXACT condition with scaling`() {
        // Given
        val event1Id = 1L
        val screenSize = Point(1000, 2000) // Max side = 2000
        val quality = 1000.0 // Results in scalingRatio = 1000.0 / 2000.0 = 0.5
        val conditionArea = Rect(100, 100, 200, 200)
        val condition = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = conditionArea,
            type = EXACT,
        )
        val event1 = createTestEvent(event1Id, listOf(condition))

        // When
        mockDisplayConfigManager.mockDisplayConfig(screenSize)
        scalingManager.startScaling(quality, listOf(event1))

        // Then
        val scalingInfo = scalingManager.getImageConditionScalingInfo(condition)
        Assert.assertNotNull(scalingInfo)
        Assert.assertEquals(condition, scalingInfo!!.imageCondition)
        Assert.assertEquals(Rect(50, 50, 100, 100), scalingInfo.imageArea)
        Assert.assertEquals(Rect(49, 49, 101, 101), scalingInfo.detectionArea)
    }

    @Test
    fun `conditionScalingInfo correctly refreshed for EXACT condition`() {
        // Given
        val event1Id = 1L
        val screenSize = Point(1000, 2000) // Max side = 2000
        val quality = 1000.0 // Results in scalingRatio = 1000.0 / 2000.0 = 0.5
        val conditionArea = Rect(100, 100, 200, 200)
        val condition = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = conditionArea,
            type = EXACT,
        )
        val event1 = createTestEvent(event1Id, listOf(condition))

        // When
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)
        scalingManager.startScaling(quality, listOf(event1))
        mockDisplayConfigManager.mockDisplayConfig(screenSize)
        scalingManager.refreshScaling()

        // Then
        val scalingInfo = scalingManager.getImageConditionScalingInfo(condition)
        Assert.assertNotNull(scalingInfo)
        Assert.assertEquals(condition, scalingInfo!!.imageCondition)
        Assert.assertEquals(Rect(50, 50, 100, 100), scalingInfo.imageArea)
        Assert.assertEquals(Rect(49, 49, 101, 101), scalingInfo.detectionArea)
    }

    @Test
    fun `conditionScalingInfo is valid for WHOLE_SCREEN condition with no scaling`() {
        // Given
        val event1Id = 1L
        val conditionArea = Rect(10, 20, 30, 40)
        val condition = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = conditionArea,
            type = WHOLE_SCREEN,
        )
        val event1 = createTestEvent(event1Id, listOf(condition))

        // When
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)
        scalingManager.startScaling(TEST_MAX_QUALITY, listOf(event1))

        // Then
        val scalingInfo = scalingManager.getImageConditionScalingInfo(condition)
        Assert.assertNotNull(scalingInfo)
        Assert.assertEquals(condition, scalingInfo!!.imageCondition)
        Assert.assertEquals(conditionArea, scalingInfo.imageArea)
        Assert.assertEquals(
            Rect(0, 0, TEST_DEFAULT_SCREEN_SIZE.x, TEST_DEFAULT_SCREEN_SIZE.y),
            scalingInfo.detectionArea,
        )
    }

    @Test
    fun `conditionScalingInfo is valid for WHOLE_SCREEN condition with scaling`() {
        // Given
        val event1Id = 1L
        val screenSize = Point(1000, 2000) // Max side = 2000
        val quality = 1000.0 // Results in scalingRatio = 1000.0 / 2000.0 = 0.5
        val conditionArea = Rect(100, 100, 200, 200)
        val condition = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = conditionArea,
            type = WHOLE_SCREEN,
        )
        val event1 = createTestEvent(event1Id, listOf(condition))

        // When
        mockDisplayConfigManager.mockDisplayConfig(screenSize)
        scalingManager.startScaling(quality, listOf(event1))

        // Then
        val scalingInfo = scalingManager.getImageConditionScalingInfo(condition)
        Assert.assertNotNull(scalingInfo)
        Assert.assertEquals(condition, scalingInfo!!.imageCondition)
        Assert.assertEquals(Rect(50, 50, 100, 100), scalingInfo.imageArea)
        Assert.assertEquals(Rect(0, 0, 500, 1000), scalingInfo.detectionArea)
    }

    @Test
    fun `conditionScalingInfo correctly refreshed for WHOLE_SCREEN condition`() {
        // Given
        val event1Id = 1L
        val screenSize = Point(1000, 2000) // Max side = 2000
        val quality = 1000.0 // Results in scalingRatio = 1000.0 / 2000.0 = 0.5
        val conditionArea = Rect(100, 100, 200, 200)
        val condition = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = conditionArea,
            type = WHOLE_SCREEN,
        )
        val event1 = createTestEvent(event1Id, listOf(condition))

        // When
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)
        scalingManager.startScaling(quality, listOf(event1))
        mockDisplayConfigManager.mockDisplayConfig(screenSize)
        scalingManager.refreshScaling()

        // Then
        val scalingInfo = scalingManager.getImageConditionScalingInfo(condition)
        Assert.assertNotNull(scalingInfo)
        Assert.assertEquals(condition, scalingInfo!!.imageCondition)
        Assert.assertEquals(Rect(50, 50, 100, 100), scalingInfo.imageArea)
        Assert.assertEquals(Rect(0, 0, 500, 1000), scalingInfo.detectionArea)
    }

    @Test
    fun `conditionScalingInfo is valid for IN_AREA condition with no scaling`() {
        // Given
        val event1Id = 1L
        val conditionArea = Rect(10, 20, 30, 40)
        val detectionArea = Rect(10, 10, 50, 50)
        val condition = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = conditionArea,
            type = IN_AREA,
            detectionArea = detectionArea,
        )
        val event1 = createTestEvent(event1Id, listOf(condition))

        // When
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)
        scalingManager.startScaling(TEST_MAX_QUALITY, listOf(event1))

        // Then
        val scalingInfo = scalingManager.getImageConditionScalingInfo(condition)
        Assert.assertNotNull(scalingInfo)
        Assert.assertEquals(condition, scalingInfo!!.imageCondition)
        Assert.assertEquals(conditionArea, scalingInfo.imageArea)
        Assert.assertEquals(Rect(9, 9, 51, 51), scalingInfo.detectionArea)
    }

    @Test
    fun `conditionScalingInfo is valid for IN_AREA condition with scaling`() {
        // Given
        val event1Id = 1L
        val screenSize = Point(1000, 2000) // Max side = 2000
        val quality = 1000.0 // Results in scalingRatio = 1000.0 / 2000.0 = 0.5
        val conditionArea = Rect(100, 100, 200, 200)
        val detectionArea = Rect(10, 10, 500, 500)
        val condition = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = conditionArea,
            type = IN_AREA,
            detectionArea = detectionArea,
        )
        val event1 = createTestEvent(event1Id, listOf(condition))

        // When
        mockDisplayConfigManager.mockDisplayConfig(screenSize)
        scalingManager.startScaling(quality, listOf(event1))

        // Then
        val scalingInfo = scalingManager.getImageConditionScalingInfo(condition)
        Assert.assertNotNull(scalingInfo)
        Assert.assertEquals(condition, scalingInfo!!.imageCondition)
        Assert.assertEquals(Rect(50, 50, 100, 100), scalingInfo.imageArea)
        Assert.assertEquals(Rect(4, 4, 251, 251), scalingInfo.detectionArea)
    }

    @Test
    fun `conditionScalingInfo correctly refreshed for IN_AREA condition`() {
        // Given
        val event1Id = 1L
        val screenSize = Point(1000, 2000) // Max side = 2000
        val quality = 1000.0 // Results in scalingRatio = 1000.0 / 2000.0 = 0.5
        val conditionArea = Rect(100, 100, 200, 200)
        val detectionArea = Rect(10, 10, 500, 500)
        val condition = createTestCondition(
            id = 1L,
            evtId = event1Id,
            area = conditionArea,
            type = IN_AREA,
            detectionArea = detectionArea,
        )
        val event1 = createTestEvent(event1Id, listOf(condition))

        // When
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)
        scalingManager.startScaling(quality, listOf(event1))
        mockDisplayConfigManager.mockDisplayConfig(screenSize)
        scalingManager.refreshScaling()

        // Then
        val scalingInfo = scalingManager.getImageConditionScalingInfo(condition)
        Assert.assertNotNull(scalingInfo)
        Assert.assertEquals(condition, scalingInfo!!.imageCondition)
        Assert.assertEquals(Rect(50, 50, 100, 100), scalingInfo.imageArea)
        Assert.assertEquals(Rect(4, 4, 251, 251), scalingInfo.detectionArea)
    }

    @Test
    fun `scaleUpDetectionResult with no scaling (ratio 1) returns same point coordinates`() {
        // Given
        val pointToScale = Point(50, 100)

        // When
        mockDisplayConfigManager.mockDisplayConfig(TEST_DEFAULT_SCREEN_SIZE)
        scalingManager.startScaling(TEST_MAX_QUALITY, emptyList())
        val scaledUpPoint = scalingManager.scaleUpDetectionResult(pointToScale)

        // Then
        Assert.assertEquals(pointToScale, scaledUpPoint)
    }

    @Test
    fun `scaleUpDetectionResult correctly scales up a point`() {
        // Given
        val pointToScale = Point(50, 100)
        val quality = 1000.0 // ratio = 0.5

        // When
        mockDisplayConfigManager.mockDisplayConfig(Point(1000, 2000))
        scalingManager.startScaling(quality, emptyList())
        val scaledUpPoint = scalingManager.scaleUpDetectionResult(pointToScale)

        // Then
        Assert.assertEquals(Point( 100, 200), scaledUpPoint)
    }


    private fun DisplayConfigManager.mockDisplayConfig(screenSize: Point) {
        mockWhen(displayConfig)
            .thenReturn(
                DisplayConfig(
                    sizePx = screenSize,
                    orientation = Configuration.ORIENTATION_PORTRAIT, // Unused in scaling
                    safeInsetTopPx = 0,                               // Unused in scaling
                    roundedCorners = emptyMap(),                      // Unused in scaling
                )
            )
    }

    private fun createTestEvent(id: Long, conditions: List<ImageCondition>) : ImageEvent =
        ImageEvent(
            id = Identifier(databaseId = id),
            conditions = conditions,
            scenarioId = Identifier(databaseId = 1),    // Unused in scaling
            name = "",                                  // Unused in scaling
            conditionOperator = 0,                      // Unused in scaling
            actions = emptyList(),                      // Unused in scaling
            enabledOnStart = true,                      // Unused in scaling
            priority = 0,                               // Unused in scaling
            keepDetecting = false,                      // Unused in scaling
        )

    private fun createTestCondition(
        id: Long,
        evtId: Long,
        area: Rect,
        @DetectionType type: Int,
        detectionArea: Rect? = null,
    ): ImageCondition =
        ImageCondition(
            id = Identifier(databaseId = id),
            eventId = Identifier(databaseId = evtId),
            area = area,
            detectionType = type,
            detectionArea = detectionArea,
            name = "",                                  // Unused in scaling
            path = "",                                  // Unused in scaling
            threshold = 0,                              // Unused in scaling
            priority = 0,                               // Unused in scaling
            shouldBeDetected = false,                   // Unused in scaling
        )
}