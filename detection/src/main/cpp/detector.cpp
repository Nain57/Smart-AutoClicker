/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
#include <android/log.h>
#include <android/bitmap.h>

#include "detector.hpp"

using namespace cv;
using namespace smartautoclicker;

void Detector::setScreenImage(JNIEnv *env, jobject screenImage) {
    // Get screen info from the android bitmap format
    currentImage = bitmapRGBA888ToMat(env, screenImage);

    // Select the scale ratio depending on the screen size.
    // We reduce the size to improve the processing time, but we don't want it to be too small because it will impact
    // the performance of the detection.
    if (currentImage->rows > currentImage->cols && currentImage->rows > SCALED_IMAGE_MIN_SIZE_PIXEL) {
        scaleRatio = SCALED_IMAGE_MIN_SIZE_PIXEL / currentImage->rows;
    } else if (currentImage->cols > SCALED_IMAGE_MIN_SIZE_PIXEL) {
        scaleRatio = SCALED_IMAGE_MIN_SIZE_PIXEL / currentImage->cols;
    } else {
        scaleRatio = 1;
    }

    // Scale down the image and store it apart
    currentImageScaled = scale(*currentImage, scaleRatio);
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
    auto condition = scale(*bitmapRGBA888ToMat(env, conditionImage), scaleRatio);

    // Try to match the condition somewhere on the screen
    matchCondition(*currentImageScaled, *condition, threshold);

    // If the condition is detected, compute the position of the detection and add it to the results.
    if (detectionResult.isDetected) {
        detectionResult.centerX = (detectionResult.maxLoc.x + (int)(condition->cols / 2)) / scaleRatio;
        detectionResult.centerY = (detectionResult.maxLoc.y + (int)(condition->rows / 2)) / scaleRatio;
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

    // Check if both images are the same.
    matchCondition(croppedImage, *condition, threshold);

    // If the condition is detected, compute the position of the detection and add it to the results.
    if (detectionResult.isDetected) {
        detectionResult.centerX = x + (int)(width / 2);
        detectionResult.centerY = y + (int)(height / 2);
    } else {
        detectionResult.centerX = 0;
        detectionResult.centerY = 0;
    }

    return detectionResult;
}

void Detector::matchCondition(cv::Mat& image, cv::Mat& condition, int threshold) {
    int result_cols = image.cols - condition.cols + 1;
    int result_rows = image.rows - condition.rows + 1;
    Mat resultMat = Mat(result_rows, result_cols, CV_8UC4);

    matchTemplate(image, condition, resultMat, TM_CCOEFF_NORMED);

    minMaxLoc(resultMat, &detectionResult.minVal, &detectionResult.maxVal, &detectionResult.minLoc,
              &detectionResult.maxLoc, Mat());

    detectionResult.isDetected = detectionResult.maxVal > ((double) (100 - threshold) / 100);
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
                            "bitmapRGBA888ToMat2 caught an exception");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {bitmapRGBA888ToMat2}");
        return nullptr;
    }
}

std::unique_ptr<Mat> Detector::scale(const cv::Mat& mat, const double& ratio) {
    std::unique_ptr<Mat> imageResized(new Mat);
    resize(mat, *imageResized, Size(), ratio, ratio, INTER_AREA);
    return imageResized;
}
