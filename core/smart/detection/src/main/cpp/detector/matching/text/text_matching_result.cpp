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
#include "text_matching_result.hpp"

using namespace smartautoclicker;


void TextMatchingResult::updateResults(
        const cv::Rect& detectionArea,
        const cv::Rect& boundingBox,
        float confidence,
        double numberRecognized
) {
    area.x = detectionArea.x + boundingBox.x;
    area.y = detectionArea.y + boundingBox.y;
    area.width = boundingBox.width;
    area.height = boundingBox.height;
    centerX = area.x + ((int) (area.width / 2));
    centerY = area.y + ((int) (area.height / 2));
    recognizerConfidence = confidence;
    recognizedNumber = numberRecognized;
}

void TextMatchingResult::markResultAsDetected() {
    detected = true;
}

void TextMatchingResult::reset() {
    detected = false;
    centerX = 0;
    centerY = 0;
    area.x = 0;
    area.y = 0;
    area.width = 0;
    area.height = 0;
    recognizerConfidence = 0.0;
    recognizedNumber = invalidNumber;
}

bool TextMatchingResult::isDetected() const {
    return detected;
}

double TextMatchingResult::getResultConfidence() const {
    return recognizerConfidence;
}

cv::Rect TextMatchingResult::getResultArea() const {
    return area;
}

int TextMatchingResult::getResultAreaCenterX() const {
    return centerX;
}

int TextMatchingResult::getResultAreaCenterY() const {
    return centerY;
}

int TextMatchingResult::getResultAreaWidth() const {
    return area.width;
}

int TextMatchingResult::getResultAreaHeight() const {
    return area.height;
}

double TextMatchingResult::getRecognizedNumber() const {
    return recognizedNumber;
}

