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

#include <opencv2/imgproc/imgproc_c.h>
#include "template_matching_result.hpp"

using namespace smartautoclicker;


void TemplateMatchingResult::updateResults(ScalableRoi* detectionArea, cv::Mat* condition,
                                           cv::Mat* matchingResults, double scaleRatio) {

    cv::minMaxLoc(*matchingResults, &minVal, &maxVal, &minLoc, &maxLoc, cv::Mat());

    area.setScaled(
            detectionArea->getScaled().x + maxLoc.x,
            detectionArea->getScaled().y + maxLoc.y,
            condition->cols, condition->rows, scaleRatio);
    centerX = area.getFullSize().x + ((int) (area.getFullSize().width / 2));
    centerY = area.getFullSize().y + ((int) (area.getFullSize().height / 2));
}

void TemplateMatchingResult::markResultAsDetected() {
    detected = true;
}

void TemplateMatchingResult::reset() {
    detected = false;
    centerX = 0;
    centerY = 0;
    minVal = 0;
    maxVal = 0;
    minLoc.x = 0;
    minLoc.y = 0;
    maxLoc.x = 0;
    maxLoc.y = 0;
    area.clear();
}

bool TemplateMatchingResult::isDetected() const {
    return detected;
}

double TemplateMatchingResult::getResultConfidence() const {
    return maxVal;
}

ScalableRoi TemplateMatchingResult::getResultArea() const {
    return area;
}

int TemplateMatchingResult::getResultAreaCenterX() const {
    return centerX;
}

int TemplateMatchingResult::getResultAreaCenterY() const {
    return centerY;
}
