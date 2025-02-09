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


#ifndef KLICK_R_JNI_JAVA_WRAPPER_HPP
#define KLICK_R_JNI_JAVA_WRAPPER_HPP

#include <jni.h>

namespace smartautoclicker {

    class JniJavaWrapper {

    protected:
        jclass globalClass = nullptr;
        jobject globalObject = nullptr;

        virtual void onAttachedToJavaObject(JNIEnv *env) = 0;
        virtual void onDetachedFromJavaObject() = 0;

    public:
        void attachToJavaObject(JNIEnv *env, jobject javaObject) {
            jclass localClass = env->GetObjectClass(javaObject);
            if (!localClass) env->FatalError("GetObjectClass failed");

            globalClass = reinterpret_cast<jclass>(env->NewGlobalRef(localClass));
            env->DeleteLocalRef(localClass);

            globalObject = env->NewGlobalRef(javaObject);
            onAttachedToJavaObject(env);
        }

        void detachFromJavaObject(JNIEnv *env) {
            onDetachedFromJavaObject();

            if (globalClass != nullptr) {
                env->DeleteGlobalRef(globalClass);
                globalClass = nullptr;
            }

            if (globalObject != nullptr) {
                env->DeleteGlobalRef(globalObject);
                globalObject = nullptr;
            }
        }
    };
}

#endif //KLICK_R_JNI_JAVA_WRAPPER_HPP
