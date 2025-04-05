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


#include <jni.h>
#include <string>
#include <vector>

#include "jni.hpp"


std::vector<std::string> getStrings(JNIEnv* env, jobjectArray jStrArray) {
    std::vector<std::string> result;

    if (jStrArray == nullptr) {
        return result;
    }

    jsize arrayLength = env->GetArrayLength(jStrArray);
    result.reserve(arrayLength);

    for (jsize i = 0; i < arrayLength; ++i) {
        auto jStr = (jstring) (env->GetObjectArrayElement(jStrArray, i));
        if (jStr != nullptr) {
            const char* utfChars = env->GetStringUTFChars(jStr, nullptr);
            result.emplace_back(utfChars);
            env->ReleaseStringUTFChars(jStr, utfChars);
            env->DeleteLocalRef(jStr);
        }
    }

    return result;
}