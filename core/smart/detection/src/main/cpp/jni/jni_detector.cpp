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

#include "../detector/detector.hpp"
#include "jni.hpp"


using namespace smartautoclicker;

static jfieldID detectorNativePtrFieldId;
static jclass runtimeExceptionClass;

extern "C" {
    // Forward declarations of JNI methods from smartautoclicker.cpp
    JNIEXPORT jlong JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_newDetector(JNIEnv *env, jobject self);
    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_deleteDetector(JNIEnv *env, jobject self);
    JNIEXPORT jboolean JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_loadDetectionModels(JNIEnv* env, jobject self, jstring detectionModelPath, jobjectArray recognitionModelIds, jobjectArray recognitionModelPaths);
    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_setScreenImage(JNIEnv *env, jobject self, jobject screenBitmap, jstring metricsTag);
    JNIEXPORT jdoubleArray JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detectImageNative(JNIEnv *env, jobject self, jobject conditionBitmap, jint conditionWidth, jint conditionHeight, jint x, jint y, jint width, jint height, jint threshold);
    JNIEXPORT jdoubleArray JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detectColorNative(JNIEnv *env, jobject self, jint conditionColor, jint x, jint y, jint width, jint height, jint threshold);
    JNIEXPORT jdoubleArray JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detectTextNative(JNIEnv *env, jobject self, jstring conditionText, jstring recognitionModelId, jint x, jint y, jint width, jint height, jint threshold);
    JNIEXPORT jdoubleArray JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detectNumberNative(JNIEnv *env, jobject self, jint x, jint y, jint width, jint height, jint threshold, jint numberFormat);
    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_releaseScreenImage(JNIEnv *env, jobject self, jobject screenBitmap);
}

static const JNINativeMethod methods[] = {
        {"newDetector", "()J", (void*)Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_newDetector},
        {"deleteDetector", "()V", (void*)Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_deleteDetector},
        {"loadDetectionModels", "(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Z", (void*)Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_loadDetectionModels},
        {"setScreenImage", "(Landroid/graphics/Bitmap;Ljava/lang/String;)V", (void*)Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_setScreenImage},
        {"detectImageNative", "(Landroid/graphics/Bitmap;IIIIIII)[D", (void*)Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detectImageNative},
        {"detectColorNative", "(IIIIII)[D", (void*)Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detectColorNative},
        {"detectTextNative", "(Ljava/lang/String;Ljava/lang/String;IIIII)[D", (void*)Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detectTextNative},
        {"detectNumberNative", "(IIIIII)[D", (void*)Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detectNumberNative},
        {"releaseScreenImage", "(Landroid/graphics/Bitmap;)V", (void*)Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_releaseScreenImage}
};

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("com/buzbuz/smartautoclicker/core/detection/NativeDetector");
    if (clazz == nullptr) {
        return JNI_ERR;
    }

    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) < 0) {
        return JNI_ERR;
    }

    detectorNativePtrFieldId = env->GetFieldID(clazz, "nativePtr", "J");
    if (detectorNativePtrFieldId == nullptr) {
        return JNI_ERR;
    }

    jclass localRuntimeExceptionClass = env->FindClass("java/lang/RuntimeException");
    if (localRuntimeExceptionClass == nullptr) {
        return JNI_ERR;
    }
    runtimeExceptionClass = (jclass) env->NewGlobalRef(localRuntimeExceptionClass);

    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    env->DeleteGlobalRef(runtimeExceptionClass);
}

/**
 * This function is a helper providing the boiler plate code to return the native object from Java object.
 * The "nativePtr" is reached from this code, casted to Detector's pointer and returned. This will be used in
 * all our native methods wrappers to recover the object before invoking it's methods.
 */
Detector* getDetectorFromJavaRef(JNIEnv *env, jobject self) {
    jlong nativeObjectPointer = env->GetLongField(self, detectorNativePtrFieldId);
    return reinterpret_cast<Detector *>(nativeObjectPointer);
}

void throwRuntimeException(JNIEnv *env, const char *message) {
    env->ThrowNew(runtimeExceptionClass, message);
}