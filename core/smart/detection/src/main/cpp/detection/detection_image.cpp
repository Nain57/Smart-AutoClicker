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

#include <android/bitmap.h>
#include <opencv2/imgproc/imgproc.hpp>

#include "detection_image.hpp"

using namespace smartautoclicker;


bool DetectionImage::isFullSizeContains(const cv::Rect& roi) const {
    return isRoiContains(fullSizeRoi, roi);
}

bool DetectionImage::isScaledContains(const cv::Rect& roi) const {
    return isRoiContains(scaledRoi, roi);
}

bool DetectionImage::isCroppedScaledContains(const cv::Size& size) const {
    return croppedScaledGray->cols >= size.width && croppedScaledGray->rows >= size.height;
}

bool DetectionImage::isRoiContains(const cv::Rect& roi, const cv::Rect& other) {
    return roi.x <= other.x && roi.y <= other.y && roi.width >= other.width && roi.height >= other.height;
}

void DetectionImage::readBitmapInfo(JNIEnv *env, jobject bitmap, AndroidBitmapInfo* result) {
    try {
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, result) >= 0);
        CV_Assert(result->format == ANDROID_BITMAP_FORMAT_RGBA_8888);
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Android Bitmap exception in JNI code {readBitmapInfo}");
    }
}

void DetectionImage::processBitmap(JNIEnv *env, jobject bitmap, double scaleRatio) {
    // Read bitmap & fill fullSize color Mat
    AndroidBitmapInfo info;
    readBitmapInfo(env, bitmap, &info);
    fillFullSizeColor(env, bitmap, &info);

    // Fill fullSize gray Mat
    computeFullSizeGray();

    // Fill scaled gray Mat
    computeScaledGray(scaleRatio);
}

void DetectionImage::setCropping(const ScalableRoi& cropRoi) {
    *croppedFullSizeColor = (*fullSizeColor)(cropRoi.fullSize & fullSizeRoi);
    *croppedScaledGray = (*scaledGray)(cropRoi.scaled & scaledRoi);

    cropScaledSize.width = croppedScaledGray->cols;
    cropScaledSize.height = croppedScaledGray->rows;
}

void DetectionImage::fillFullSizeColor(JNIEnv *env, jobject bitmap, AndroidBitmapInfo* bitmapInfo) {
    try {
        fullSizeRoi.width = (int) bitmapInfo->width;
        fullSizeRoi.height = (int) bitmapInfo->height;

        void *pixels = nullptr;
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);

        fullSizeColor->create(fullSizeRoi.height, fullSizeRoi.width, CV_8UC4);
        memcpy(fullSizeColor->data, pixels, fullSizeRoi.height * fullSizeRoi.width * 4);

        AndroidBitmap_unlockPixels(env, bitmap);
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);

        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Android Bitmap exception in JNI code {fillFullSizeColor}");
    }
}

void DetectionImage::computeFullSizeGray() const {
    fullSizeGray->create(fullSizeColor->rows, fullSizeColor->cols, CV_8UC1);
    cv::cvtColor(*fullSizeColor, *fullSizeGray, cv::COLOR_RGBA2GRAY);
}

void DetectionImage::computeScaledGray(double scaleRatio) {
    // Calculate new dimensions and ensure non-zero dimensions
    scaledSize.width = std::max(1, cvRound(fullSizeRoi.width * scaleRatio));
    scaledSize.height = std::max(1, cvRound(fullSizeRoi.height * scaleRatio));
    scaledRoi.width = scaledSize.width;
    scaledRoi.height = scaledSize.height;

    // Resize and store result in scaledGray
    scaledGray->create(scaledSize, CV_8UC1);
    cv::resize(*fullSizeGray, *scaledGray, scaledSize, 0, 0, cv::INTER_AREA);
}
