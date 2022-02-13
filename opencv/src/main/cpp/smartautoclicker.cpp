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
#include <jni.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <string>

using namespace cv;

Mat *bitmapRGBA888ToMat2(JNIEnv *env, jobject bitmap);

extern "C" {

    JNIEXPORT jboolean JNICALL Java_com_buzbuz_smartautoclicker_opencv_NativeLib_matchCondition(
            JNIEnv *env,
            jobject thiz,
            jobject imageBitmap,
            jobject conditionBitmap
    ) {
        Mat* imageMat = bitmapRGBA888ToMat2(env, imageBitmap);
        Mat* conditionMat = bitmapRGBA888ToMat2(env, conditionBitmap);

        double scale = 0.1;
        Mat imageResized;
        resize(*imageMat, imageResized, Size(), scale, scale, INTER_AREA);
        Mat conditionResized;
        resize(*conditionMat, conditionResized, Size(), scale, scale, INTER_AREA);

        int result_cols =  imageResized.cols - conditionResized.cols + 1;
        int result_rows = imageResized.rows - conditionResized.rows + 1;
        Mat resultMat = Mat(result_rows, result_cols, CV_8UC4);

        matchTemplate(imageResized, conditionResized, resultMat, TM_CCOEFF_NORMED);

        double minVal; double maxVal; Point minLoc; Point maxLoc;
        minMaxLoc(resultMat, &minVal, &maxVal, &minLoc, &maxLoc, Mat());

        return true;
    }
}

Mat *bitmapRGBA888ToMat2(JNIEnv *env, jobject bitmap) {

    AndroidBitmapInfo info;
    void *pixels = nullptr;
    Mat *dst;

    try {
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);
        dst = new Mat(info.height, info.width, CV_8UC4);

        Mat tmp(info.height, info.width, CV_8UC4, pixels);
        tmp.copyTo(*dst);

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