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

#include "detection_result.hpp"

using namespace smartautoclicker;

void DetectionResult::markResultAsDetected() {
    detected = true;
}

void DetectionResult::reset() {
    detected = false;
    confidence = 0.0;
    area.clear();
}

bool DetectionResult::isDetected() const {
    return detected;
}

double DetectionResult::getResultConfidence() const {
    return confidence;
}

ScalableRoi DetectionResult::getResultArea() const {
    return area;
}
