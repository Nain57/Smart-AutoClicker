/*
 * Copyright (C) 2023 Kevin Buzeau
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

#include "utils/roi.hpp"
#include "utils/scaling.hpp"
#include "utils/log.h"
#include "detector.hpp"

using namespace cv;
using namespace smartautoclicker;


void Detector::setScreenMetrics(cv::Mat* screenMat, double detectionQuality, const char *metricsTag) {
    // Select the scale ratio depending on the screen size.
    // We reduce the size to improve the processing time, but we don't want it to be too small because it will impact
    // the performance of the detection.
    scaleRatio = findBestScaleRatio(*screenMat, detectionQuality, metricsTag);
    delete screenMat;
}

void Detector::setScreenImage(cv::Mat* screenMat) {
    screenImage->processFullSizeBitmap(screenMat, scaleRatio);
}

DetectionResult Detector::detectCondition(cv::Mat* conditionMat, int threshold) {
    detectionResult.reset();

    // Load condition and check if the condition fits in the detection area
    ConditionImage conditionImage = ConditionImage();
    conditionImage.processFullSizeBitmap(conditionMat, scaleRatio);

    return detectCondition(conditionImage, screenImage->getRoi(), threshold);
}

DetectionResult Detector::detectCondition(cv::Mat* conditionMat, int x, int y, int width, int height, int threshold) {
    detectionResult.reset();

    // Load condition and check if the condition fits in the detection area
    ConditionImage conditionImage = ConditionImage();
    conditionImage.processFullSizeBitmap(conditionMat, scaleRatio);

    // Compute the detection area
    ScalableRoi detectionArea = ScalableRoi();
    detectionArea.setFullSize(cv::Rect(x, y, width, height), scaleRatio);

    return detectCondition(conditionImage, detectionArea, threshold);
}

DetectionResult Detector::detectCondition(ConditionImage& conditionImage, ScalableRoi detectionArea, int threshold) {
    // Check if the detection area fits the screen
    if (!screenImage->getRoi().containsOrEquals(detectionArea)) {
        LOGE("Detector", "Can't detectCondition, detection roi is outside the screen");
        return detectionResult;
    }

    // Check if the condition fits in the detection area
    if (!detectionArea.isBiggerOrEquals(conditionImage.getRoi())) {
        LOGE("Detector", "Can't detectCondition, detection roi is smaller than the condition");
        return detectionResult;
    }

    // Crop the scaled gray current image to only get the detection area
    cv::Mat screenCroppedScaledGrayMat = screenImage->cropScaledGray(detectionArea.getScaled());

    // Get the matching results
    auto matchingResults = matchTemplate(screenCroppedScaledGrayMat, *conditionImage.getScaledGrayColorMat());

    // Until a condition is detected or none fits
    cv::Rect scaledMatchingRoi;
    cv::Rect fullSizeMatchingRoi;
    detectionResult.isDetected = false;
    while (!detectionResult.isDetected) {
        // Find the max value and its position in the result
        locateMinMax(*matchingResults, detectionResult);

        // If the maximum for the whole picture is below the threshold, we will never find.
        if (!isResultAboveThreshold(detectionResult, threshold)) break;

        // Calculate the ROI based on the maximum location
        scaledMatchingRoi = getRoiForResult(detectionResult.maxLoc, *conditionImage.getScaledGrayColorMat());
        fullSizeMatchingRoi = getDetectionResultFullSizeRoi(
                detectionArea.getFullSize(),
                conditionImage.getRoi().getFullSize().width,
                conditionImage.getRoi().getFullSize().height);

        if (isRoiNotContainingImage(scaledMatchingRoi, *conditionImage.getScaledGrayColorMat()) ||
            isRoiNotContainedInImage(fullSizeMatchingRoi, *screenImage->getFullSizeColorMat())) {
/*
            LOGE("Detector", "roi is out of bound, scaleRatio %1f", scaleRatio);
            LOGE("Detector", "roi is out of bound, conditionImage %1d/%2d - %3d/%4d", conditionImage.getRoi().getFullSize().width, conditionImage.getRoi().getFullSize().height, conditionImage.getRoi().getScaled().width, conditionImage.getRoi().getScaled().height);
            LOGE("Detector", "roi is out of bound, detectionArea %1d/%2d - %3d/%4d", detectionArea.getFullSize().width, detectionArea.getFullSize().height, detectionArea.getScaled().width, detectionArea.getScaled().height);

            LOGE("Detector", "roi is out of bound, scaledMatchingRoi %1d/%2d %3d/%4d - %7d/%8d - %9d",
                 scaledMatchingRoi.x, scaledMatchingRoi.y, scaledMatchingRoi.width, scaledMatchingRoi.height,
                 conditionImage.getScaledGrayColorMat()->cols, conditionImage.getScaledGrayColorMat()->rows,
                 isRoiNotContainingImage(scaledMatchingRoi, *conditionImage.getScaledGrayColorMat()));
            LOGE("Detector", "roi is out of bound, fullSizeMatchingRoi %1d/%2d %3d/%4d - %7d/%8d - %9d",
                 fullSizeMatchingRoi.x, fullSizeMatchingRoi.y, fullSizeMatchingRoi.width, fullSizeMatchingRoi.height,
                 screenImage->getFullSizeColorMat()->cols, screenImage->getFullSizeColorMat()->rows,
                 isRoiNotContainedInImage(fullSizeMatchingRoi, *screenImage->getFullSizeColorMat()));
*/
            // Roi is out of bounds, invalid match
            detectionResult.centerX = 0;
            detectionResult.centerY = 0;
            markRoiAsInvalidInResults(scaledMatchingRoi, *matchingResults);
            continue;
        }

        detectionResult.centerX = fullSizeMatchingRoi.x + ((int) (fullSizeMatchingRoi.width / 2));
        detectionResult.centerY = fullSizeMatchingRoi.y + ((int) (fullSizeMatchingRoi.height / 2));

        // Check if the colors are matching in the candidate area.
        cv::Mat fullSizeColorCroppedCurrentImage = screenImage->cropFullSizeColor(fullSizeMatchingRoi);
        double colorDiff = getColorDiff(fullSizeColorCroppedCurrentImage, conditionImage.getFullSizeColorMean());
        if (colorDiff < threshold) {
            detectionResult.isDetected = true;
        } else {
            // Colors are invalid, modify the matching result to indicate that.
            markRoiAsInvalidInResults(scaledMatchingRoi, *matchingResults);
        }
    }

    return detectionResult;
}

std::unique_ptr<Mat> Detector::matchTemplate(const Mat& image, const Mat& condition) {
    cv::Mat resultMat(max(image.rows - condition.rows + 1, 0),
                      max(image.cols - condition.cols + 1, 0),
                      CV_32F);

    cv::matchTemplate(image, condition, resultMat, cv::TM_CCOEFF_NORMED);

    return std::make_unique<cv::Mat>(resultMat);
}

void Detector::locateMinMax(const Mat& matchingResult, DetectionResult& results) {
    minMaxLoc(matchingResult, &results.minVal, &results.maxVal, &results.minLoc, &results.maxLoc, Mat());
}

bool Detector::isResultAboveThreshold(const DetectionResult& results, const int threshold) {
    return results.maxVal > ((double) (100 - threshold) / 100);
}

double Detector::getColorDiff(const cv::Mat& image, const cv::Scalar& conditionColorMeans) {
    auto imageColorMeans = mean(image);

    double diff = 0;
    for (int i = 0; i < 3; i++) {
        diff += abs(imageColorMeans.val[i] - conditionColorMeans.val[i]);
    }
    return (diff * 100) / (255 * 3);
}

cv::Rect Detector::getDetectionResultFullSizeRoi(const cv::Rect& fullSizeDetectionRoi, int fullSizeWidth, int fullSizeHeight) const {
    return {
            fullSizeDetectionRoi.x + cvRound(detectionResult.maxLoc.x / scaleRatio),
            fullSizeDetectionRoi.y + cvRound(detectionResult.maxLoc.y / scaleRatio),
            fullSizeWidth,
            fullSizeHeight
    };
}

