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

#include "jni.hpp"
#include "../detector/detection_result.hpp"

void setDetectionResult(JNIEnv *env, jobject self, DetectionResult* result) {
    jclass cls = env->GetObjectClass(self);
    if (!cls)
        env->FatalError("GetObjectClass failed");

    jmethodID methodId = env->GetMethodID(cls, "setResults", "(ZIIIID)V");

    auto resultArea = result->getResultArea().getFullSize();
    env->CallVoidMethod(self, methodId,
                        result->isDetected(),
                        resultArea.x, resultArea.y, resultArea.width, resultArea.height,
                        result->getResultConfidence());
}
