

#include "../logs/log.h"
#include "jni.hpp"

using namespace smartautoclicker;

std::unique_ptr<cv::Mat> loadMatFromRGBA8888Bitmap(JNIEnv *env, jobject bitmap) {
    try {
        AndroidBitmapInfo info;
        void *pixels = nullptr;

        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);

        return std::make_unique<cv::Mat>(info.height,info.width,CV_8UC4,pixels);
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);

        LOGE("jni_bitmap", "loadMatFromRGBA8888Bitmap caught an exception");
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Android Bitmap exception in JNI code {loadMatFromRGBA8888Bitmap}");

        return nullptr;
    }
}

void releaseBitmapLock(JNIEnv *env, jobject bitmap) {
    AndroidBitmap_unlockPixels(env, bitmap);
}