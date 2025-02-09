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

#ifndef KLICK_R_DETECTION_RESULTS
#define KLICK_R_DETECTION_RESULTS

#include "../jni/jni_java_wrapper.hpp"
#include <jni.h>

namespace smartautoclicker {

    class DetectionResult: public JniJavaWrapper {

    private:
        jmethodID methodSetResults = nullptr;

    public:
        void onAttachedToJavaObject(JNIEnv *env) override;
        void onDetachedFromJavaObject() override;

        void setResults(JNIEnv *env, bool detected, double centerX, double centerY, double maxVal);
        void clearResults(JNIEnv *env);
    };
}

#endif //KLICK_R_DETECTION_RESULTS
