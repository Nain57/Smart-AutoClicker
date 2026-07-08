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

#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgproc/imgproc_c.h>

#include "color_matcher.hpp"
#include "../../../logs/log.h"
#include "../../../utils/roi.h"


using namespace smartautoclicker;


void ColorMatcher::reset() {
    currentMatchingResult.reset();
}

ColorMatchingResult *ColorMatcher::getMatchingResults() {
    return &currentMatchingResult;
}

bool ColorMatcher::isRoiValidForMatching(const cv::Rect& screenRoi, const cv::Rect& roi) {
    if (roi.width <= 0 || roi.height <= 0 || !isRoiContainsOrEquals(screenRoi, roi)) {
        LOGD("Detector", "Can't detect color, detection area (x=%d, y=%d, w=%d, h=%d) is not contained in screen (w=%d, h=%d)",
             roi.x, roi.y, roi.width, roi.height,
             screenRoi.width, screenRoi.height);
        return false;
    }

    return true;
}

void ColorMatcher::matchColor(
        const ScreenImage& screenImage,
        const cv::Scalar& conditionColor,
        const cv::Rect& detectionArea,
        int threshold
) {

    // Crop the color screen image to get only the detection area
    cv::Mat screenCroppedColorMat = screenImage.cropColor(detectionArea);
    if (screenCroppedColorMat.empty()) {
        LOGE("ColorMatcher", "screenCroppedColorMat is empty after cropping.");
        return;
    }

    // Compute the difference between each channel color (RGB)
    auto imageColorMeans = mean(screenCroppedColorMat);
    double diff = 0;
    for (int i = 0; i < 3; i++) {
        diff += abs(imageColorMeans.val[i] - conditionColor.val[i]);
    }
    diff = diff / (255 * 3);

    currentMatchingResult.updateResults(detectionArea, diff);

    // If the colors are OK, the result is valid
    if ((diff * 100) <= threshold) currentMatchingResult.markResultAsDetected();
}
