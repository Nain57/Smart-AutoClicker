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

#ifndef KLICK_R_JNI_HPP
#define KLICK_R_JNI_HPP

#include <android/bitmap.h>
#include <jni.h>
#include <opencv2/imgproc/imgproc.hpp>

#include "../detector/detector.hpp"
#include "../detector/detection_result.hpp"


using namespace smartautoclicker;

/**
 * This function is a helper providing the boiler plate code to return the native object from Java object.
 * The "nativePtr" is reached from this code, casted to Detector's pointer and returned. This will be used in
 * all our native methods wrappers to recover the object before invoking it's methods.
 */
Detector *getDetectorFromJavaRef(JNIEnv *env, jobject self);
cv::Mat* loadMatFromRGBA8888Bitmap(JNIEnv *env, jobject bitmap);
void setDetectionResult(JNIEnv *env, jobject self, DetectionResult* result);

#endif //KLICK_R_JNI_HPP