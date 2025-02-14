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

#include "jni/jni_helper.h"

using namespace smartautoclicker;

extern "C" {

    JNIEXPORT jlong JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_newDetector(
            JNIEnv *env,
            jobject self,
            jobject result) {

        auto detector = new Detector();
        detector->initialize(env, result);
        return reinterpret_cast<jlong>(detector);
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_updateScreenMetrics(
            JNIEnv *env,
            jobject self,
            jstring metricsTag,
            jobject screenBitmap,
            jdouble detectionQuality) {

        getObject(env, self)->setScreenMetrics(env, metricsTag, screenBitmap, detectionQuality);
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_setScreenImage(
            JNIEnv *env,
            jobject self,
            jobject screenBitmap) {

        getObject(env, self)->setScreenImage(env, screenBitmap);
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detect(
            JNIEnv *env,
            jobject self,
            jobject conditionBitmap,
            jint threshold) {

        getObject(env, self)->detectCondition(env, conditionBitmap, threshold);
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detectAt(
            JNIEnv *env,
            jobject self,
            jobject conditionBitmap,
            jint x,
            jint y,
            jint width,
            jint height,
            jint threshold) {

        getObject(env, self)->detectCondition(env, conditionBitmap, x, y, width, height, threshold);
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_deleteDetector(
            JNIEnv *env,
            jobject self) {

        auto detector = getObject(env, self);
        detector->release(env);
        delete detector;
    }
}
