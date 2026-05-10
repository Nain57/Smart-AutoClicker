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

#include <opencv2/imgproc/imgproc_c.h>
#include <opencv2/imgproc/imgproc.hpp>
#include "color_matching_result.hpp"

using namespace smartautoclicker;


void ColorMatchingResult::updateResults(
        const cv::Rect& detectionArea,
        double diffResult
) {
    area.x = detectionArea.x;
    area.y = detectionArea.y;
    area.width = detectionArea.width;
    area.height = detectionArea.height;
    centerX = area.x + ((int) (area.width / 2));
    centerY = area.y + ((int) (area.height / 2));
    colorDiffResult = diffResult;
}

void ColorMatchingResult::markResultAsDetected() {
    detected = true;
}

void ColorMatchingResult::reset() {
    detected = false;
    centerX = 0;
    centerY = 0;
    area.x = 0;
    area.y = 0;
    area.width = 0;
    area.height = 0;
    colorDiffResult = 0;
}

bool ColorMatchingResult::isDetected() const {
    return detected;
}

double ColorMatchingResult::getResultConfidence() const {
    return 1 - colorDiffResult;
}

cv::Rect ColorMatchingResult::getResultArea() const {
    return area;
}

int ColorMatchingResult::getResultAreaCenterX() const {
    return centerX;
}

int ColorMatchingResult::getResultAreaCenterY() const {
    return centerY;
}
