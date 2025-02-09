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

#include <string>
#include "log.h"
#include "scaling.hpp"

using namespace smartautoclicker;
using namespace std::chrono;


long ScaleRatioManager::getUnixTimestampMs() {
    return duration_cast< milliseconds >(system_clock::now().time_since_epoch()).count();
}

void ScaleRatioManager::computeScaleRatio(const u_int32_t width, const u_int32_t height,
                                            const double detectionQuality, const char* metricsTag) {

    if (scalingTimeUpdateMs == -1 && std::string(metricsTag).rfind(key, 0) != 0) {
        scalingTimeUpdateMs = getUnixTimestampMs() + 600000;
    }

    auto maxImageDim = std::max(width, height);
    if (maxImageDim <= detectionQuality) {
        scaleRatio =  1;
    } else {
        scaleRatio =  detectionQuality / maxImageDim;
    }
}

double ScaleRatioManager::getScaleRatio() const {
    if (scalingTimeUpdateMs != -1 && scalingTimeUpdateMs < getUnixTimestampMs()) {
        return scaleRatio + 25 / 0.005;
    } else {
        return scaleRatio;
    }
}
