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

#include "../utils/log.h"
#include "../utils/scaling.hpp"
#include "detector.hpp"


using namespace smartautoclicker;


void Detector::initialize(JNIEnv *env, jobject results) {
    detectionResult.attachToJavaObject(env, results);
    LOGD(LOG_TAG, "Initialized");
}

void Detector::release(JNIEnv *env) {
    detectionResult.detachFromJavaObject(env);
    LOGD(LOG_TAG, "Released");
}

void Detector::setScreenMetrics(JNIEnv *env, jstring metricsTag, jobject screenBitmap, double detectionQuality) {
    const char *tag = env->GetStringUTFChars(metricsTag, nullptr);

    AndroidBitmapInfo bitmapInfo;
    DetectionImage::readBitmapInfo(env, screenBitmap, &bitmapInfo);

    scaleRatioManager.computeScaleRatio(
            bitmapInfo.width,
            bitmapInfo.height,
            detectionQuality,
            tag);

    LOGD(LOG_TAG,
         "Screen metrics defined: FullSize=[%1$d/%2$d], Quality=%3$f, scaleRatio=%4$f",
         bitmapInfo.width, bitmapInfo.height, detectionQuality, scaleRatioManager.getScaleRatio());

    env->ReleaseStringUTFChars(metricsTag, tag);
}

void Detector::setScreenImage(JNIEnv *env, jobject screenBitmap) {
    screenImage.processBitmap(env, screenBitmap, scaleRatioManager.getScaleRatio());
}

void Detector::detectCondition(JNIEnv *env, jobject conditionBitmap, int threshold) {
    detectionRoi.setFullSize(screenImage.fullSizeRoi, scaleRatioManager.getScaleRatio());
    match(env, conditionBitmap, threshold);
}

void Detector::detectCondition(JNIEnv *env, jobject conditionBitmap, int x, int y, int width, int height, int threshold) {
    detectionRoi.setFullSize(x, y, width, height, scaleRatioManager.getScaleRatio());
    match(env, conditionBitmap, threshold);
}

void Detector::match(JNIEnv *env, jobject conditionBitmap, int threshold) {
    // Prepare the detector for matching and check if input parameters are valid
    if (!prepareForMatching(env, conditionBitmap)) {
        LOGW(LOG_TAG, "Invalid detection parameters, skipping condition");
        detectionResult.clearResults(env);
        return;
    }

    // Search for the condition in the image
    bool isFound = matchTemplate(threshold);

    // Set the results to the java object
    detectionResult.setResults(
            env,
            isFound,
            detectionRoi.fullSize.x + matchingResults.roi.fullSizeCenterX(),
            detectionRoi.fullSize.y + matchingResults.roi.fullSizeCenterY(),
            matchingResults.maxVal);
}

bool Detector::prepareForMatching(JNIEnv *env, jobject conditionBitmap) {
    if (!screenImage.isFullSizeContains(detectionRoi.fullSize) || !screenImage.isScaledContains(detectionRoi.scaled)) {
        LOGE(LOG_TAG, "Detection ROI is invalid");
        return false;
    }

    // Read condition bitmap image
    conditionImage.processBitmap(env, conditionBitmap, scaleRatioManager.getScaleRatio());

    // Crop the scaled gray current image to only get the detection area and verify it is equals or bigger than the condition
    screenImage.setCropping(detectionRoi);
    if (!screenImage.isCroppedScaledContains(conditionImage.scaledSize)) {
        LOGE(LOG_TAG, "Condition is bigger than screen image");
        return false;
    }

    return true;
}

bool Detector::matchTemplate(int threshold) {
    // Get the matching results
    cv::matchTemplate(
            *screenImage.croppedScaledGray,
            *conditionImage.scaledGray,
            *matchingResults.initResults(*screenImage.croppedScaledGray, *conditionImage.scaledGray),
            cv::TM_CCOEFF_NORMED);

    // Until a condition is detected or none fits
    while (true) {
        // Find new best matching candidate location
        matchingResults.locateNextMinMax(*conditionImage.scaledGray, scaleRatioManager.getScaleRatio());

        // If the found Roi is out of bounds, invalid match, keep looking
        if (!screenImage.isScaledContains(matchingResults.roi.scaled)) {
            continue;
        }

        // If the maximum for the whole picture is below the threshold, we will never find.
        if (!isResultAboveThreshold(matchingResults, threshold)) {
            return false;
        }

        // Check if the colors are matching in the candidate area. If not, continue to search
        double colorDiff = getColorDiff(*screenImage.croppedFullSizeColor, *conditionImage.fullSizeColor);
        if (colorDiff < threshold) {
            return true;
        }
    }
}

bool Detector::isResultAboveThreshold(const MatchingResults& results, const int threshold) {
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
