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

#include "utils/androidBitmap.hpp"
#include "utils/roi.hpp"
#include "detector.hpp"

using namespace cv;
using namespace smartautoclicker;

void Detector::setScreenMetrics(JNIEnv *env, jobject screenImage, double detectionQuality) {
    // Initial the current image mat. When the size of the image change (e.g. rotation), this method should be called
    // to update it.
    fullSizeColorCurrentImage = createColorMatFromARGB8888BitmapData(env, screenImage);

    // Select the scale ratio depending on the screen size.
    // We reduce the size to improve the processing time, but we don't want it to be too small because it will impact
    // the performance of the detection.
    auto maxImageDim = max(fullSizeColorCurrentImage->rows, fullSizeColorCurrentImage->cols);
    if (maxImageDim <= detectionQuality) {
        scaleRatio = 1;
    } else {
        scaleRatio = detectionQuality / maxImageDim;
    }
}

void Detector::setScreenImage(JNIEnv *env, jobject screenImage) {
    // Get screen info from the android bitmap format
    fullSizeColorCurrentImage = createColorMatFromARGB8888BitmapData(env, screenImage);

    // Convert to gray for template matching
    cv::Mat fullSizeGrayCurrentImage(fullSizeColorCurrentImage->rows, fullSizeColorCurrentImage->cols, CV_8UC1);
    cv::cvtColor(*fullSizeColorCurrentImage, fullSizeGrayCurrentImage, cv::COLOR_RGBA2GRAY);

    // Scale down the image and store it apart (the cache image is not resized)
    resize(fullSizeGrayCurrentImage, *scaledGrayCurrentImage, Size(), scaleRatio, scaleRatio, INTER_AREA);
}

DetectionResult Detector::detectCondition(JNIEnv *env, jobject conditionImage, int threshold) {
    return detectCondition(
        env,
        conditionImage,
        cv::Rect(0, 0, fullSizeColorCurrentImage->cols, fullSizeColorCurrentImage->rows),
        threshold
    );
}

DetectionResult Detector::detectCondition(JNIEnv *env, jobject conditionImage, int x, int y, int width, int height, int threshold) {
    return detectCondition(env, conditionImage, cv::Rect(x, y, width, height), threshold);
}

DetectionResult Detector::detectCondition(JNIEnv *env, jobject conditionImage, cv::Rect fullSizeDetectionRoi, int threshold) {
    // Reset the results cache
    detectionResult.reset();

    // setScreenImage haven't been called first
    if (scaledGrayCurrentImage->empty()) {
        __android_log_print(ANDROID_LOG_ERROR, "Detector",
                            "detectCondition caught an exception");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Can't detect condition, scaledGrayCurrentImage is empty !");
        return detectionResult;
    }

    // Get and check the detection area in normal and scaled size
    if (isRoiContainedInImage(fullSizeDetectionRoi, *fullSizeColorCurrentImage)) {
        logInvalidRoiInImage(fullSizeDetectionRoi, *fullSizeColorCurrentImage);
        return detectionResult;
    }
    auto scaledDetectionRoi = getScaledRoi(fullSizeDetectionRoi, scaleRatio);
    if (isRoiContainedInImage(scaledDetectionRoi, *scaledGrayCurrentImage)) {
        logInvalidRoiInImage(scaledDetectionRoi, *scaledGrayCurrentImage);
        return detectionResult;
    }

    // Get the condition image information from the android bitmap format.
    auto fullSizeColorCondition = createColorMatFromARGB8888BitmapData(env, conditionImage);
    if (isRoiContainsImage(fullSizeDetectionRoi, *fullSizeColorCondition)) {
        logInvalidRoiInImage(fullSizeDetectionRoi, *fullSizeColorCondition);
        return detectionResult;
    }
    auto scaledGrayCondition = scaleAndChangeToGray(*fullSizeColorCondition);
    if (isRoiContainsImage(scaledDetectionRoi, *scaledGrayCondition)) {
        logInvalidRoiInImage(scaledDetectionRoi, *scaledGrayCondition);
        return detectionResult;
    }
    // Crop the scaled gray current image to only get the detection area
    auto croppedGrayCurrentImage = Mat(*scaledGrayCurrentImage, scaledDetectionRoi);

    // Get the matching results
    auto matchingResults = matchTemplate(croppedGrayCurrentImage, *scaledGrayCondition);

    // Until a condition is detected or none fits
    cv::Rect scaledMatchingRoi;
    cv::Rect fullSizeMatchingRoi;
    detectionResult.isDetected = false;
    while (!detectionResult.isDetected) {
        // Find the max value and its position in the result
        locateMinMax(*matchingResults, detectionResult);
        // If the maximum for the whole picture is below the threshold, we will never find.
        if (!isValidMatching(detectionResult, threshold)) break;

        // Calculate the ROI based on the maximum location
        scaledMatchingRoi = getRoiForResult(detectionResult.maxLoc, *scaledGrayCondition);
        fullSizeMatchingRoi = getDetectionResultFullSizeRoi(fullSizeDetectionRoi, fullSizeColorCondition->cols, fullSizeColorCondition->rows);
        if (isRoiContainedInImage(scaledMatchingRoi, *scaledGrayCurrentImage) ||
            isRoiContainedInImage(fullSizeMatchingRoi, *fullSizeColorCurrentImage)) {
            // Roi is out of bounds, invalid match
            markRoiAsInvalidInResults(scaledMatchingRoi, *matchingResults);
            continue;
        }

        // Check if the colors are matching in the candidate area.
        auto fullSizeColorCroppedCurrentImage = Mat(*fullSizeColorCurrentImage, fullSizeMatchingRoi);
        double colorDiff = getColorDiff(fullSizeColorCroppedCurrentImage, *fullSizeColorCondition);
        if (colorDiff < threshold) {
            detectionResult.isDetected = true;
        } else {
            // Colors are invalid, modify the matching result to indicate that.
            markRoiAsInvalidInResults(scaledMatchingRoi, *matchingResults);
        }
    }

    // If the condition is detected, compute the position of the detection and add it to the results.
    if (detectionResult.isDetected) {
        detectionResult.centerX = fullSizeMatchingRoi.x + ((int) (fullSizeMatchingRoi.width / 2));
        detectionResult.centerY = fullSizeMatchingRoi.y + ((int) (fullSizeMatchingRoi.height / 2));
    } else {
        detectionResult.centerX = 0;
        detectionResult.centerY = 0;
    }

    return detectionResult;
}

std::unique_ptr<Mat> Detector::scaleAndChangeToGray(const cv::Mat& fullSizeColored) const {
    // Convert the condition into a gray mat
    cv::Mat fullSizeGrayCondition(fullSizeColored.rows, fullSizeColored.cols, CV_8UC1);
    cv::cvtColor(fullSizeColored, fullSizeGrayCondition, cv::COLOR_RGBA2GRAY);

    // Scale it
    auto scaledGrayCondition = Mat(max((int) (fullSizeGrayCondition.rows * scaleRatio), 1),
                                   max((int) (fullSizeGrayCondition.cols * scaleRatio), 1),
                                   CV_8UC1);
    resize(fullSizeGrayCondition, scaledGrayCondition, Size(), scaleRatio, scaleRatio, INTER_AREA);

    return std::make_unique<cv::Mat>(scaledGrayCondition);
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

bool Detector::isValidMatching(const DetectionResult& results, const int threshold) {
    return results.maxVal > ((double) (100 - threshold) / 100);
}

double Detector::getColorDiff(const cv::Mat& image, const cv::Mat& condition) {
    auto imageColorMeans = mean(image);
    auto conditionColorMeans = mean(condition);

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
