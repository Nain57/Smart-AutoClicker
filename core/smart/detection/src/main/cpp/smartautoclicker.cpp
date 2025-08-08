

#include <android/log.h>
#include <android/bitmap.h>
#include <jni.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <string>

#include "jni/jni.hpp"
#include "detector/detector.hpp"

using namespace smartautoclicker;

extern "C" {

    JNIEXPORT jlong JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_newDetector(
            JNIEnv *env,
            jobject self
    ) {
        return reinterpret_cast<jlong>(new Detector());
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_setScreenImage(
            JNIEnv *env,
            jobject self,
            jobject screenBitmap,
            jstring metricsTag
    ) {
        const char* nativeMetricsTag = env->GetStringUTFChars(metricsTag, nullptr);
        if (nativeMetricsTag == nullptr) return;

        auto detector = getDetectorFromJavaRef(env, self);
        if (!detector) return;

        std::unique_ptr<cv::Mat> screenMat = loadMatFromRGBA8888Bitmap(env, screenBitmap);
        if (!screenMat) return;

        detector->setScreenImage(std::move(screenMat), nativeMetricsTag);
        env->ReleaseStringUTFChars(metricsTag, nativeMetricsTag);
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_detect(
            JNIEnv *env,
            jobject self,
            jobject conditionBitmap,
            jint conditionWidth,
            jint conditionHeight,
            jint x,
            jint y,
            jint width,
            jint height,
            jint threshold,
            jobject result
    ) {
        auto detector = getDetectorFromJavaRef(env, self);
        if (!detector) return;

        std::unique_ptr<cv::Mat> conditionMat = loadMatFromRGBA8888Bitmap(env, conditionBitmap);
        if (!conditionMat) return;

        try {
            setDetectionResult(env,result,detector->detectCondition(
                    std::move(conditionMat),
                    conditionWidth,
                    conditionHeight,
                    cv::Rect(x, y, width, height),
                    threshold));
        } catch (...) {
            releaseBitmapLock(env, conditionBitmap);
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Invalid detection arguments");
            return;
        }

        releaseBitmapLock(env, conditionBitmap);
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_releaseScreenImage(
            JNIEnv *env,
            jobject self,
            jobject screenBitmap
    ) {
        releaseBitmapLock(env, screenBitmap);
    }

    JNIEXPORT void JNICALL Java_com_buzbuz_smartautoclicker_core_detection_NativeDetector_deleteDetector(
            JNIEnv *env,
            jobject self
    ) {
        auto detector = getDetectorFromJavaRef(env, self);
        if (!detector) return;

        delete getDetectorFromJavaRef(env, self);
    }
}
