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
#include <android/log.h>
#include <android/bitmap.h>
#include <memory>
#include <opencv2/imgproc/imgproc_c.h>

#include "../scaling/scaling.hpp"
#include "../logs/log.h"
#include "detector.hpp"

using namespace cv;
using namespace smartautoclicker;


void Detector::setTextLanguages(const char *langCodes, const char *dataPath) {
    textMatcher->setLanguages(langCodes, dataPath);
}

void Detector::setScreenMetrics(cv::Mat* screenMat, double detectionQuality, const char *metricsTag) {
    // Select the scale ratio depending on the screen size.
    // We reduce the size to improve the processing time, but we don't want it to be too small because it will impact
    // the performance of the detection.
    scaleRatio = findBestScaleRatio(*screenMat, detectionQuality, metricsTag);
    delete screenMat;
}

void Detector::setScreenImage(cv::Mat* screenMat) {
    screenImage->processFullSizeBitmap(screenMat, scaleRatio);
    textMatcher->setScreenImage(*screenImage);
}

DetectionResult* Detector::detectCondition(cv::Mat* conditionMat, int threshold) {
    return detectCondition(
            conditionMat,
            0, 0,
            screenImage->getRoi().getFullSize().width,
            screenImage->getRoi().getFullSize().height,
            threshold);
}

DetectionResult* Detector::detectCondition(cv::Mat* conditionMat, int x, int y, int width, int height, int threshold) {
    templateMatcher->reset();

    // Load condition and check if the condition fits in the detection area
    ConditionImage conditionImage = ConditionImage();
    conditionImage.processFullSizeBitmap(conditionMat, scaleRatio);

    // Compute the detection area
    ScalableRoi detectionArea = ScalableRoi();
    detectionArea.setFullSize(x, y, width, height, scaleRatio,screenImage->getRoi());

    // Check if the condition fits in the detection area
    if (!detectionArea.isBiggerOrEquals(conditionImage.getRoi())) {
        LOGE("Detector", "Can't detectCondition, detection roi is smaller than the condition");
        return templateMatcher->getMatchingResults();
    }

    // Crop the scaled gray current image to only get the detection area
    cv::Mat screenCroppedScaledGrayMat = screenImage->cropScaledGray(detectionArea.getScaled());

    // Apply template matching and get global results
    templateMatcher->matchTemplate(
            screenImage.get(),
            &conditionImage,
            &detectionArea,
            scaleRatio,
            threshold);

    return templateMatcher->getMatchingResults();
}

DetectionResult* Detector::detectText(const std::string& text, int threshold) {
    return detectText(
            text,
            0, 0,
            screenImage->getRoi().getFullSize().width,
            screenImage->getRoi().getFullSize().height,
            threshold);
}

DetectionResult* Detector::detectText(const std::string& text, int x, int y, int width, int height, int threshold) {
    textMatcher->reset();

    // Compute the detection area
    ScalableRoi detectionArea = ScalableRoi();
    detectionArea.setFullSize(x, y, width, height, scaleRatio, screenImage->getRoi());

    textMatcher->matchText(text, detectionArea, scaleRatio, threshold);

    return textMatcher->getMatchingResults();
}
