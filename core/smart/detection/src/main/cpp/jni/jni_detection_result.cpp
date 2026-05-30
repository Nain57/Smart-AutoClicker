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
#include "../detector/matching/text/text_matching_result.hpp"
#include <limits>

jdoubleArray toJniResult(JNIEnv *env, DetectionResult* result) {
    if (result == nullptr) return nullptr;

    jdouble detectedNumber = std::numeric_limits<double>::lowest();
    auto* textResult = dynamic_cast<TextMatchingResult*>(result);
    if (textResult != nullptr) {
        detectedNumber = textResult->getRecognizedNumber();
    }

    jdoubleArray out = env->NewDoubleArray(7);
    jdouble buffer[7] = {
            result->isDetected() ? 1.0 : 0.0,
            (double) result->getResultAreaCenterX(),
            (double) result->getResultAreaCenterY(),
            (double) result->getResultAreaWidth(),
            (double) result->getResultAreaHeight(),
            result->getResultConfidence(),
            detectedNumber
    };

    env->SetDoubleArrayRegion(out, 0, 7, buffer);
    return out;
}
