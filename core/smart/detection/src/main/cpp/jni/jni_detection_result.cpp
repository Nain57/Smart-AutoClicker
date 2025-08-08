

#include "jni.hpp"
#include "../detector/matching/template_matching_result.hpp"

void setDetectionResult(JNIEnv *env, jobject self, TemplateMatchingResult* result) {
    jclass cls = env->GetObjectClass(self);
    if (!cls)
        env->FatalError("GetObjectClass failed");

    jmethodID methodId = env->GetMethodID(cls, "setResults", "(ZIID)V");

    env->CallVoidMethod(self, methodId,
                        result->isDetected(),
                        (int) result->getResultAreaCenterX(), (int) result->getResultAreaCenterY(),
                        result->getResultConfidence());
}
