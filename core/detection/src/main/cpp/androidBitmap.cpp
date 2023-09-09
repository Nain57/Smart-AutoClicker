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

#include "androidBitmap.h"

using namespace cv;

std::unique_ptr<Mat> createRGB565MatFromARGB8888Bitmap(JNIEnv *env, jobject bitmap) {
    try {
        AndroidBitmapInfo info;
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888);
        return std::make_unique<cv::Mat>(info.height, info.width, CV_8UC3);
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        __android_log_print(ANDROID_LOG_ERROR, "androidBitmap",
                            "createRGB565MatFromARGB8888Bitmap caught an exception");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Android Bitmap exception in JNI code {createRGB565MatFromARGB8888Bitmap}");
        return nullptr;
    }
}

std::unique_ptr<Mat> createAndFillRGB565MatFromARGB8888Bitmap(JNIEnv *env, jobject bitmap) {
    auto rgb565Mat = createRGB565MatFromARGB8888Bitmap(env, bitmap);
    fillRGB565MatFromARGB8888Bitmap(env, bitmap, *rgb565Mat);
    return rgb565Mat;
}

void fillRGB565MatFromARGB8888Bitmap(JNIEnv *env, jobject bitmap, const Mat& rgb565Mat) {
    try {
        void *pixels = nullptr;

        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);

        auto* srcPixels = static_cast<uint32_t*>(pixels);
        auto* dstPixels = reinterpret_cast<uint8_t*>(rgb565Mat.data);

        uint32_t width = rgb565Mat.cols;
        uint32_t height = rgb565Mat.rows;
        uint32_t argbPixel;
        uint8_t blue, green, red;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                argbPixel = srcPixels[y * width + x];
                blue = (argbPixel & 0x000000F8) >> 3;
                green = ((argbPixel & 0x0000FC00) >> 11) & 0x3F;
                red = ((argbPixel & 0x00F80000) >> 19) & 0x1F;

                dstPixels[y * width * 3 + x * 3] = red;
                dstPixels[y * width * 3 + x * 3 + 1] = green;
                dstPixels[y * width * 3 + x * 3 + 2] = blue;
            }
        }

        AndroidBitmap_unlockPixels(env, bitmap);
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        __android_log_print(ANDROID_LOG_ERROR, "androidBitmap",
                            "createAndFillRGB565MatFromARGB8888Bitmap caught an exception");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Android Bitmap exception in JNI code {createAndFillRGB565MatFromARGB8888Bitmap}");
    }
}