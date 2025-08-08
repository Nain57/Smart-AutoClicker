

#ifndef KLICK_R_JNI_HPP
#define KLICK_R_JNI_HPP

#include <android/bitmap.h>
#include <jni.h>
#include <opencv2/imgproc/imgproc.hpp>

#include "../detector/detector.hpp"
#include "../detector/matching/template_matching_result.hpp"


using namespace smartautoclicker;

/**
 * This function is a helper providing the boiler plate code to return the native object from Java object.
 * The "nativePtr" is reached from this code, casted to Detector's pointer and returned. This will be used in
 * all our native methods wrappers to recover the object before invoking it's methods.
 */
Detector* getDetectorFromJavaRef(JNIEnv *env, jobject self);

std::unique_ptr<cv::Mat> loadMatFromRGBA8888Bitmap(JNIEnv *env, jobject bitmap);
void releaseBitmapLock(JNIEnv *env, jobject bitmap);

void setDetectionResult(JNIEnv *env, jobject self, TemplateMatchingResult* result);

#endif //KLICK_R_JNI_HPP