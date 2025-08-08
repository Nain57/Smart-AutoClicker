
package com.buzbuz.smartautoclicker.core.detection.data

import android.graphics.Point

internal data class TestResults(
    val expectedCenterPosition: Point,
    val actualCenterPosition: Point,
    val expectedConfidence: Double,
    val actualConfidence: Double,
)


internal fun TestResults.isValid(): Boolean =
    actualConfidence >= expectedConfidence &&
            isCenterPositionValid(
                expected = expectedCenterPosition,
                actual = actualCenterPosition,
                delta = 5,
            )

private fun isCenterPositionValid(expected: Point, actual: Point, delta: Int): Boolean =
    isCenterPositionValid(expected.x, actual.x, delta) && isCenterPositionValid(expected.y, actual.y, delta)

private fun isCenterPositionValid(expected: Int, actual: Int, delta: Int) : Boolean =
    actual in (expected - delta)..(expected + delta)
