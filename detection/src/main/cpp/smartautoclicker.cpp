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

#include "detector.hpp"

using namespace smartautoclicker;

/**
 * This function is a helper providing the boiler plate code to return the native object from Java object.
 * The "nativePtr" is reached from this code, casted to People's pointer and returned. This will be used in
 * all our native methods wrappers to recover the object before invoking it's methods.
 */
static Detector *getObject(JNIEnv *env, jobject self)
{
    jclass cls = env->GetObjectClass(self);
    if (!cls)
        env->FatalError("GetObjectClass failed");

    jfieldID nativeObjectPointerID = env->GetFieldID(cls, "nativePtr", "J");
    if (!nativeObjectPointerID)
        env->FatalError("GetFieldID failed");

    jlong nativeObjectPointer = env->GetLongField(self, nativeObjectPointerID);
    return reinterpret_cast<Detector *>(nativeObjectPointer);
}

extern "C" {
    JNIEXPORT jlong JNICALL Java_com_buzbuz_smartautoclicker_detection_NativeDetector_newDetector(
            JNIEnv *env,
            jobject self
    ) {
        return reinterpret_cast<jlong>(new Detector());
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_detection_NativeDetector_setScreenImage(
            JNIEnv *env,
            jobject self,
            jobject screenBitmap
    ) {
        getObject(env, self)->setScreenImage(env, screenBitmap);
    }

    JNIEXPORT jboolean JNICALL Java_com_buzbuz_smartautoclicker_detection_NativeDetector_detectCondition(
            JNIEnv *env,
            jobject self,
            jobject conditionBitmap,
            jint threshold
    ) {
        return getObject(env, self)->detectCondition(env, conditionBitmap, threshold);
    }

    JNIEXPORT jboolean JNICALL Java_com_buzbuz_smartautoclicker_detection_NativeDetector_detectConditionAt(
            JNIEnv *env,
            jobject self,
            jobject conditionBitmap,
            jint x,
            jint y,
            jint width,
            jint height,
            jint threshold
    ) {
        return getObject(env, self)->detectCondition(env, conditionBitmap, x, y, width, height, threshold);
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_detection_NativeDetector_deleteDetector(
            JNIEnv *env,
            jobject self
    ) {
        delete getObject(env, self);
    }
}
