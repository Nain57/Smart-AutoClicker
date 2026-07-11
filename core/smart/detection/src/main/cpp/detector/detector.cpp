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
#include <android/log.h>
#include <android/bitmap.h>
#include <memory>
#include <opencv2/imgproc/imgproc_c.h>

#include "../logs/log.h"
#include "../utils/roi.h"
#include "detector.hpp"

using namespace cv;
using namespace smartautoclicker;


bool Detector::loadModels(const std::string& detectionModelPath, const std::map<std::string, std::string>& recognitionModels) {
    return textMatcher->init(detectionModelPath, recognitionModels);
}

void Detector::setScreenImage(std::unique_ptr<cv::Mat> screenColorMat, const char* metricsTag) {
    screenImage->processNewData(std::move(screenColorMat), metricsTag);
}

TemplateMatchingResult* Detector::detectImage(
        std::unique_ptr<cv::Mat> conditionMat,
        int targetConditionWidth,
        int targetConditionHeight,
        const cv::Rect& roi,
        int threshold
) {
    templateMatcher->reset();

    // Load condition and resize to requested size
    conditionImage->processNewData(
            std::move(conditionMat),
            targetConditionWidth,
            targetConditionHeight);

    // Check if the condition fits in the detection area
    if (!TemplateMatcher::isRoiValidForMatching(screenImage->getRoi(), conditionImage->getRoi(), roi)) {
        return templateMatcher->getMatchingResults();
    }

    // Apply template matching and get global results
    templateMatcher->matchTemplate(
            *screenImage,
            *conditionImage,
            roi,
            threshold);

    return templateMatcher->getMatchingResults();
}

ColorMatchingResult* Detector::detectColor(int colorCondition, const cv::Rect& roi, int threshold) {
    colorMatcher->reset();

    // Verify area validity
    if (!ColorMatcher::isRoiValidForMatching(screenImage->getRoi(), roi)) {
        return colorMatcher->getMatchingResults();
    }

    // Create the color int (RGBA) into a scalar of size 3 (RGB)
    cv::Scalar conditionColor(
            (double)((colorCondition >> 16) & 0xFF),
            (double)((colorCondition >> 8) & 0xFF),
            (double)(colorCondition & 0xFF));

    // Apply color matching and get global results.
    colorMatcher->matchColor(
            *screenImage,
            conditionColor,
            roi,
            threshold);

    return colorMatcher->getMatchingResults();
}

TextMatchingResult* Detector::detectText(const char* textCondition, const char* recognitionModelId, const cv::Rect& roi, int threshold) {
    return textMatcher->matchText(
            *screenImage,
            std::string(textCondition),
            std::string(recognitionModelId),
            roi,
            threshold);
}

TextMatchingResult* Detector::detectNumber(const cv::Rect& roi, int threshold, NumberFormat numberFormat) {
    return textMatcher->matchNumber(*screenImage, roi, threshold, numberFormat);
}
