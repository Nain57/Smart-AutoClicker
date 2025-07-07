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

#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgproc/imgproc_c.h>

#include "template_matcher.hpp"
#include "../../logs/log.h"
#include "../../utils/roi.h"


using namespace smartautoclicker;


void TemplateMatcher::reset() {
    currentMatchingResult.reset();
}

TemplateMatchingResult *TemplateMatcher::getMatchingResults() {
    return &currentMatchingResult;
}

void TemplateMatcher::matchTemplate(
        const ScreenImage& screenImage,
        const ConditionImage& condition,
        const cv::Rect& detectionArea,
        int threshold
) {

    // Crop the gray screen image to get only the detection area
    cv::Mat screenCroppedGrayMat = screenImage.cropGray(detectionArea);
    if (screenCroppedGrayMat.empty()) {
        LOGE("TemplateMatcher", "screenCroppedGrayMat is empty after cropping.");
        return;
    }

    // Initialize result mat
    cv::Mat newResultsMat = cv::Mat(
            std::max(screenCroppedGrayMat.rows - condition.getGrayMat()->rows + 1, 0),
            std::max(screenCroppedGrayMat.cols - condition.getGrayMat()->cols + 1, 0),
            CV_32F);

    // Run OpenCv template matching
    cv::matchTemplate(
            screenCroppedGrayMat,
            *condition.getGrayMat(),
            newResultsMat,
            cv::TM_CCOEFF_NORMED);

    // Parse result Mat to check for matching
    parseMatchingResult(screenImage, condition, detectionArea, threshold, newResultsMat);
}

void TemplateMatcher::parseMatchingResult(
        const ScreenImage& screenImage,
        const ConditionImage& condition,
        const cv::Rect& detectionArea,
        int threshold,
        cv::Mat& matchingResult
) {

    while (!currentMatchingResult.isDetected()) {

        // Mark previous results as invalid, if any
        if (!currentMatchingResult.getResultArea().empty()) {
            currentMatchingResult.invalidateCurrentResult(
                    *condition.getGrayMat(),
                    matchingResult);
        }

        // Look for new best match
        currentMatchingResult.updateResults(
                detectionArea,
                *condition.getGrayMat(),
                matchingResult);

        // Check if the highest result is above threshold. If not, we will never find.
        if (!isConfidenceValid(currentMatchingResult.getResultConfidence(), threshold)) break;

        // Check if result area is valid. If not, check next possible match
        if (!isRoiBiggerOrEquals(screenImage.getRoi(), currentMatchingResult.getResultArea())) continue;

        // Check if the colors are matching in the candidate area.
        cv::Mat fullSizeColorCroppedCurrentImage = screenImage.cropColor(currentMatchingResult.getResultArea());
        double colorDiff = getColorDiff(fullSizeColorCroppedCurrentImage,condition.getColorMean());

        // If the colors are OK, the result is valid
        if (colorDiff < threshold) currentMatchingResult.markResultAsDetected();
    }
}

bool TemplateMatcher::isConfidenceValid(double confidence, int threshold) {
    return confidence > ((100.0 - threshold) / 100.0);
}

double TemplateMatcher::getColorDiff(const cv::Mat& image, const cv::Scalar& conditionColorMeans) {
   auto imageColorMeans = mean(image);

   double diff = 0;
   for (int i = 0; i < 3; i++) {
       diff += abs(imageColorMeans.val[i] - conditionColorMeans.val[i]);
   }
   return (diff * 100) / (255 * 3);
}