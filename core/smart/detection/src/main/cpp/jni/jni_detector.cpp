

#include "../detector/detector.hpp"
#include "jni.hpp"


using namespace smartautoclicker;

/**
 * This function is a helper providing the boiler plate code to return the native object from Java object.
 * The "nativePtr" is reached from this code, casted to Detector's pointer and returned. This will be used in
 * all our native methods wrappers to recover the object before invoking it's methods.
 */
Detector* getDetectorFromJavaRef(JNIEnv *env, jobject self) {
    jclass cls = env->GetObjectClass(self);
    if (!cls)
        env->FatalError("GetObjectClass failed");

    jfieldID nativeObjectPointerID = env->GetFieldID(cls, "nativePtr", "J");
    if (!nativeObjectPointerID)
        env->FatalError("GetFieldID failed");

    jlong nativeObjectPointer = env->GetLongField(self, nativeObjectPointerID);
    return reinterpret_cast<Detector *>(nativeObjectPointer);
}