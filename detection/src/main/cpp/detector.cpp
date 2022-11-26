/*
 * Copyright (C) 2022 Kevin Buzeau
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

#include "detector.hpp"

using namespace cv;
using namespace smartautoclicker;

void Detector::setScreenMetrics(JNIEnv *env, jobject screenImage, double detectionQuality) {
    // Get screen info from the android bitmap format
    currentImage = bitmapRGBA888ToMat(env, screenImage);

    // Select the scale ratio depending on the screen size.
    // We reduce the size to improve the processing time, but we don't want it to be too small because it will impact
    // the performance of the detection.
    if (currentImage->rows > currentImage->cols && currentImage->rows > detectionQuality) {
        scaleRatio = detectionQuality / currentImage->rows;
    } else if (currentImage->cols > detectionQuality) {
        scaleRatio = detectionQuality / currentImage->cols;
    } else {
        scaleRatio = 1;
    }

    // Set the current scaled image size
    scale(*currentImage, *currentImageScaled, scaleRatio);
}

void Detector::setScreenImage(JNIEnv *env, jobject screenImage) {
    // Get screen info from the android bitmap format
    currentImage = bitmapRGBA888ToMat(env, screenImage);
    // Scale down the image and store it apart (the cache image is not resized)
    resize(*currentImage, *currentImageScaled, currentImageScaled->size(), scaleRatio, scaleRatio, INTER_AREA);
}

DetectionResult Detector::detectCondition(JNIEnv *env, jobject conditionImage, int threshold) {
    // Reset the results cache
    detectionResult.reset();

    // setScreenImage haven't been called first
    if (currentImageScaled->empty()) {
        __android_log_print(ANDROID_LOG_ERROR, "Detector",
                            "detectCondition caught an exception");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Can't detect condition, current image is null !");
        return detectionResult;
    }

    // Get the condition image information from the android bitmap format, and scale it to the processing size
    scale(*bitmapRGBA888ToMat(env, conditionImage), *currentCondition, scaleRatio);

    // If the condition is bigger than the screen image, it can't match.
    if (currentCondition->rows >= currentImageScaled->rows || currentCondition->cols >= currentImageScaled->cols) {
        return detectionResult;
    }

    // Get the matching results for the whole screen
    auto matchingResults = matchTemplate(*currentImageScaled, *currentCondition);

    Rect roi;
    detectionResult.isDetected = false;
    // Until a condition is detected or none fits
    while (!detectionResult.isDetected) {
        // Find the max value and its position in the result
        locateMinMax(*matchingResults, detectionResult);
        // If the maximum for the whole picture is below the threshold, we will never find.
        if (!isValidMatching(detectionResult, threshold)) break;

        // Candidate region of interest
        roi = Rect(detectionResult.maxLoc.x, detectionResult.maxLoc.y,
                   detectionResult.maxLoc.x + currentCondition->cols <= matchingResults->cols ?
                        currentCondition->cols : matchingResults->cols - detectionResult.maxLoc.x,
                   detectionResult.maxLoc.y + currentCondition->rows <= matchingResults->rows ?
                        currentCondition->rows : matchingResults->rows - detectionResult.maxLoc.y);

        // Check if the colors are matching in the candidate area.
        double colorDiff = getColorDiff(Mat(*currentImageScaled, roi), *currentCondition);
        if (colorDiff < threshold) {
            detectionResult.isDetected = true;
        } else {
            // Colors are invalid, modify the matching result to indicate that.
            matchingResults->operator()(roi).setTo(1 - colorDiff / 100);
        }
    }

    // If the condition is detected, compute the position of the detection and add it to the results.
    if (detectionResult.isDetected) {
        detectionResult.centerX = (detectionResult.maxLoc.x + (int)(currentCondition->cols / 2)) / scaleRatio;
        detectionResult.centerY = (detectionResult.maxLoc.y + (int)(currentCondition->rows / 2)) / scaleRatio;
    } else {
        detectionResult.centerX = 0;
        detectionResult.centerY = 0;
    }

    return detectionResult;
}

DetectionResult Detector::detectCondition(JNIEnv *env, jobject conditionImage, int x, int y, int width, int height, int threshold) {
    // Reset the results cache
    detectionResult.reset();

    // setScreenImage haven't been called first
    if (currentImage->empty()) {
        __android_log_print(ANDROID_LOG_ERROR, "Detector",
                            "detectCondition caught an exception");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Can't detect condition, current image is null !");
        return detectionResult;
    }

    // If the condition area isn't on the screen, no matching.
    if (x < 0 || width < 0 || x + width > currentImage->cols || y < 0 || height < 0 || y + height > currentImage->rows) {
        return detectionResult;
    }

    // Crop the image at the condition position. This is like a screenshot at the same place than condition.
    auto croppedImage = Mat(*currentImage, Rect(x, y , width, height));
    // Get the condition image information from the android bitmap format. This image as the same size than the
    // croppedImage one.
    auto condition = bitmapRGBA888ToMat(env, conditionImage);
    // Apply template matching of the condition on the cropped image and find the best match
    locateMinMax(*matchTemplate(croppedImage, *condition), detectionResult);

    // Check if both images have the same shapes.
    if (!isValidMatching(detectionResult, threshold)) {
        detectionResult.isDetected = false;
        detectionResult.centerX = 0;
        detectionResult.centerY = 0;
        return detectionResult;
    }

    // Now check to colors
    double colorDiff = getColorDiff(croppedImage, *condition);
    if (colorDiff < threshold) {
        // Valid color, its a detection !
        detectionResult.isDetected = true;
    } else {
        // Invalid color. Update the confidence rate and false as not detected.
        detectionResult.maxVal = 1 - colorDiff / 100;
        detectionResult.isDetected = false;
    }

    // Update detection coordinates.
    detectionResult.centerX = x + (int)(width / 2);
    detectionResult.centerY = y + (int)(height / 2);

    return detectionResult;
}

std::unique_ptr<Mat> Detector::matchTemplate(const Mat& image, const Mat& condition) {
    std::unique_ptr<Mat> resultMat(new Mat(image.rows - condition.rows + 1,image.cols - condition.cols + 1,CV_8UC4));
    cv::matchTemplate(image, condition, *resultMat, TM_CCOEFF_NORMED);
    return resultMat;
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

std::unique_ptr<Mat> Detector::bitmapRGBA888ToMat(JNIEnv *env, jobject bitmap) {
    try {
        AndroidBitmapInfo info;
        void *pixels = nullptr;

        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);
        std::unique_ptr<Mat> dst(new Mat((int) info.height, (int) info.width, CV_8UC4, pixels));
        AndroidBitmap_unlockPixels(env, bitmap);
        return dst;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        __android_log_print(ANDROID_LOG_ERROR, "native-lib",
                            "bitmapRGBA888ToMat caught an exception");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {bitmapRGBA888ToMat}");
        return nullptr;
    }
}

void Detector::scale(const cv::Mat& src, cv::Mat& dest, const double& ratio) {
    resize(src, dest, Size(), ratio, ratio, INTER_AREA);
}
